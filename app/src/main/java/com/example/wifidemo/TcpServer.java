package com.example.wifidemo;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {
    private static final String TAG = "TcpServer";
    private static final int DEFAULT_PORT = 8888;

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
            clientHandler.sendMessage(message);
        } else {
            Log.w(TAG, "No client connected");
        }
    }

    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean connected = true;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String inputLine;
                while (connected && (inputLine = in.readLine()) != null) {
                    Log.i(TAG, "Received from client: " + inputLine);
                    if (messageListener != null) {
                        messageListener.onMessageReceived(inputLine);
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
                out.println(message);
                Log.i(TAG, "Sent to client: " + message);
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
