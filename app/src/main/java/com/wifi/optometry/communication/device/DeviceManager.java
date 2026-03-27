package com.wifi.optometry.communication.device;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * WiFi 模块设备管理器。
 * 负责管理所有 TCP 客户端连接，包括连接的添加、移除、消息发送等。
 */
public class DeviceManager {
    private static final String TAG = "DeviceManager";

    private final Map<String, ClientHandler> deviceMap;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Charset messageCharset;

    private DeviceListener deviceListener;

    public static final class DeviceConnection {
        private final String deviceId;
        private final String remoteIp;
        private final int remotePort;
        private final long connectedAt;
        private volatile String macAddress;

        public DeviceConnection(
                String deviceId,
                String macAddress,
                String remoteIp,
                int remotePort,
                long connectedAt
        ) {
            this.deviceId = deviceId;
            this.macAddress = DeviceHistoryStore.normalizeMacAddress(macAddress);
            this.remoteIp = remoteIp;
            this.remotePort = remotePort;
            this.connectedAt = connectedAt;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public boolean hasResolvedMac() {
            return !TextUtils.isEmpty(macAddress);
        }

        public String getArchiveDeviceId() {
            return hasResolvedMac() ? macAddress : null;
        }

        public boolean updateMacAddress(String macAddress) {
            String normalizedMac = DeviceHistoryStore.normalizeMacAddress(macAddress);
            if (TextUtils.equals(this.macAddress, normalizedMac)) {
                return false;
            }
            this.macAddress = normalizedMac;
            return !TextUtils.isEmpty(this.macAddress);
        }

        public String getRemoteIp() {
            return remoteIp;
        }

        public int getRemotePort() {
            return remotePort;
        }

        public long getConnectedAt() {
            return connectedAt;
        }

        public String getInlineLabel() {
            StringBuilder builder = new StringBuilder();
            builder.append(!TextUtils.isEmpty(macAddress) ? macAddress : "未识别 MAC");
            if (!TextUtils.isEmpty(remoteIp)) {
                builder.append(" (").append(remoteIp);
                if (remotePort > 0) {
                    builder.append(":").append(remotePort);
                }
                builder.append(")");
            }
            return builder.toString();
        }

        public String getSelectionLabel() {
            StringBuilder builder = new StringBuilder();
            builder.append(!TextUtils.isEmpty(macAddress) ? macAddress : "未识别 MAC");
            if (!TextUtils.isEmpty(remoteIp)) {
                builder.append(" | ").append(remoteIp);
                if (remotePort > 0) {
                    builder.append(":").append(remotePort);
                }
            }
            return builder.toString();
        }
    }

    public interface DeviceListener {
        void onDeviceConnected(DeviceConnection device);

        void onDeviceDisconnected(DeviceConnection device);

        void onMessageReceived(DeviceConnection device, String message);
    }

    public DeviceManager(ExecutorService executorService) {
        this(executorService, StandardCharsets.UTF_8);
    }

    public DeviceManager(ExecutorService executorService, Charset charset) {
        this.deviceMap = new ConcurrentHashMap<>();
        this.executorService = executorService;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.messageCharset = charset != null ? charset : StandardCharsets.UTF_8;
    }

    public void setDeviceListener(DeviceListener listener) {
        this.deviceListener = listener;
    }

