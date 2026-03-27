package com.wifi.optometry.communication;

import android.os.Handler;
import android.os.Looper;
import com.wifi.lib.log.JLog;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WiFi 模块心跳管理器
 * 负责维护 TCP 连接的保活机制，防止模块因长时间无通信而断开
 * 
 * 功能特性：
 * 1. 10 分钟无消息自动发送心跳
 * 2. 最长保活时间 8 小时，超时后停止心跳
 * 3. 与 TCP 连接代码完全解耦
 */
public class HeartbeatManager {
    private static final String TAG = "HeartbeatManager";
    
    /**
     * 心跳间隔时间：10 分钟（毫秒）
     * WiFi 模块 10 分钟无消息会自动断连，因此设置 10 分钟发送一次心跳
     */
    private static final long HEARTBEAT_INTERVAL = 10 * 60 * 1000; // 10 分钟
    
    /**
     * 最大保活时间：8 小时（毫秒）
     * 超过 8 小时后，即使有通信也停止心跳，避免资源浪费
     */
    private static final long MAX_KEEPALIVE_TIME = 8 * 60 * 60 * 1000; // 8 小时
    
    /**
     * 心跳消息内容，可根据实际模块协议调整
     */
    private static final String HEARTBEAT_MESSAGE = "PING";
    
    private static volatile HeartbeatManager instance;
    
    // 已启动心跳计时器的客户端集合
    private final Map<String, ClientHeartbeatInfo> clientHeartbeatMap;
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutor;
    
    // 线程池，用于异步发送心跳
    private final ExecutorService executorService;
    
    // 主线程 Handler，用于回调
    private final Handler mainHandler;
    
    // 心跳发送接口，由外部实现实际的发送逻辑
    private HeartbeatSender heartbeatSender;
    
    /**
     * 心跳发送接口
     * 用于解耦心跳管理和实际的 TCP 发送逻辑
     */
    public interface HeartbeatSender {
        /**
         * 向指定客户端发送心跳消息
         * @param clientId 客户端 ID（IP 地址）
         * @param message 心跳消息内容
         */
        void sendHeartbeat(String clientId, String message);
    }
    
    /**
     * 客户端心跳信息
     */
    private static class ClientHeartbeatInfo {
        // 最后一次收到消息的时间戳
        long lastMessageTime;
        // 心跳任务是否正在运行
        boolean isRunning;
        // 心跳定时任务引用，用于取消
        java.util.concurrent.ScheduledFuture<?> scheduledFuture;
        
        ClientHeartbeatInfo() {
            this.lastMessageTime = System.currentTimeMillis();
            this.isRunning = false;
        }
    }
    
