package com.example.wifidemo.device;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

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
        Log.i(TAG, "Adding new device: " + deviceId);

        if (executorService.isShutdown() || executorService.isTerminated()) {
            Log.w(TAG, "ExecutorService is shutdown, cannot add device: " + deviceId);
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
            Log.i(TAG, "Device added successfully, total devices: " + deviceMap.size());
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute ClientHandler for " + deviceId, e);
            deviceMap.remove(deviceId);
            closeSocketQuietly(socket);
            return;
        }

        if (deviceListener != null) {
            mainHandler.post(() -> deviceListener.onDeviceConnected(deviceConnection));
        }
    }

    public void removeDevice(String deviceId) {
        Log.i(TAG, "========================================");
        Log.i(TAG, "Removing device: " + deviceId);

        ClientHandler handler = deviceMap.remove(deviceId);
        if (handler == null) {
            Log.w(TAG, "No handler found for device: " + deviceId);
            Log.i(TAG, "========================================");
            return;
        }

        handler.stop();
        if (deviceListener != null) {
            DeviceConnection deviceConnection = handler.getDeviceConnection();
            mainHandler.post(() -> deviceListener.onDeviceDisconnected(deviceConnection));
        }

        Log.i(TAG, "Device removed, remaining devices: " + deviceMap.size());
        Log.i(TAG, "========================================");
    }

    public DeviceConnection sendMessageToDevice(String deviceId, String message) {
        ClientHandler handler = deviceMap.get(deviceId);
        if (handler == null || !handler.isConnected()) {
            Log.w(TAG, "Device not found or not connected: " + deviceId);
            return null;
        }

        try {
            executorService.execute(() -> handler.sendMessage(message));
            return handler.getDeviceConnection();
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule message for device [" + deviceId + "]", e);
            return null;
        }
    }

    public List<DeviceConnection> broadcastMessage(String message) {
        List<DeviceConnection> targets = new ArrayList<>();
        if (deviceMap.isEmpty()) {
            Log.w(TAG, "No devices connected, cannot broadcast");
            return targets;
        }

        for (ClientHandler handler : deviceMap.values()) {
            if (!handler.isConnected()) {
                continue;
            }

            try {
                executorService.execute(() -> handler.sendMessage(message));
                targets.add(handler.getDeviceConnection());
            } catch (Exception e) {
                Log.e(TAG, "Failed to schedule broadcast for [" + handler.getDeviceConnection().getDeviceId() + "]", e);
            }
        }

        Log.i(TAG, "Broadcasted message to " + targets.size() + " devices");
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
        Log.i(TAG, "Closing all devices...");
        for (String deviceId : new ArrayList<>(deviceMap.keySet())) {
            removeDevice(deviceId);
        }
        Log.i(TAG, "All devices closed");
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
                    Log.i(TAG, "Received from device [" + deviceId + "]: " + received + " (bytes: " + bytesRead + ")");

                    if (deviceListener != null) {
                        mainHandler.post(() -> deviceListener.onMessageReceived(deviceConnection, received));
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    Log.e(TAG, "========================================");
                    Log.e(TAG, "Device [" + deviceId + "] disconnected unexpectedly");
                    Log.e(TAG, "Exception: " + e.getMessage());
                    Log.e(TAG, "========================================");
                }
            } finally {
                cleanup(true);
            }
        }

        public void sendMessage(String message) {
            if (out == null) {
                Log.w(TAG, "Output stream is not ready for device [" + deviceId + "]");
                return;
            }

            try {
                out.write((message + "\n").getBytes(messageCharset));
                out.flush();
                Log.i(TAG, "Sent to device [" + deviceId + "]: " + message);
            } catch (IOException e) {
                Log.e(TAG, "Failed to send message to device [" + deviceId + "]", e);
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

            Log.i(TAG, "========================================");
            Log.i(TAG, "Device [" + deviceId + "] connection fully cleaned up");
            Log.i(TAG, "========================================");

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
                Log.d(TAG, "Failed to decode with configured charset, trying fallback");
            }

            try {
                String gbkDecoded = new String(buffer, offset, length, Charset.forName("GBK"));
                if (!gbkDecoded.contains("?")) {
                    Log.d(TAG, "Successfully decoded with GBK charset");
                    return gbkDecoded;
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to decode with GBK charset");
            }

            try {
                String isoDecoded = new String(buffer, offset, length, Charset.forName("ISO-8859-1"));
                if (!isoDecoded.contains("?")) {
                    Log.d(TAG, "Successfully decoded with ISO-8859-1 charset");
                    return isoDecoded;
                }
            } catch (Exception e) {
                Log.d(TAG, "Failed to decode with ISO-8859-1 charset");
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
            Log.d(TAG, "Device [" + deviceId + "] " + streamName + " stream closed");
        } catch (IOException e) {
            Log.e(TAG, "Error closing " + streamName + " stream for device [" + deviceId + "]", e);
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
            Log.e(TAG, "Error closing socket", e);
        }
    }
}