    public void addDevice(Socket socket, DeviceConnection deviceConnection) {
        String deviceId = deviceConnection.getDeviceId();
            JLog.i(TAG, "Adding new device: " + deviceId);
        trace("接管模块连接，deviceId=" + deviceId
                + ", remote=" + deviceConnection.getRemoteIp() + ":" + deviceConnection.getRemotePort());

        if (executorService.isShutdown() || executorService.isTerminated()) {
                JLog.w(TAG, "ExecutorService is shutdown, cannot add device: " + deviceId);
            traceWarn("线程池不可用，拒绝接入模块 deviceId=" + deviceId);
            closeSocketQuietly(socket);
            return;
        }

        if (deviceMap.containsKey(deviceId)) {
            removeDevice(deviceId);
        }

        ClientHandler handler = new ClientHandler(socket, deviceConnection);
        deviceMap.put(deviceId, handler);

        try {
            executorService.execute(handler);
                JLog.i(TAG, "Device added successfully, total devices: " + deviceMap.size());
            trace("模块接入调度成功，当前在线数=" + deviceMap.size());
        } catch (Exception e) {
                JLog.e(TAG, "Failed to execute ClientHandler for " + deviceId, e);
            traceError("模块读写任务调度失败，deviceId=" + deviceId, e);
            deviceMap.remove(deviceId);
            closeSocketQuietly(socket);
            return;
        }

        if (deviceListener != null) {
            mainHandler.post(() -> deviceListener.onDeviceConnected(deviceConnection));
        }
    }

    public void removeDevice(String deviceId) {
        JLog.i(TAG, "========================================");
        JLog.i(TAG, "Removing device: " + deviceId);
        trace("开始移除模块连接，deviceId=" + deviceId);

        ClientHandler handler = deviceMap.remove(deviceId);
        if (handler == null) {
            JLog.w(TAG, "No handler found for device: " + deviceId);
            traceWarn("移除模块失败，未找到连接句柄 deviceId=" + deviceId);
            JLog.i(TAG, "========================================");
            return;
        }

        handler.stop();
        if (deviceListener != null) {
            DeviceConnection deviceConnection = handler.getDeviceConnection();
            mainHandler.post(() -> deviceListener.onDeviceDisconnected(deviceConnection));
        }

        JLog.i(TAG, "Device removed, remaining devices: " + deviceMap.size());
        trace("模块移除完成，剩余在线数=" + deviceMap.size());
        JLog.i(TAG, "========================================");
    }

    public DeviceConnection sendMessageToDevice(String deviceId, String message) {
        ClientHandler handler = deviceMap.get(deviceId);
        if (handler == null || !handler.isConnected()) {
            JLog.w(TAG, "Device not found or not connected: " + deviceId);
            traceWarn("发送消息失败，模块未在线 deviceId=" + deviceId);
            return null;
        }

        try {
            trace("准备定向发送消息，deviceId=" + deviceId + ", length=" + (message == null ? 0 : message.length()));
            executorService.execute(() -> handler.sendMessage(message));
            return handler.getDeviceConnection();
        } catch (Exception e) {
            JLog.e(TAG, "Failed to schedule message for device [" + deviceId + "]", e);
            traceError("定向发送任务调度失败，deviceId=" + deviceId, e);
            return null;
        }
    }

    public List<DeviceConnection> broadcastMessage(String message) {
        List<DeviceConnection> targets = new ArrayList<>();
        if (deviceMap.isEmpty()) {
            JLog.w(TAG, "No devices connected, cannot broadcast");
            traceWarn("广播失败，当前没有在线模块");
            return targets;
        }

        for (ClientHandler handler : deviceMap.values()) {
            if (!handler.isConnected()) {
                continue;
            }

            try {
                executorService.execute(() -> handler.sendMessage(message));
                targets.add(handler.getDeviceConnection());
                trace("广播任务已调度，deviceId=" + handler.getDeviceConnection().getDeviceId());
            } catch (Exception e) {
                JLog.e(TAG, "Failed to schedule broadcast for [" + handler.getDeviceConnection().getDeviceId() + "]", e);
                traceError("广播任务调度失败，deviceId=" + handler.getDeviceConnection().getDeviceId(), e);
            }
        }

        JLog.i(TAG, "Broadcasted message to " + targets.size() + " devices");
        trace("广播调度完成，targetCount=" + targets.size());
        return targets;
    }

    public int getConnectedDeviceCount() {
        return deviceMap.size();
    }

    public String[] getConnectedDeviceIds() {
        return deviceMap.keySet().toArray(new String[0]);
    }