    private HeartbeatManager() {
        clientHeartbeatMap = new ConcurrentHashMap<>();
        scheduledExecutor = Executors.newScheduledThreadPool(2);
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 获取单例实例
     */
    public static HeartbeatManager getInstance() {
        if (instance == null) {
            synchronized (HeartbeatManager.class) {
                if (instance == null) {
                    instance = new HeartbeatManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 设置心跳发送器
     * @param sender 心跳发送实现
     */
    public void setHeartbeatSender(HeartbeatSender sender) {
        this.heartbeatSender = sender;
    }
    
    /**
     * 客户端连接时调用，开始监控该客户端
     * @param clientId 客户端 ID（IP 地址）
     */
    public void onClientConnected(String clientId) {
        JLog.i(TAG, "Client connected, start monitoring: " + clientId);
        
        ClientHeartbeatInfo info = new ClientHeartbeatInfo();
        clientHeartbeatMap.put(clientId, info);
        
        // 启动心跳监控
        startHeartbeatMonitoring(clientId, info);
    }
    
    /**
     * 客户端断开时调用，停止该客户端的心跳
     * @param clientId 客户端 ID（IP 地址）
     */
    public void onClientDisconnected(String clientId) {
        JLog.i(TAG, "Client disconnected, stop heartbeat: " + clientId);
        
        ClientHeartbeatInfo info = clientHeartbeatMap.remove(clientId);
        if (info != null) {
            stopHeartbeatForClient(clientId, info);
        }
    }
    
    /**
     * 收到客户端消息时调用，重置心跳计时器
     * @param clientId 客户端 ID（IP 地址）
     */
    public void onMessageReceived(String clientId) {
        ClientHeartbeatInfo info = clientHeartbeatMap.get(clientId);
        if (info != null) {
            info.lastMessageTime = System.currentTimeMillis();
        JLog.d(TAG, "Message received from " + clientId + ", reset heartbeat timer");
        }
    }
    
    /**
     * 启动心跳监控
     */
    private void startHeartbeatMonitoring(String clientId, ClientHeartbeatInfo info) {
        if (info.isRunning) {
        JLog.w(TAG, "Heartbeat already running for client: " + clientId);
            return;
        }
        
        info.isRunning = true;
        final long startTime = System.currentTimeMillis();
        
        // 定期检查和发送心跳
        info.scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastMessage = currentTime - info.lastMessageTime;
                long elapsedSinceStart = currentTime - startTime;
                
                // 检查是否超过最大保活时间
                if (elapsedSinceStart >= MAX_KEEPALIVE_TIME) {
                    JLog.i(TAG, "Max keepalive time reached for " + clientId + ", stopping heartbeat");
                    stopHeartbeatForClient(clientId, info);
                    return;
                }
                
                // 如果超过心跳间隔未收到消息，发送心跳
                if (timeSinceLastMessage >= HEARTBEAT_INTERVAL) {
                    JLog.d(TAG, "Sending heartbeat to " + clientId);
                    sendHeartbeatInternal(clientId);
                    
                    // 更新最后消息时间，避免重复发送
                    info.lastMessageTime = System.currentTimeMillis();
                }
            } catch (Exception e) {
                    JLog.e(TAG, "Error in heartbeat task for " + clientId, e);
            }
        }, HEARTBEAT_INTERVAL / 2, HEARTBEAT_INTERVAL / 2, TimeUnit.MILLISECONDS);
        
        JLog.i(TAG, "Started heartbeat monitoring for " + clientId);
    }
    
    /**
     * 停止客户端的心跳
     */
    private void stopHeartbeatForClient(String clientId, ClientHeartbeatInfo info) {
        if (info.scheduledFuture != null) {
            info.scheduledFuture.cancel(false);
            info.scheduledFuture = null;
        }
        info.isRunning = false;
        JLog.i(TAG, "Stopped heartbeat for " + clientId);
    }
    
    /**
     * 内部方法：发送心跳
     */
    private void sendHeartbeatInternal(String clientId) {
        if (heartbeatSender == null) {
            JLog.w(TAG, "HeartbeatSender not set, cannot send heartbeat");
            return;
        }
        
        executorService.execute(() -> {
            try {
                heartbeatSender.sendHeartbeat(clientId, HEARTBEAT_MESSAGE);
            JLog.d(TAG, "Heartbeat sent to " + clientId);
            } catch (Exception e) {
            JLog.e(TAG, "Failed to send heartbeat to " + clientId, e);
            }
        });
    }
    
    /**
     * 手动触发一次心跳（可选功能）
     * @param clientId 客户端 ID
     */
    public void triggerHeartbeat(String clientId) {
        ClientHeartbeatInfo info = clientHeartbeatMap.get(clientId);
        if (info != null) {
        JLog.d(TAG, "Manually triggering heartbeat for " + clientId);
            sendHeartbeatInternal(clientId);
            info.lastMessageTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 释放资源
     */
    public void destroy() {
        JLog.i(TAG, "Destroying HeartbeatManager");
        
        // 停止所有心跳任务
        for (Map.Entry<String, ClientHeartbeatInfo> entry : clientHeartbeatMap.entrySet()) {
            stopHeartbeatForClient(entry.getKey(), entry.getValue());
        }
        clientHeartbeatMap.clear();
        
        // 关闭线程池
        scheduledExecutor.shutdownNow();
        executorService.shutdownNow();
        
        instance = null;
    }
}
