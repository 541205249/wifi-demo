package com.wifi.optometry.communication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.wifi.lib.command.OutboundCommand;
import com.wifi.lib.command.ack.AckMessage;
import com.wifi.lib.command.gateway.ProtocolGateway;
import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.profile.OptometryCommandProfile;
import com.wifi.lib.command.stream.StreamFrame;
import com.wifi.lib.command.stream.StreamMetadata;
import com.wifi.lib.command.stream.StreamSender;
import com.wifi.lib.command.stream.StreamStats;
import com.wifi.lib.command.stream.StreamStatsListener;
import com.wifi.lib.command.transfer.TransferMetadata;
import com.wifi.lib.command.transfer.TransferProgress;
import com.wifi.lib.command.transfer.TransferProgressListener;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;
import com.wifi.optometry.communication.device.DeviceHistoryStore;
import com.wifi.optometry.communication.device.DeviceManager;
import com.wifi.optometry.communication.device.Hc25MacDiscoveryClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;

public class TcpServerService extends Service {
    private static final String TAG = "TcpServerService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "tcp_server_channel";
    private static final int MAX_MAC_DISCOVERY_ROUNDS = 3;

    private final IBinder binder = new TcpServerBinder();
    private final Map<String, PendingArchiveState> pendingArchiveStates = new ConcurrentHashMap<>();
    private final Hc25MacDiscoveryClient macDiscoveryClient = new Hc25MacDiscoveryClient();
    private ProtocolGateway protocolGateway;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRunning = false;

    private DeviceManager deviceManager;
    private DeviceHistoryStore deviceHistoryStore;
    private final CopyOnWriteArrayList<OnMessageListener> messageListeners = new CopyOnWriteArrayList<>();
    @Nullable
    private OnMessageListener legacyMessageListener;
    private Handler mainHandler;
    private String localIpAddress;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private HeartbeatManager heartbeatManager;

    public interface OnMessageListener {
        void onMessageReceived(String clientId, String message);

        void onClientConnected(String clientId);

        void onClientIdentityResolved(String clientId, String macAddress);

        void onClientDisconnected(String clientId);

        void onError(String error);

        void onServerStarted(String ipAddress);

        default void onProtocolEvent(String clientId, @NonNull ProtocolInboundEvent event) {
        }

        default void onProtocolError(String clientId, String rawMessage, String errorMessage) {
        }
    }

    public class TcpServerBinder extends Binder {
        public TcpServerService getService() {
            return TcpServerService.this;
        }
    }

    private static final class PendingLogRecord {
        private final long timestamp;
        private final String category;
        private final String action;
        private final String message;
        private final String remoteIp;
        private final int remotePort;
        private final String localIp;
        private final int localPort;

        private PendingLogRecord(
                long timestamp,
                String category,
                String action,
                String message,
                String remoteIp,
                int remotePort,
                String localIp,
                int localPort
        ) {
            this.timestamp = timestamp;
            this.category = category;
            this.action = action;
            this.message = message;
            this.remoteIp = remoteIp;
            this.remotePort = remotePort;
            this.localIp = localIp;
            this.localPort = localPort;
        }
    }

    private static final class PendingArchiveState {
        private final List<PendingLogRecord> pendingLogs = new ArrayList<>();
        private boolean discoveryInProgress;
        private boolean disconnected;
        private int discoveryRounds;

        synchronized boolean tryStartDiscovery(int maxRounds) {
            if (discoveryInProgress || discoveryRounds >= maxRounds) {
                return false;
            }
            discoveryInProgress = true;
            discoveryRounds++;
            return true;
        }

        synchronized void finishDiscovery() {
            discoveryInProgress = false;
        }

        synchronized boolean isDiscoveryInProgress() {
            return discoveryInProgress;
        }

        synchronized void addLog(PendingLogRecord logRecord) {
            pendingLogs.add(logRecord);
        }