    public boolean isDeviceConnected(String deviceId) {
        ClientHandler handler = deviceMap.get(deviceId);
        return handler != null && handler.isConnected();
    }

    public List<DeviceConnection> getConnectedDevices() {
        List<DeviceConnection> connections = new ArrayList<>();
        for (ClientHandler handler : deviceMap.values()) {
            if (handler.isConnected()) {
                connections.add(handler.getDeviceConnection());
            }
        }
        connections.sort((left, right) -> Long.compare(right.getConnectedAt(), left.getConnectedAt()));
        return connections;
    }

    public DeviceConnection getConnectedDevice(String deviceId) {
        ClientHandler handler = deviceMap.get(deviceId);
        if (handler == null || !handler.isConnected()) {
            return null;
        }
        return handler.getDeviceConnection();
    }

    public void closeAllDevices() {
        JLog.i(TAG, "Closing all devices...");
        trace("开始关闭全部模块连接，count=" + deviceMap.size());
        for (String deviceId : new ArrayList<>(deviceMap.keySet())) {
            removeDevice(deviceId);
        }
        JLog.i(TAG, "All devices closed");
    }

    public ClientHandler getDeviceHandler(String deviceId) {
        return deviceMap.get(deviceId);
    }

    public class ClientHandler implements Runnable {
        private final DeviceConnection deviceConnection;
        private final String deviceId;

        private Socket socket;
        private OutputStream out;
        private InputStream in;
        private boolean connected = true;
        private boolean cleanedUp = false;

        public ClientHandler(Socket socket, DeviceConnection deviceConnection) {
            this.socket = socket;
            this.deviceConnection = deviceConnection;
            this.deviceId = deviceConnection.getDeviceId();
        }

        @Override
        public void run() {
            try {
                out = socket.getOutputStream();
                in = socket.getInputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;

                while (connected && (bytesRead = in.read(buffer)) != -1) {
                    if (bytesRead <= 0) {
                        continue;
                    }

                    String received = decodeMessage(buffer, 0, bytesRead);
                    JLog.i(TAG, "Received from device [" + deviceId + "]: " + received + " (bytes: " + bytesRead + ")");
                    trace("模块消息解码完成，deviceId=" + deviceId + ", bytes=" + bytesRead);

                    if (deviceListener != null) {
                        mainHandler.post(() -> deviceListener.onMessageReceived(deviceConnection, received));
                    }
                }
            } catch (IOException e) {
                if (connected) {
                JLog.e(TAG, "========================================");
                JLog.e(TAG, "Device [" + deviceId + "] disconnected unexpectedly");
                JLog.e(TAG, "Exception: " + e.getMessage());
                JLog.e(TAG, "========================================");
                    traceError("模块异常断开，deviceId=" + deviceId, e);
                }
            } finally {
                cleanup(true);
            }
        }

        public void sendMessage(String message) {
            if (out == null) {
                JLog.w(TAG, "Output stream is not ready for device [" + deviceId + "]");
                traceWarn("输出流尚未就绪，无法发送消息 deviceId=" + deviceId);
                return;
            }

            try {
                out.write((message + "\n").getBytes(messageCharset));
                out.flush();
                JLog.i(TAG, "Sent to device [" + deviceId + "]: " + message);
                trace("消息已发送到模块，deviceId=" + deviceId + ", length=" + (message == null ? 0 : message.length()));
            } catch (IOException e) {
                JLog.e(TAG, "Failed to send message to device [" + deviceId + "]", e);
                traceError("模块消息发送失败，deviceId=" + deviceId, e);
                cleanup(true);
            }
        }

        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed();
        }

        public DeviceConnection getDeviceConnection() {
            return deviceConnection;
        }

        public void stop() {
            cleanup(false);
        }

