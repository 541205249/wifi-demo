package com.example.wifidemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
    private ClientHandler clientHandler;
    private OnMessageListener messageListener;
    private Handler mainHandler;
    private String localIpAddress;

    public interface OnMessageListener {
        void onMessageReceived(String message);
        void onClientConnected();
        void onClientDisconnected();
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

    public void sendMessageToClient(String message) {
        if (clientHandler != null && clientHandler.isConnected()) {
            executorService.execute(() -> clientHandler.sendMessage(message));
        } else {
            Log.w(TAG, "No client connected");
        }
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

                        clientHandler = new ClientHandler(clientSocket);
                        executorService.execute(clientHandler);

                        if (messageListener != null) {
                            mainHandler.post(() -> messageListener.onClientConnected());
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

        if (clientHandler != null) {
            clientHandler.stop();
        }

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
        private boolean connected = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
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
                        Log.i(TAG, "Received from client: " + received + " (bytes: " + bytesRead + ")");
                        if (messageListener != null) {
                            mainHandler.post(() -> messageListener.onMessageReceived(received));
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    Log.e(TAG, "Client disconnected unexpectedly", e);
                    if (messageListener != null) {
                        mainHandler.post(() -> messageListener.onClientDisconnected());
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
                    Log.i(TAG, "Sent to client: " + message);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send message", e);
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
                Log.e(TAG, "Error cleaning up client connection", e);
            }
            socket = null;
            if (messageListener != null) {
                mainHandler.post(() -> messageListener.onClientDisconnected());
            }
        }
    }
}
