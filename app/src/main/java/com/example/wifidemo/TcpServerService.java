package com.example.wifidemo;

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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.wifidemo.device.DeviceHistoryStore;
import com.example.wifidemo.device.DeviceManager;
import com.example.wifidemo.device.NetworkIdentityResolver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServerService extends Service {
    private static final String TAG = "TcpServerService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "tcp_server_channel";

    private final IBinder binder = new TcpServerBinder();

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRunning = false;

    private DeviceManager deviceManager;
    private DeviceHistoryStore deviceHistoryStore;
    private OnMessageListener messageListener;
    private Handler mainHandler;
    private String localIpAddress;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private HeartbeatManager heartbeatManager;

    public interface OnMessageListener {
        void onMessageReceived(String clientId, String message);

        void onClientConnected(String clientId);

        void onClientDisconnected(String clientId);

        void onError(String error);

        void onServerStarted(String ipAddress);
    }

    public class TcpServerBinder extends Binder {
        public TcpServerService getService() {
            return TcpServerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        mainHandler = new Handler(Looper.getMainLooper());
        deviceHistoryStore = DeviceHistoryStore.getInstance(this);
        deviceHistoryStore.markAllDevicesOffline();
        createNotificationChannel();
        acquireWakeLocks();
        initHeartbeatManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
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
        Log.i(TAG, "Service destroyed");
        stopServer();
        releaseWakeLocks();
        destroyDeviceManager();
        destroyHeartbeatManager();
    }

    public void setOnMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }

    public void setLocalIpAddress(String ipAddress) {
        this.localIpAddress = ipAddress;
        Log.i(TAG, "Set local IP address from Activity: " + ipAddress);
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

        DeviceManager.DeviceConnection device = deviceManager.sendMessageToDevice(clientId, message);
        if (device != null) {
            recordCommunication(device, DeviceHistoryStore.ACTION_SENT, message);
        }
    }

    public void broadcastMessage(String message) {
        if (deviceManager == null) {
            Log.w(TAG, "DeviceManager not initialized, cannot broadcast");
            return;
        }

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

        Log.i(TAG, "Stopping server...");
        isRunning = false;

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket", e);
            }
            serverSocket = null;
        }

        if (deviceManager != null) {
            deviceManager.closeAllDevices();
        }

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        updateNotification("TCP 服务器已停止");
        Log.i(TAG, "Server stopped");
    }

    private void startServer() {
        if (isRunning) {
            Log.w(TAG, "Server already running");
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

            Log.i(TAG, "Server started on port " + ServerConstance.SERVER_PORT + ", IP: " + localIpAddress);

            if (messageListener != null) {
                mainHandler.post(() -> messageListener.onServerStarted(localIpAddress));
            }

            updateNotification("TCP 服务器运行中 - 端口：" + ServerConstance.SERVER_PORT);
            Log.i(TAG, "Server started successfully");

            executorService.execute(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        String remoteIp = clientSocket.getInetAddress().getHostAddress();
                        Log.i(TAG, "Client connected: " + remoteIp);

                        if (!isRunning || deviceManager == null) {
                            Log.w(TAG, "Server is stopping, rejecting new connection: " + remoteIp);
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Error closing rejected socket", e);
                            }
                            continue;
                        }

                        String macAddress = NetworkIdentityResolver.resolveMacAddress(remoteIp);
                        String deviceId = DeviceHistoryStore.createDeviceId(macAddress, remoteIp);
                        DeviceManager.DeviceConnection deviceConnection = new DeviceManager.DeviceConnection(
                                deviceId,
                                macAddress,
                                remoteIp,
                                clientSocket.getPort(),
                                System.currentTimeMillis()
                        );

                        deviceManager.addDevice(clientSocket, deviceConnection);
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client", e);
                            if (messageListener != null) {
                                mainHandler.post(() ->
                                        messageListener.onError("接受客户端失败：" + e.getMessage()));
                            }
                        } else {
                            Log.d(TAG, "Server stopped, accept loop exiting");
                        }
                    }
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server", e);
            isRunning = false;
            if (messageListener != null) {
                mainHandler.post(() -> messageListener.onError("启动服务器失败：" + e.getMessage()));
            }
        }
    }

    private void initDeviceManager() {
        deviceManager = new DeviceManager(executorService, java.nio.charset.StandardCharsets.UTF_8);
        deviceManager.setDeviceListener(new DeviceManager.DeviceListener() {
            @Override
            public void onDeviceConnected(DeviceManager.DeviceConnection device) {
                Log.i(TAG, "Device connected: " + device.getInlineLabel());
                if (heartbeatManager != null) {
                    heartbeatManager.onClientConnected(device.getDeviceId());
                }
                recordConnection(device, true);
                if (messageListener != null) {
                    mainHandler.post(() -> messageListener.onClientConnected(device.getDeviceId()));
                }
            }

            @Override
            public void onDeviceDisconnected(DeviceManager.DeviceConnection device) {
                Log.i(TAG, "========================================");
                Log.i(TAG, "Device disconnected: " + device.getInlineLabel());
                if (heartbeatManager != null) {
                    heartbeatManager.onClientDisconnected(device.getDeviceId());
                }
                recordConnection(device, false);
                Log.i(TAG, "========================================");

                if (messageListener != null) {
                    mainHandler.post(() -> messageListener.onClientDisconnected(device.getDeviceId()));
                }
            }

            @Override
            public void onMessageReceived(DeviceManager.DeviceConnection device, String message) {
                if (heartbeatManager != null) {
                    heartbeatManager.onMessageReceived(device.getDeviceId());
                }
                recordCommunication(device, DeviceHistoryStore.ACTION_RECEIVED, message);

                if (messageListener != null) {
                    mainHandler.post(() -> messageListener.onMessageReceived(device.getDeviceId(), message));
                }
            }
        });
        Log.i(TAG, "DeviceManager initialized");
    }

    private void destroyDeviceManager() {
        if (deviceManager != null) {
            deviceManager.closeAllDevices();
            deviceManager = null;
        }
        Log.i(TAG, "DeviceManager destroyed");
    }

    private void initHeartbeatManager() {
        heartbeatManager = HeartbeatManager.getInstance();
        heartbeatManager.setHeartbeatSender(this::sendMessageToClient);
        Log.i(TAG, "HeartbeatManager initialized");
    }

    private void destroyHeartbeatManager() {
        if (heartbeatManager != null) {
            heartbeatManager.destroy();
            heartbeatManager = null;
        }
        Log.i(TAG, "HeartbeatManager destroyed");
    }

    private void recordConnection(DeviceManager.DeviceConnection device, boolean connected) {
        if (deviceHistoryStore == null || device == null) {
            return;
        }

        deviceHistoryStore.recordConnection(
                device.getDeviceId(),
                device.getMacAddress(),
                device.getRemoteIp(),
                device.getRemotePort(),
                resolveLocalIpAddress(),
                ServerConstance.SERVER_PORT,
                connected
        );
    }

    private void recordCommunication(DeviceManager.DeviceConnection device, String action, String message) {
        if (deviceHistoryStore == null || device == null) {
            return;
        }

        deviceHistoryStore.recordCommunication(
                device.getDeviceId(),
                device.getMacAddress(),
                device.getRemoteIp(),
                device.getRemotePort(),
                resolveLocalIpAddress(),
                ServerConstance.SERVER_PORT,
                action,
                message
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
                    Log.i(TAG, "Power wake lock acquired");
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
                    Log.i(TAG, "WiFi wake lock acquired");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake locks", e);
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
            Log.e(TAG, "Failed to release wake locks", e);
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
            Log.e(TAG, "Failed to resolve local IP", e);
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
}