        private synchronized void cleanup(boolean notifyDisconnect) {
            if (cleanedUp) {
                return;
            }
            cleanedUp = true;
            connected = false;

            closeStreamQuietly(in, "input", deviceId);
            closeStreamQuietly(out, "output", deviceId);
            closeSocketQuietly(socket);

            in = null;
            out = null;
            socket = null;

            JLog.i(TAG, "========================================");
            JLog.i(TAG, "Device [" + deviceId + "] connection fully cleaned up");
            JLog.i(TAG, "========================================");
            trace("模块连接资源已清理，deviceId=" + deviceId + ", notifyDisconnect=" + notifyDisconnect);

            boolean removed = deviceMap.remove(deviceId, this);
            if (notifyDisconnect && removed && deviceListener != null) {
                mainHandler.post(() -> deviceListener.onDeviceDisconnected(deviceConnection));
            }
        }

        private String decodeMessage(byte[] buffer, int offset, int length) {
            try {
                String decoded = new String(buffer, offset, length, messageCharset);
                if (!decoded.contains("?") || isValidUtf8(buffer, offset, length)) {
                    return decoded;
                }
            } catch (Exception e) {
            JLog.d(TAG, "Failed to decode with configured charset, trying fallback");
                traceWarn("默认字符集解码失败，尝试回退 deviceId=" + deviceId);
            }

            try {
                String gbkDecoded = new String(buffer, offset, length, Charset.forName("GBK"));
                if (!gbkDecoded.contains("?")) {
            JLog.d(TAG, "Successfully decoded with GBK charset");
                    trace("使用 GBK 回退解码成功，deviceId=" + deviceId);
                    return gbkDecoded;
                }
            } catch (Exception e) {
            JLog.d(TAG, "Failed to decode with GBK charset");
                traceWarn("GBK 回退解码失败，deviceId=" + deviceId);
            }

            try {
                String isoDecoded = new String(buffer, offset, length, Charset.forName("ISO-8859-1"));
                if (!isoDecoded.contains("?")) {
            JLog.d(TAG, "Successfully decoded with ISO-8859-1 charset");
                    trace("使用 ISO-8859-1 回退解码成功，deviceId=" + deviceId);
                    return isoDecoded;
                }
            } catch (Exception e) {
            JLog.d(TAG, "Failed to decode with ISO-8859-1 charset");
                traceWarn("ISO-8859-1 回退解码失败，deviceId=" + deviceId);
            }

            return new String(buffer, offset, length, StandardCharsets.UTF_8);
        }

        private boolean isValidUtf8(byte[] buffer, int offset, int length) {
            int index = offset;
            while (index < offset + length) {
                byte current = buffer[index];
                if ((current & 0x80) == 0) {
                    index++;
                } else if ((current & 0xE0) == 0xC0) {
                    if (index + 1 >= offset + length || (buffer[index + 1] & 0xC0) != 0x80) {
                        return false;
                    }
                    index += 2;
                } else if ((current & 0xF0) == 0xE0) {
                    if (index + 2 >= offset + length
                            || (buffer[index + 1] & 0xC0) != 0x80
                            || (buffer[index + 2] & 0xC0) != 0x80) {
                        return false;
                    }
                    index += 3;
                } else if ((current & 0xF8) == 0xF0) {
                    if (index + 3 >= offset + length
                            || (buffer[index + 1] & 0xC0) != 0x80
                            || (buffer[index + 2] & 0xC0) != 0x80
                            || (buffer[index + 3] & 0xC0) != 0x80) {
                        return false;
                    }
                    index += 4;
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    private void closeStreamQuietly(java.io.Closeable closeable, String streamName, String deviceId) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
                JLog.d(TAG, "Device [" + deviceId + "] " + streamName + " stream closed");
        } catch (IOException e) {
                JLog.e(TAG, "Error closing " + streamName + " stream for device [" + deviceId + "]", e);
            traceError("关闭流失败，deviceId=" + deviceId + ", stream=" + streamName, e);
        }
    }

    private void closeSocketQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
                JLog.e(TAG, "Error closing socket", e);
            traceError("关闭 Socket 失败", e);
        }
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

