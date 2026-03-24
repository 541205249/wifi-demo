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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private OnMessageListener messageListener;
    private Handler mainHandler;
    private String localIpAddress;
    
    // 电源锁和 WiFi 锁，确保后台持续运行
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

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

    public void setLocalIpAddress(String ipAddress) {
        this.localIpAddress = ipAddress;
        Log.i(TAG, "Set local IP address from Activity: " + ipAddress);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        acquireWakeLocks();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        startForeground(NOTIFICATION_ID, createNotification("TCP 服务器正在启动..."));
        startServer();
        return START_STICKY;
    }
    
    /**
     * 获取电源锁和 WiFi 锁，确保后台持续运行
     */
    private void acquireWakeLocks() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "TcpServerService::wakeLock"
                );
                wakeLock.setReferenceCounted(false);
            }
            
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "TcpServerService::wifiLock"
                );
                wifiLock.setReferenceCounted(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake locks", e);
        }
    }
    
    /**
     * 释放电源锁和 WiFi 锁
     */
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
    }

    public void setOnMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }

    public boolean isServerRunning() {
        return isRunning;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    /**
     * 获取所有已连接的客户端 ID 列表
     */
    public String[] getConnectedClientIds() {
        return clientMap.keySet().toArray(new String[0]);
    }

    /**
     * 向指定客户端发送消息
     * @param clientId 客户端 ID（IP 地址）
     * @param message 消息内容
     */
    public void sendMessageToClient(String clientId, String message) {
        ClientHandler handler = clientMap.get(clientId);
        if (handler != null && handler.isConnected()) {
            executorService.execute(() -> handler.sendMessage(message));
        } else {
            Log.w(TAG, "Client not found or not connected: " + clientId);
        }
    }

    /**
     * 向所有客户端发送消息
     * @param message 消息内容
     */
    public void broadcastMessage(String message) {
        if (clientMap.isEmpty()) {
            Log.w(TAG, "No clients connected");
            return;
        }
        
        for (ClientHandler handler : clientMap.values()) {
            if (handler.isConnected()) {
                executorService.execute(() -> handler.sendMessage(message));
            }
        }
        Log.i(TAG, "Broadcasted message to " + clientMap.size() + " clients");
    }

    /**
     * 获取已连接的客户端数量
     */
    public int getConnectedClientCount() {
        return clientMap.size();
    }

    private void startServer() {
        if (isRunning) {
            Log.w(TAG, "Server already running");
            return;
        }

        try {
            executorService = Executors.newCachedThreadPool();
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
                        Log.i(TAG, "Client connected: " + clientSocket.getInetAddress());

                        String clientId = clientSocket.getInetAddress().getHostAddress();
                        ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                        clientMap.put(clientId, clientHandler);
                        executorService.execute(clientHandler);

                        if (messageListener != null) {
                            mainHandler.post(() -> messageListener.onClientConnected(clientId));
                        }
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client", e);
                            if (messageListener != null) {
                                mainHandler.post(() ->
                                        messageListener.onError("接受客户端失败：" + e.getMessage()));
                            }
                        }
                    }
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server", e);
            isRunning = false;
            if (messageListener != null) {
                mainHandler.post(() ->
                        messageListener.onError("启动服务器失败：" + e.getMessage()));
            }
        }
    }

    public void stopServer() {
        if (!isRunning) {
            return;
        }

        Log.i(TAG, "Stopping server...");
        isRunning = false;

        // 关闭所有客户端连接
        for (ClientHandler handler : clientMap.values()) {
            handler.stop();
        }
        clientMap.clear();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket", e);
            }
            serverSocket = null;
        }

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }

        updateNotification("TCP 服务器已停止");
        Log.i(TAG, "Server stopped");
    }

    private String getLocalIpAddressInternal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private class ClientHandler implements Runnable {
        private Socket socket;
        private OutputStream out;
        private InputStream in;
        private String clientId;
        private boolean connected = true;

        public ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                out = socket.getOutputStream();
                in = socket.getInputStream();

                byte[] buffer = new byte[1024];
                int bytesRead;

                while (connected && (bytesRead = in.read(buffer)) != -1) {
                    if (bytesRead > 0) {
                        String received = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        Log.i(TAG, "Received from client [" + clientId + "]: " + received + " (bytes: " + bytesRead + ")");
                        if (messageListener != null) {
                            mainHandler.post(() -> messageListener.onMessageReceived(clientId, received));
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    Log.e(TAG, "Client [" + clientId + "] disconnected unexpectedly", e);
                    if (messageListener != null) {
                        mainHandler.post(() -> messageListener.onClientDisconnected(clientId));
                    }
                }
            } finally {
                cleanup();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                try {
                    out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Log.i(TAG, "Sent to client [" + clientId + "]: " + message);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send message to client [" + clientId + "]", e);
                }
            }
        }

        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed();
        }

        public void stop() {
            connected = false;
            cleanup();
        }

        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error cleaning up client connection [" + clientId + "]", e);
            }
            
            // 从客户端列表中移除
            clientMap.remove(clientId);
            socket = null;
            
            if (messageListener != null) {
                mainHandler.post(() -> messageListener.onClientDisconnected(clientId));
            }
        }
    }
}
