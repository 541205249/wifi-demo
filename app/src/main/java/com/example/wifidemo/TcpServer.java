package com.example.wifidemo;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private static final String TAG = "TcpServer";
    private static final int DEFAULT_PORT = 9111;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private boolean isRunning = false;
    private ClientHandler clientHandler;
    private OnMessageListener messageListener;

    public interface OnMessageListener {
        void onMessageReceived(String message);
        void onClientConnected();
        void onClientDisconnected();
        void onError(String error);
    }

    public void setOnMessageListener(OnMessageListener listener) {
        this.messageListener = listener;
    }

    public boolean start(int port) {
        if (isRunning) {
            Log.w(TAG, "Server already running");
            return false;
        }

        try {
            executorService = Executors.newCachedThreadPool();
            serverSocket = new ServerSocket(port);
            isRunning = true;
            Log.i(TAG, "Server started on port " + port);

            executorService.execute(() -> {
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Log.i(TAG, "Client connected: " + clientSocket.getInetAddress());

                        clientHandler = new ClientHandler(clientSocket);
                        executorService.execute(clientHandler);

                        if (messageListener != null) {
                            messageListener.onClientConnected();
                        }
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client", e);
                            if (messageListener != null) {
                                messageListener.onError("接受客户端失败：" + e.getMessage());
                            }
                        }
                    }
                }
            });

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to start server", e);
            if (messageListener != null) {
                messageListener.onError("启动服务器失败：" + e.getMessage());
            }
            return false;
        }
    }

    public boolean start() {
        return start(DEFAULT_PORT);
    }

    public void stop() {
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

        Log.i(TAG, "Server stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void sendMessageToClient(String message) {
        if (clientHandler != null && clientHandler.isConnected()) {
            executorService.execute(() -> clientHandler.sendMessage(message));
        } else {
            Log.w(TAG, "No client connected");
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
                            messageListener.onMessageReceived(received);
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    Log.e(TAG, "Client disconnected unexpectedly", e);
                    if (messageListener != null) {
                        messageListener.onClientDisconnected();
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
                messageListener.onClientDisconnected();
            }
        }
    }
}