        synchronized List<PendingLogRecord> consumeLogs() {
            List<PendingLogRecord> copiedLogs = new ArrayList<>(pendingLogs);
            pendingLogs.clear();
            return copiedLogs;
        }

        synchronized void markDisconnected() {
            disconnected = true;
        }

        synchronized boolean isDisconnected() {
            return disconnected;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        JLog.i(TAG, "Service created");
        trace("服务已创建，准备恢复数据库状态和后台保活");
        mainHandler = new Handler(Looper.getMainLooper());
        deviceHistoryStore = DeviceHistoryStore.getInstance(this);
        deviceHistoryStore.markAllDevicesOffline();
        protocolGateway = new ProtocolGateway(this, OptometryCommandProfile.getInstance());
        createNotificationChannel();
        acquireWakeLocks();
        initHeartbeatManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        JLog.i(TAG, "Service started");
        trace("收到启动命令，开始拉起监听");
        startForeground(NOTIFICATION_ID, createNotification("TCP 服务器正在启动..."));
        startServer();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        JLog.i(TAG, "Service destroyed");
        trace("服务销毁，开始释放连接和锁资源");
        stopServer();
        releaseWakeLocks();
        destroyDeviceManager();
        destroyHeartbeatManager();
    }

    public void setOnMessageListener(OnMessageListener listener) {
        if (legacyMessageListener != null) {
            messageListeners.remove(legacyMessageListener);
        }
        legacyMessageListener = listener;
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    public void registerOnMessageListener(@NonNull OnMessageListener listener) {
        if (!messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }

    public void unregisterOnMessageListener(@Nullable OnMessageListener listener) {
        if (listener == null) {
            return;
        }
        messageListeners.remove(listener);
        if (listener == legacyMessageListener) {
            legacyMessageListener = null;
        }
    }

    public void setLocalIpAddress(String ipAddress) {
        this.localIpAddress = ipAddress;
        JLog.i(TAG, "Set local IP address from Activity: " + ipAddress);
        trace("同步本机监听地址，ip=" + ipAddress);
    }

    public boolean isServerRunning() {
        return isRunning;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public String[] getConnectedClientIds() {
        return deviceManager != null ? deviceManager.getConnectedDeviceIds() : new String[0];
    }

    public DeviceManager.DeviceConnection[] getConnectedDevices() {
        if (deviceManager == null) {
            return new DeviceManager.DeviceConnection[0];
        }
        List<DeviceManager.DeviceConnection> devices = deviceManager.getConnectedDevices();
        return devices.toArray(new DeviceManager.DeviceConnection[0]);
    }

    public String getDeviceDisplayLabel(String clientId) {
        if (deviceManager == null) {
            return clientId;
        }
        DeviceManager.DeviceConnection device = deviceManager.getConnectedDevice(clientId);
        return device != null ? device.getInlineLabel() : clientId;
    }

    public void sendMessageToClient(String clientId, String message) {
        if (deviceManager == null) {
            return;
        }

        trace("准备向模块发送消息，clientId=" + clientId + ", length=" + (message == null ? 0 : message.length()));
        DeviceManager.DeviceConnection device = deviceManager.sendMessageToDevice(clientId, message);
        if (device != null) {
            recordCommunication(device, DeviceHistoryStore.ACTION_SENT, message);
        }
    }

    @Nullable
    public OutboundCommand sendCommandByCodeToClient(
            @NonNull String clientId,
            @NonNull String code,
            @Nullable Map<String, String> arguments
    ) {
        if (protocolGateway == null) {
            return null;
        }
        try {
            return protocolGateway.sendCommand(code, arguments, rawMessage -> sendMessageToClient(clientId, rawMessage));
        } catch (Exception exception) {
            traceError("按编码定向发送失败，code=" + code + ", clientId=" + clientId, exception);
            return null;
        }
    }

    @Nullable
    public OutboundCommand broadcastCommandByCode(
            @NonNull String code,
            @Nullable Map<String, String> arguments
    ) {
        if (protocolGateway == null) {
            return null;
        }
        try {
            return protocolGateway.sendCommand(code, arguments, this::broadcastMessage);
        } catch (Exception exception) {
            traceError("按编码广播发送失败，code=" + code, exception);
            return null;
        }
    }

    public void sendAckToClient(@NonNull String clientId, @NonNull AckMessage ackMessage) {
        if (protocolGateway == null) {
            return;
        }
        protocolGateway.sendAck(ackMessage, rawMessage -> sendMessageToClient(clientId, rawMessage));
    }

    public void broadcastAck(@NonNull AckMessage ackMessage) {
        if (protocolGateway == null) {
            return;
        }
        protocolGateway.sendAck(ackMessage, this::broadcastMessage);
    }

    @Nullable
    public TransferProgress sendTransferBytesToClient(
            @NonNull String clientId,
            @NonNull TransferMetadata metadata,
            @NonNull byte[] payload,
            @Nullable TransferProgressListener listener
    ) {
        if (protocolGateway == null) {
            return null;
        }
        return protocolGateway.sendTransferBytes(metadata, payload, rawMessage -> sendMessageToClient(clientId, rawMessage), listener);
    }

    @Nullable
    public TransferProgress sendTransferFileToClient(
            @NonNull String clientId,
            @NonNull TransferMetadata metadata,
            @NonNull File file,
            @Nullable TransferProgressListener listener
    ) throws IOException {
        if (protocolGateway == null) {
            return null;
        }
        return protocolGateway.sendTransferFile(metadata, file, rawMessage -> sendMessageToClient(clientId, rawMessage), listener);
    }

    @Nullable
    public StreamSender createStreamSender(@NonNull StreamMetadata metadata) {
        if (protocolGateway == null) {
            return null;
        }
        return protocolGateway.createStreamSender(metadata);
    }

    @Nullable
    public StreamStats sendStreamPayloadToClient(
            @NonNull String clientId,
            @NonNull StreamSender streamSender,
            @NonNull byte[] payload,
            @Nullable StreamStatsListener listener
    ) {
        if (protocolGateway == null) {
            return null;
        }
        return protocolGateway.sendStreamPayload(streamSender, payload, rawMessage -> sendMessageToClient(clientId, rawMessage), listener);
    }

    @Nullable
    public StreamStats finishStreamToClient(
            @NonNull String clientId,
            @NonNull StreamSender streamSender,
            @Nullable StreamStatsListener listener
    ) {
        if (protocolGateway == null) {
            return null;
        }
        return protocolGateway.finishStream(streamSender, rawMessage -> sendMessageToClient(clientId, rawMessage), listener);
    }

    public void sendStreamFrameToClient(@NonNull String clientId, @NonNull StreamFrame streamFrame) {
        if (protocolGateway == null) {
            return;
        }
        protocolGateway.sendStreamFrame(streamFrame, rawMessage -> sendMessageToClient(clientId, rawMessage));
    }

    public void broadcastMessage(String message) {
        if (deviceManager == null) {
            JLog.w(TAG, "DeviceManager not initialized, cannot broadcast");
            traceWarn("广播失败，DeviceManager 尚未初始化");
            return;
        }

        trace("准备广播消息，length=" + (message == null ? 0 : message.length()));
        List<DeviceManager.DeviceConnection> targets = deviceManager.broadcastMessage(message);
        for (DeviceManager.DeviceConnection target : targets) {
            recordCommunication(target, DeviceHistoryStore.ACTION_SENT, message);
        }
    }

    public int getConnectedClientCount() {
        return deviceManager != null ? deviceManager.getConnectedDeviceCount() : 0;
    }

    public void stopServer() {
        if (!isRunning) {
            return;
        }

        JLog.i(TAG, "Stopping server...");
        trace("开始停止监听服务并关闭所有连接");
        isRunning = false;

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            JLog.e(TAG, "Error closing server socket", e);
            }
            serverSocket = null;
        }

        if (deviceManager != null) {
            deviceManager.closeAllDevices();
        }
        pendingArchiveStates.clear();

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        updateNotification("TCP 服务器已停止");
        JLog.i(TAG, "Server stopped");
        trace("监听服务已停止");
    }

    private void startServer() {
        if (isRunning) {
            JLog.w(TAG, "Server already running");
            traceWarn("忽略重复启动请求，服务已在运行");
            return;
        }

        try {
            executorService = Executors.newCachedThreadPool();
            if (deviceManager == null) {
                initDeviceManager();
            }

            serverSocket = new ServerSocket(ServerConstance.SERVER_PORT);
            isRunning = true;

            if (localIpAddress == null) {
                localIpAddress = getLocalIpAddressInternal();
            }

                JLog.i(TAG, "Server started on port " + ServerConstance.SERVER_PORT + ", IP: " + localIpAddress);
            trace("监听已启动，address=" + localIpAddress + ":" + ServerConstance.SERVER_PORT);

            postToListeners(listener -> listener.onServerStarted(localIpAddress));

            updateNotification("TCP 服务器运行中 - 端口：" + ServerConstance.SERVER_PORT);
                JLog.i(TAG, "Server started successfully");

            executorService.execute(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String remoteIp = clientSocket.getInetAddress().getHostAddress();
                    JLog.i(TAG, "Client connected: " + remoteIp);
                        trace("接受到新的模块连接，remoteIp=" + remoteIp + ", remotePort=" + clientSocket.getPort());

                        if (!isRunning || deviceManager == null) {
                        JLog.w(TAG, "Server is stopping, rejecting new connection: " + remoteIp);
                            traceWarn("服务停止过程中拒绝新连接，remoteIp=" + remoteIp);
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                            JLog.e(TAG, "Error closing rejected socket", e);
                            }
                            continue;
                        }

                        DeviceManager.DeviceConnection deviceConnection = new DeviceManager.DeviceConnection(
                                remoteIp,
                                null,
                                remoteIp,
                                clientSocket.getPort(),
                                System.currentTimeMillis()
                        );
                        deviceManager.addDevice(clientSocket, deviceConnection);
                    } catch (IOException e) {
                        if (isRunning) {
                    JLog.e(TAG, "Error accepting client", e);
                            traceError("接收客户端连接失败", e);
                            postToListeners(listener ->
                                    listener.onError("接受客户端失败：" + e.getMessage()));
                        } else {
                    JLog.d(TAG, "Server stopped, accept loop exiting");
                        }
                    }
                }
            });
        } catch (IOException e) {
                JLog.e(TAG, "Failed to start server", e);
                traceError("监听启动失败", e);
            isRunning = false;
            postToListeners(listener -> listener.onError("启动服务器失败：" + e.getMessage()));
        }
    }

    private void initDeviceManager() {
        deviceManager = new DeviceManager(executorService, java.nio.charset.StandardCharsets.UTF_8);
        deviceManager.setDeviceListener(new DeviceManager.DeviceListener() {
            @Override
            public void onDeviceConnected(DeviceManager.DeviceConnection device) {
        JLog.i(TAG, "Device connected: " + device.getInlineLabel());
                trace("模块接入完成，deviceId=" + device.getDeviceId()
                        + ", remote=" + device.getRemoteIp() + ":" + device.getRemotePort());
                if (heartbeatManager != null) {
                    heartbeatManager.onClientConnected(device.getDeviceId());
                }
                recordConnection(device, true);
                postToListeners(listener -> listener.onClientConnected(device.getDeviceId()));
            }

            @Override
            public void onDeviceDisconnected(DeviceManager.DeviceConnection device) {
        JLog.i(TAG, "========================================");
        JLog.i(TAG, "Device disconnected: " + device.getInlineLabel());
                trace("模块断开连接，deviceId=" + device.getDeviceId());
                if (heartbeatManager != null) {
                    heartbeatManager.onClientDisconnected(device.getDeviceId());
                }
                recordConnection(device, false);
        JLog.i(TAG, "========================================");

                postToListeners(listener -> listener.onClientDisconnected(device.getDeviceId()));
            }

            @Override
            public void onMessageReceived(DeviceManager.DeviceConnection device, String message) {
                if (heartbeatManager != null) {
                    heartbeatManager.onMessageReceived(device.getDeviceId());
                }
                trace("收到模块消息，deviceId=" + device.getDeviceId()
                        + ", length=" + (message == null ? 0 : message.length()));
                recordCommunication(device, DeviceHistoryStore.ACTION_RECEIVED, message);

                dispatchProtocolEvent(device.getDeviceId(), message);
                postToListeners(listener -> listener.onMessageReceived(device.getDeviceId(), message));
            }
        });
        JLog.i(TAG, "DeviceManager initialized");
    }

    private void destroyDeviceManager() {
        if (deviceManager != null) {
            deviceManager.closeAllDevices();
            deviceManager = null;
        }
        JLog.i(TAG, "DeviceManager destroyed");
    }

    private void initHeartbeatManager() {
        heartbeatManager = HeartbeatManager.getInstance();
        heartbeatManager.setHeartbeatSender(this::sendMessageToClient);
        JLog.i(TAG, "HeartbeatManager initialized");
    }

    private void destroyHeartbeatManager() {
        if (heartbeatManager != null) {
            heartbeatManager.destroy();
            heartbeatManager = null;
        }
        JLog.i(TAG, "HeartbeatManager destroyed");
    }

    private void recordConnection(DeviceManager.DeviceConnection device, boolean connected) {
        if (deviceHistoryStore == null || device == null) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String localIp = resolveLocalIpAddress();
        String archiveDeviceId = device.getArchiveDeviceId();
        if (!TextUtils.isEmpty(archiveDeviceId)) {
            trace("连接事件直接归档，deviceId=" + archiveDeviceId + ", connected=" + connected);
            deviceHistoryStore.recordConnectionAt(
                    archiveDeviceId,
                    archiveDeviceId,
                    device.getRemoteIp(),
                    device.getRemotePort(),
                    localIp,
                    ServerConstance.SERVER_PORT,
                    connected,
                    timestamp
            );
            return;
        }
        if (!isRunning) {
            traceWarn("服务未运行，忽略连接事件缓存，sessionId=" + device.getDeviceId());
            return;
        }

        bufferPendingLog(device, new PendingLogRecord(
                timestamp,
                DeviceHistoryStore.CATEGORY_CONNECTION,
                connected ? DeviceHistoryStore.ACTION_CONNECTED : DeviceHistoryStore.ACTION_DISCONNECTED,
                null,
                device.getRemoteIp(),
                device.getRemotePort(),
                localIp,
                ServerConstance.SERVER_PORT
        ));
        if (!connected) {
            markSessionDisconnected(device.getDeviceId());
        }
        trace("连接事件已缓存，sessionId=" + device.getDeviceId() + ", connected=" + connected);
        requestMacResolution(device);
    }

    private void recordCommunication(DeviceManager.DeviceConnection device, String action, String message) {
        if (deviceHistoryStore == null || device == null) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        String localIp = resolveLocalIpAddress();
        String archiveDeviceId = device.getArchiveDeviceId();
        if (!TextUtils.isEmpty(archiveDeviceId)) {
            trace("通信事件直接归档，deviceId=" + archiveDeviceId + ", action=" + action);
            deviceHistoryStore.recordCommunicationAt(
                    archiveDeviceId,
                    archiveDeviceId,
                    device.getRemoteIp(),
                    device.getRemotePort(),
                    localIp,
                    ServerConstance.SERVER_PORT,
                    action,
                    message,
                    timestamp
            );
            return;
        }
        if (!isRunning) {
            traceWarn("服务未运行，忽略通信事件缓存，sessionId=" + device.getDeviceId());
            return;
        }

        bufferPendingLog(device, new PendingLogRecord(
                timestamp,
                DeviceHistoryStore.CATEGORY_COMMUNICATION,
                action,
                message,
                device.getRemoteIp(),
                device.getRemotePort(),
                localIp,
                ServerConstance.SERVER_PORT
        ));
        trace("通信事件已缓存，sessionId=" + device.getDeviceId() + ", action=" + action);
        requestMacResolution(device);
    }

    private void bufferPendingLog(DeviceManager.DeviceConnection device, PendingLogRecord logRecord) {
        PendingArchiveState state = pendingArchiveStates.computeIfAbsent(
                device.getDeviceId(),
                key -> new PendingArchiveState()
        );
        state.addLog(logRecord);
    }

    private void markSessionDisconnected(String sessionId) {
        PendingArchiveState state = pendingArchiveStates.get(sessionId);
        if (state != null) {
            state.markDisconnected();
        }
    }

    private void requestMacResolution(DeviceManager.DeviceConnection device) {
        if (device == null || device.hasResolvedMac() || executorService == null) {
            return;
        }

        PendingArchiveState state = pendingArchiveStates.computeIfAbsent(
                device.getDeviceId(),
                key -> new PendingArchiveState()
        );
        if (!state.tryStartDiscovery(MAX_MAC_DISCOVERY_ROUNDS)) {
            if (state.isDisconnected() && !state.isDiscoveryInProgress()) {
                pendingArchiveStates.remove(device.getDeviceId(), state);
            }
            traceWarn("跳过 MAC 解析调度，sessionId=" + device.getDeviceId());
            return;
        }

        try {
            trace("开始调度 MAC 解析，sessionId=" + device.getDeviceId() + ", remoteIp=" + device.getRemoteIp());
            executorService.execute(() -> {
                String macAddress = macDiscoveryClient.queryMacAddress(device.getRemoteIp());
                if (!TextUtils.isEmpty(macAddress)) {
                    handleResolvedMac(device, macAddress);
                    return;
                }
                handleMacResolutionFailed(device.getDeviceId());
            });
        } catch (Exception e) {
            JLog.e(TAG, "Failed to schedule MAC discovery for " + device.getDeviceId(), e);
            traceError("调度 MAC 解析失败，sessionId=" + device.getDeviceId(), e);
            handleMacResolutionFailed(device.getDeviceId());
        }
    }

    private void handleResolvedMac(DeviceManager.DeviceConnection device, String macAddress) {
        String normalizedMac = DeviceHistoryStore.normalizeMacAddress(macAddress);
        if (TextUtils.isEmpty(normalizedMac)) {
            handleMacResolutionFailed(device.getDeviceId());
            return;
        }

        JLog.i(TAG, "Resolved device MAC [" + normalizedMac + "] for session [" + device.getDeviceId() + "]");
        trace("MAC 解析成功，sessionId=" + device.getDeviceId() + ", mac=" + normalizedMac);
        device.updateMacAddress(normalizedMac);

        PendingArchiveState state = pendingArchiveStates.remove(device.getDeviceId());
        if (state != null) {
            state.finishDiscovery();
            for (PendingLogRecord logRecord : state.consumeLogs()) {
                persistPendingLog(normalizedMac, logRecord);
            }
        }

        postToListeners(listener -> listener.onClientIdentityResolved(device.getDeviceId(), normalizedMac));
    }

    private void handleMacResolutionFailed(String sessionId) {
        PendingArchiveState state = pendingArchiveStates.get(sessionId);
        if (state == null) {
            return;
        }

        state.finishDiscovery();
        if (state.isDisconnected()) {
            JLog.w(TAG, "MAC resolution failed for disconnected session: " + sessionId);
            traceWarn("MAC 解析失败且会话已断开，sessionId=" + sessionId);
            pendingArchiveStates.remove(sessionId, state);
        }
    }

    private void persistPendingLog(String archiveDeviceId, PendingLogRecord logRecord) {
        if (deviceHistoryStore == null || logRecord == null || TextUtils.isEmpty(archiveDeviceId)) {
            return;
        }

        if (DeviceHistoryStore.CATEGORY_CONNECTION.equals(logRecord.category)) {
            trace("回放缓存连接日志，deviceId=" + archiveDeviceId + ", action=" + logRecord.action);
            deviceHistoryStore.recordConnectionAt(
                    archiveDeviceId,
                    archiveDeviceId,
                    logRecord.remoteIp,
                    logRecord.remotePort,
                    logRecord.localIp,
                    logRecord.localPort,
                    DeviceHistoryStore.ACTION_CONNECTED.equals(logRecord.action),
                    logRecord.timestamp
            );
            return;
        }

        trace("回放缓存通信日志，deviceId=" + archiveDeviceId + ", action=" + logRecord.action);
        deviceHistoryStore.recordCommunicationAt(
                archiveDeviceId,
                archiveDeviceId,
                logRecord.remoteIp,
                logRecord.remotePort,
                logRecord.localIp,
                logRecord.localPort,
                logRecord.action,
                logRecord.message,
                logRecord.timestamp
        );
    }

    private String resolveLocalIpAddress() {
        if (localIpAddress == null) {
            localIpAddress = getLocalIpAddressInternal();
        }
        return localIpAddress;
    }

    private void acquireWakeLocks() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "TcpServerService::wakeLock"
                );
                wakeLock.setReferenceCounted(false);
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
            JLog.i(TAG, "Power wake lock acquired");
                }
            }

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                        "TcpServerService::wifiLock"
                );
                wifiLock.setReferenceCounted(false);
                if (!wifiLock.isHeld()) {
                    wifiLock.acquire();
            JLog.i(TAG, "WiFi wake lock acquired");
                }
            }
        } catch (Exception e) {
            JLog.e(TAG, "Failed to acquire wake locks", e);
        }
    }

    private void releaseWakeLocks() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
                wifiLock = null;
            }
        } catch (Exception e) {
            JLog.e(TAG, "Failed to release wake locks", e);
        }
    }

    private String getLocalIpAddressInternal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            JLog.e(TAG, "Failed to resolve local IP", e);
        }
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "TCP 服务器服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持 TCP 服务器在后台运行");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WiFi模块通信服务器")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String content) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(content));
        }
    }

    private void dispatchProtocolEvent(@NonNull String clientId, @Nullable String rawMessage) {
        if (protocolGateway == null) {
            return;
        }
        ProtocolInboundEvent event = protocolGateway.resolveInbound(rawMessage);
        postToListeners(listener -> listener.onProtocolEvent(clientId, event));
        if (event.isInvalid()) {
            postToListeners(listener -> listener.onProtocolError(clientId, event.getRawMessage(), event.getErrorMessage()));
        }
    }

    private void postToListeners(@NonNull ListenerAction action) {
        if (mainHandler == null || messageListeners.isEmpty()) {
            return;
        }
        mainHandler.post(() -> {
            for (OnMessageListener listener : messageListeners) {
                try {
                    action.onCallback(listener);
                } catch (Exception e) {
                    traceError("分发服务回调失败", e);
                }
            }
        });
    }

    private interface ListenerAction {
        void onCallback(@NonNull OnMessageListener listener);
    }

    private void trace(String message) {
        DLog.i(TAG, message);
    }

    private void traceWarn(String message) {
        DLog.w(TAG, message);
    }

    private void traceError(String message, Throwable throwable) {
        DLog.e(TAG, message, throwable);
    }
}

