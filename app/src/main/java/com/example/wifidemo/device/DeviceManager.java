package com.example.wifidemo.device;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * WiFi 模块设备管理器
 * 负责管理所有 TCP 客户端连接，包括连接的添加、移除、消息发送等
 * 
 * 功能特性：
 * 1. 一对多连接管理
 * 2. 线程安全的并发控制
 * 3. 支持广播和单播消息
 * 4. 与 TcpServerService 解耦
 */
public class DeviceManager {
    private static final String TAG = "DeviceManager";
    
    // 已连接的设备集合，使用 ConcurrentHashMap 保证线程安全
    private final Map<String, ClientHandler> deviceMap;
    
    // 线程池，用于异步发送消息
    private final ExecutorService executorService;
    
    // 主线程 Handler，用于回调
    private final Handler mainHandler;
    
    // 设备连接状态监听器
    private DeviceListener deviceListener;
    
    /**
     * 设备监听器接口
     * 用于通知外部设备连接状态变化
     */
    public interface DeviceListener {
        /**
         * 设备连接时回调
         * @param deviceId 设备 ID（IP 地址）
         */
        void onDeviceConnected(String deviceId);
        
        /**
         * 设备断开时回调
         * @param deviceId 设备 ID（IP 地址）
         */
        void onDeviceDisconnected(String deviceId);
        
        /**
         * 收到设备消息时回调
         * @param deviceId 设备 ID（IP 地址）
         * @param message 消息内容
         */
        void onMessageReceived(String deviceId, String message);
    }
    
    /**
     * 构造函数
     * @param executorService 线程池，用于异步任务执行
     */
    public DeviceManager(ExecutorService executorService) {
        this.deviceMap = new ConcurrentHashMap<>();
        this.executorService = executorService;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 设置设备监听器
     * @param listener 监听器实例
     */
    public void setDeviceListener(DeviceListener listener) {
        this.deviceListener = listener;
    }
    
    /**
     * 添加新设备连接
     * @param socket TCP Socket 连接
     * @param deviceId 设备 ID（IP 地址）
     */
    public void addDevice(Socket socket, String deviceId) {
        Log.i(TAG, "Adding new device: " + deviceId);
        
        ClientHandler handler = new ClientHandler(socket, deviceId);
        deviceMap.put(deviceId, handler);
        
        // 在线程池中执行客户端处理器
        executorService.execute(handler);
        
        // 通知监听器
        if (deviceListener != null) {
            mainHandler.post(() -> deviceListener.onDeviceConnected(deviceId));
        }
        
        Log.i(TAG, "Device added successfully, total devices: " + deviceMap.size());
    }
    
    /**
     * 移除设备连接
     * @param deviceId 设备 ID（IP 地址）
     */
    public void removeDevice(String deviceId) {
        Log.i(TAG, "Removing device: " + deviceId);
        
        ClientHandler handler = deviceMap.remove(deviceId);
        if (handler != null) {
            handler.stop();
        }
        
        // 通知监听器
        if (deviceListener != null) {
            mainHandler.post(() -> deviceListener.onDeviceDisconnected(deviceId));
        }
        
        Log.i(TAG, "Device removed, remaining devices: " + deviceMap.size());
    }
    
    /**
     * 向指定设备发送消息
     * @param deviceId 设备 ID（IP 地址）
     * @param message 消息内容
     */
    public void sendMessageToDevice(String deviceId, String message) {
        ClientHandler handler = deviceMap.get(deviceId);
        if (handler != null && handler.isConnected()) {
            executorService.execute(() -> handler.sendMessage(message));
        } else {
            Log.w(TAG, "Device not found or not connected: " + deviceId);
        }
    }
    
    /**
     * 向所有设备广播消息
     * @param message 消息内容
     */
    public void broadcastMessage(String message) {
        if (deviceMap.isEmpty()) {
            Log.w(TAG, "No devices connected, cannot broadcast");
            return;
        }
        
        for (ClientHandler handler : deviceMap.values()) {
            if (handler.isConnected()) {
                executorService.execute(() -> handler.sendMessage(message));
            }
        }
        
        Log.i(TAG, "Broadcasted message to " + deviceMap.size() + " devices");
    }
    
    /**
     * 获取已连接设备的数量
     * @return 设备数量
     */
    public int getConnectedDeviceCount() {
        return deviceMap.size();
    }
    
    /**
     * 获取所有已连接的设备 ID 数组
     * @return 设备 ID 数组
     */
    public String[] getConnectedDeviceIds() {
        return deviceMap.keySet().toArray(new String[0]);
    }
    
    /**
     * 检查指定设备是否已连接
     * @param deviceId 设备 ID（IP 地址）
     * @return true-已连接，false-未连接
     */
    public boolean isDeviceConnected(String deviceId) {
        ClientHandler handler = deviceMap.get(deviceId);
        return handler != null && handler.isConnected();
    }
    
    /**
     * 关闭所有设备连接并清理资源
     */
    public void closeAllDevices() {
        Log.i(TAG, "Closing all devices...");
        
        // 关闭所有客户端连接
        for (ClientHandler handler : deviceMap.values()) {
            handler.stop();
        }
        
        deviceMap.clear();
        Log.i(TAG, "All devices closed");
    }
    
    /**
     * 获取设备处理器（用于心跳管理等高级功能）
     * @param deviceId 设备 ID
     * @return ClientHandler 实例，如果不存在则返回 null
     */
    public ClientHandler getDeviceHandler(String deviceId) {
        return deviceMap.get(deviceId);
    }
    
    /**
     * 客户端处理器
     * 负责处理单个设备的 TCP 连接和消息收发
     */
    public class ClientHandler implements Runnable {
        private Socket socket;
        private OutputStream out;
        private InputStream in;
        private String deviceId;
        private boolean connected = true;
        
        /**
         * 构造函数
         * @param socket TCP Socket 连接
         * @param deviceId 设备 ID（IP 地址）
         */
        public ClientHandler(Socket socket, String deviceId) {
            this.socket = socket;
            this.deviceId = deviceId;
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
                        Log.i(TAG, "Received from device [" + deviceId + "]: " + received + " (bytes: " + bytesRead + ")");
                        
                        // 通知监听器收到消息
                        if (deviceListener != null) {
                            mainHandler.post(() -> deviceListener.onMessageReceived(deviceId, received));
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    Log.e(TAG, "Device [" + deviceId + "] disconnected unexpectedly", e);
                }
            } finally {
                cleanup();
            }
        }
        
        /**
         * 发送消息到设备
         * @param message 消息内容
         */
        public void sendMessage(String message) {
            if (out != null) {
                try {
                    out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Log.i(TAG, "Sent to device [" + deviceId + "]: " + message);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to send message to device [" + deviceId + "]", e);
                }
            }
        }
        
        /**
         * 检查设备是否已连接
         * @return true-已连接，false-未连接
         */
        public boolean isConnected() {
            return connected && socket != null && !socket.isClosed();
        }
        
        /**
         * 停止设备连接
         */
        public void stop() {
            connected = false;
            cleanup();
        }
        
        /**
         * 清理资源
         */
        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error cleaning up device connection [" + deviceId + "]", e);
            }
            
            socket = null;
        }
        
        /**
         * 获取设备 ID
         * @return 设备 ID（IP 地址）
         */
        public String getDeviceId() {
            return deviceId;
        }
    }
}
