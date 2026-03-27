package com.example.wifidemo.device;

import android.content.Context;
import android.text.TextUtils;

import com.wifi.lib.db.DaoSession;
import com.wifi.lib.db.DeviceLogEntity;
import com.wifi.lib.db.DeviceLogEntityDao;
import com.wifi.lib.db.TrackedDeviceEntity;
import com.wifi.lib.db.TrackedDeviceEntityDao;
import com.wifi.lib.db.WifiDeviceDbInitiator;
import com.wifi.lib.db.WifiDeviceDbSessionProvider;
import com.wifi.lib.log.DLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 按设备归档历史连接与通信记录。
 * 当前底层持久化使用 greenDAO。
 */
public class DeviceHistoryStore {
    private static final String TAG = "DeviceHistoryStore";
    public static final String CATEGORY_COMMUNICATION = "communication";
    public static final String CATEGORY_CONNECTION = "connection";
    public static final String ACTION_CONNECTED = "connected";
    public static final String ACTION_DISCONNECTED = "disconnected";
    public static final String ACTION_SENT = "sent";
    public static final String ACTION_RECEIVED = "received";
    private static final int MAX_LOGS_PER_DEVICE = 600;

    private static volatile DeviceHistoryStore instance;

    private final TrackedDeviceEntityDao trackedDeviceDao;
    private final DeviceLogEntityDao deviceLogDao;

    public enum LogFilter {
        ALL,
        COMMUNICATION,
        CONNECTION
    }

    public static final class DeviceSummary {
        private final String deviceId;
        private final String macAddress;
        private final String lastKnownIp;
        private final int lastKnownPort;
        private final String lastLocalIp;
        private final int lastLocalPort;
        private final long lastSeenAt;
        private final boolean currentlyConnected;
        private final int communicationCount;
        private final int connectionCount;

        DeviceSummary(
                String deviceId,
                String macAddress,
                String lastKnownIp,
                int lastKnownPort,
                String lastLocalIp,
                int lastLocalPort,
                long lastSeenAt,
                boolean currentlyConnected,
                int communicationCount,
                int connectionCount
        ) {
            this.deviceId = deviceId;
            this.macAddress = macAddress;
            this.lastKnownIp = lastKnownIp;
            this.lastKnownPort = lastKnownPort;
            this.lastLocalIp = lastLocalIp;
            this.lastLocalPort = lastLocalPort;
            this.lastSeenAt = lastSeenAt;
            this.currentlyConnected = currentlyConnected;
            this.communicationCount = communicationCount;
            this.connectionCount = connectionCount;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public String getLastKnownIp() {
            return lastKnownIp;
        }

        public int getLastKnownPort() {
            return lastKnownPort;
        }

        public String getLastLocalIp() {
            return lastLocalIp;
        }

        public int getLastLocalPort() {
            return lastLocalPort;
        }

        public long getLastSeenAt() {
            return lastSeenAt;
        }

        public boolean isCurrentlyConnected() {
            return currentlyConnected;
        }

        public int getCommunicationCount() {
            return communicationCount;
        }

        public int getConnectionCount() {
            return connectionCount;
        }

        public String getPrimaryLabel() {
            return !TextUtils.isEmpty(macAddress) ? macAddress : buildUnknownMacLabel(lastKnownIp);
        }

        public String getInlineLabel() {
            StringBuilder builder = new StringBuilder(getPrimaryLabel());
            if (!TextUtils.isEmpty(lastKnownIp)) {
                builder.append(" (").append(lastKnownIp);
                if (lastKnownPort > 0) {
                    builder.append(":").append(lastKnownPort);
                }
                builder.append(")");
            }
            return builder.toString();
        }

        public String getSelectionLabel() {
            StringBuilder builder = new StringBuilder(getPrimaryLabel());
            builder.append(currentlyConnected ? " | 在线" : " | 离线");
            if (!TextUtils.isEmpty(lastKnownIp)) {
                builder.append(" | 最近 ").append(lastKnownIp);
                if (lastKnownPort > 0) {
                    builder.append(":").append(lastKnownPort);
                }
            }
            return builder.toString();
        }
    }

    public static final class DeviceLogEntry {
        private final long timestamp;
        private final String category;
        private final String action;
        private final String message;
        private final String remoteIp;
        private final int remotePort;
        private final String localIp;
        private final int localPort;

        DeviceLogEntry(
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

        public long getTimestamp() {
            return timestamp;
        }

        public String formatForDisplay() {
            StringBuilder builder = new StringBuilder();
            builder.append("[")
                    .append(formatTimestamp(timestamp))
                    .append("] ");

            if (CATEGORY_CONNECTION.equals(category)) {
                builder.append(ACTION_CONNECTED.equals(action) ? "连接" : "断开");
            } else {
                builder.append(ACTION_RECEIVED.equals(action) ? "收到" : "发送");
            }

            String endpoint = formatEndpoint(remoteIp, remotePort, localIp, localPort);
            if (!TextUtils.isEmpty(endpoint)) {
                builder.append(" | ").append(endpoint);
            }

            if (!TextUtils.isEmpty(message)) {
                builder.append("\n").append(message);
            }
            return builder.toString();
        }
    }

    private DeviceHistoryStore(Context context) {
        WifiDeviceDbInitiator dbInitiator = new WifiDeviceDbInitiator();
        dbInitiator.setup(context.getApplicationContext());
        DaoSession daoSession = WifiDeviceDbSessionProvider.getInstance().getDaoSession();
        if (daoSession == null) {
            throw new IllegalStateException("DaoSession is not initialized");
        }
        trackedDeviceDao = daoSession.getTrackedDeviceEntityDao();
        deviceLogDao = daoSession.getDeviceLogEntityDao();
        trace("历史仓库初始化完成，DAO 已就绪");
    }

    public static DeviceHistoryStore getInstance(Context context) {
        if (instance == null) {
            synchronized (DeviceHistoryStore.class) {
                if (instance == null) {
                    instance = new DeviceHistoryStore(context);
                }
            }
        }
        return instance;
    }

    public static String normalizeMacAddress(String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return null;
        }

        String compact = macAddress.trim()
                .replace(":", "")
                .replace("-", "")
                .replaceAll("\\s+", "");
        if (compact.matches("[0-9A-Fa-f]{12}") && !"000000000000".equalsIgnoreCase(compact)) {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < compact.length(); index += 2) {
                if (builder.length() > 0) {
                    builder.append(':');
                }
                builder.append(compact, index, index + 2);
            }
            return builder.toString().toUpperCase(Locale.US);
        }

        String normalized = macAddress.trim()
                .replace('-', ':')
                .toUpperCase(Locale.US);
        if (normalized.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}")
                && !"00:00:00:00:00:00".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    public static String createDeviceId(String macAddress, String remoteIp) {
        String normalizedMac = normalizeMacAddress(macAddress);
        if (!TextUtils.isEmpty(normalizedMac)) {
            return normalizedMac;
        }
        return "UNRESOLVED@" + (TextUtils.isEmpty(remoteIp) ? "UNKNOWN" : remoteIp);
    }

    public synchronized void markAllDevicesOffline() {
        List<TrackedDeviceEntity> devices = trackedDeviceDao.loadAll();
        int updatedCount = 0;
        for (TrackedDeviceEntity device : devices) {
            if (device.getCurrentlyConnected()) {
                device.setCurrentlyConnected(false);
                trackedDeviceDao.update(device);
                updatedCount++;
            }
        }
        trace("批量重置模块离线状态完成，updated=" + updatedCount);
    }

    public synchronized void recordConnection(
            String deviceId,
            String macAddress,
            String remoteIp,
            int remotePort,
            String localIp,
            int localPort,
            boolean connected
    ) {
        recordConnectionAt(
                deviceId,
                macAddress,
                remoteIp,
                remotePort,
                localIp,
                localPort,
                connected,
                System.currentTimeMillis()
        );
    }

    public synchronized void recordConnectionAt(
            String deviceId,
            String macAddress,
            String remoteIp,
            int remotePort,
            String localIp,
            int localPort,
            boolean connected,
            long timestamp
    ) {
        TrackedDeviceEntity device = getOrCreateDevice(deviceId, macAddress);
        updateDeviceSummary(device, macAddress, remoteIp, remotePort, localIp, localPort, timestamp);
        device.setCurrentlyConnected(connected);
        device.setConnectionCount(device.getConnectionCount() + 1);
        saveDevice(device);

        DeviceLogEntity log = new DeviceLogEntity(
                null,
                deviceId,
                CATEGORY_CONNECTION,
                connected ? ACTION_CONNECTED : ACTION_DISCONNECTED,
                null,
                remoteIp,
                remotePort > 0 ? remotePort : null,
                localIp,
                localPort > 0 ? localPort : null,
                timestamp
        );
        deviceLogDao.insert(log);
        trimDeviceLogs(deviceId);
        trace("连接记录已入库，deviceId=" + deviceId
                + ", connected=" + connected
                + ", remote=" + remoteIp + ":" + remotePort);
    }

    public synchronized void recordCommunication(
            String deviceId,
            String macAddress,
            String remoteIp,
            int remotePort,
            String localIp,
            int localPort,
            String action,
            String message
    ) {
        recordCommunicationAt(
                deviceId,
                macAddress,
                remoteIp,
                remotePort,
                localIp,
                localPort,
                action,
                message,
                System.currentTimeMillis()
        );
    }

    public synchronized void recordCommunicationAt(
            String deviceId,
            String macAddress,
            String remoteIp,
            int remotePort,
            String localIp,
            int localPort,
            String action,
            String message,
            long timestamp
    ) {
        TrackedDeviceEntity device = getOrCreateDevice(deviceId, macAddress);
        updateDeviceSummary(device, macAddress, remoteIp, remotePort, localIp, localPort, timestamp);
        device.setCommunicationCount(device.getCommunicationCount() + 1);
        saveDevice(device);

        DeviceLogEntity log = new DeviceLogEntity(
                null,
                deviceId,
                CATEGORY_COMMUNICATION,
                action,
                sanitizeMessage(message),
                remoteIp,
                remotePort > 0 ? remotePort : null,
                localIp,
                localPort > 0 ? localPort : null,
                timestamp
        );
        deviceLogDao.insert(log);
        trimDeviceLogs(deviceId);
        trace("通信记录已入库，deviceId=" + deviceId
                + ", action=" + action
                + ", length=" + (message == null ? 0 : message.length()));
    }

    public synchronized List<DeviceSummary> getKnownDevices() {
        List<DeviceSummary> result = new ArrayList<>();
        List<TrackedDeviceEntity> devices = trackedDeviceDao.queryBuilder()
                .orderDesc(TrackedDeviceEntityDao.Properties.LastSeenAt)
                .list();
        for (TrackedDeviceEntity device : devices) {
            result.add(toSummary(device));
        }
        trace("查询已建档模块列表完成，count=" + result.size());
        return result;
    }

    public synchronized DeviceSummary getDeviceSummary(String deviceId) {
        TrackedDeviceEntity device = findDevice(deviceId);
        return device == null ? null : toSummary(device);
    }

    public synchronized List<DeviceLogEntry> getLogs(String deviceId, LogFilter filter) {
        List<DeviceLogEntry> result = new ArrayList<>();
        List<DeviceLogEntity> logs;
        if (filter == LogFilter.COMMUNICATION) {
            logs = deviceLogDao.queryBuilder()
                    .where(
                            DeviceLogEntityDao.Properties.DeviceId.eq(deviceId),
                            DeviceLogEntityDao.Properties.Category.eq(CATEGORY_COMMUNICATION)
                    )
                    .orderDesc(DeviceLogEntityDao.Properties.Timestamp)
                    .list();
        } else if (filter == LogFilter.CONNECTION) {
            logs = deviceLogDao.queryBuilder()
                    .where(
                            DeviceLogEntityDao.Properties.DeviceId.eq(deviceId),
                            DeviceLogEntityDao.Properties.Category.eq(CATEGORY_CONNECTION)
                    )
                    .orderDesc(DeviceLogEntityDao.Properties.Timestamp)
                    .list();
        } else {
            logs = deviceLogDao.queryBuilder()
                    .where(DeviceLogEntityDao.Properties.DeviceId.eq(deviceId))
                    .orderDesc(DeviceLogEntityDao.Properties.Timestamp)
                    .list();
        }

        for (DeviceLogEntity log : logs) {
            result.add(new DeviceLogEntry(
                    log.getTimestamp(),
                    log.getCategory(),
                    log.getAction(),
                    log.getMessage(),
                    log.getRemoteIp(),
                    safeInt(log.getRemotePort()),
                    log.getLocalIp(),
                    safeInt(log.getLocalPort())
            ));
        }
        trace("查询模块日志完成，deviceId=" + deviceId + ", filter=" + filter + ", count=" + result.size());
        return result;
    }

    private TrackedDeviceEntity getOrCreateDevice(String deviceId, String macAddress) {
        TrackedDeviceEntity device = findDevice(deviceId);
        if (device == null) {
            device = new TrackedDeviceEntity();
            device.setDeviceId(deviceId);
            device.setMacAddress(normalizeMacAddress(macAddress));
            device.setCurrentlyConnected(false);
            device.setCommunicationCount(0);
            device.setConnectionCount(0);
        } else if (!TextUtils.isEmpty(normalizeMacAddress(macAddress))) {
            device.setMacAddress(normalizeMacAddress(macAddress));
        }
        return device;
    }

    private TrackedDeviceEntity findDevice(String deviceId) {
        return trackedDeviceDao.queryBuilder()
                .where(TrackedDeviceEntityDao.Properties.DeviceId.eq(deviceId))
                .unique();
    }

    private void saveDevice(TrackedDeviceEntity device) {
        if (device.getId() == null) {
            trackedDeviceDao.insert(device);
        } else {
            trackedDeviceDao.update(device);
        }
    }

    private void updateDeviceSummary(
            TrackedDeviceEntity device,
            String macAddress,
            String remoteIp,
            int remotePort,
            String localIp,
            int localPort,
            long timestamp
    ) {
        String normalizedMac = normalizeMacAddress(macAddress);
        if (!TextUtils.isEmpty(normalizedMac)) {
            device.setMacAddress(normalizedMac);
        }
        device.setLastKnownIp(remoteIp);
        device.setLastKnownPort(remotePort > 0 ? remotePort : null);
        device.setLastLocalIp(localIp);
        device.setLastLocalPort(localPort > 0 ? localPort : null);
        device.setLastSeenAt(timestamp);
    }

    private void trimDeviceLogs(String deviceId) {
        List<DeviceLogEntity> allLogs = deviceLogDao.queryBuilder()
                .where(DeviceLogEntityDao.Properties.DeviceId.eq(deviceId))
                .orderAsc(DeviceLogEntityDao.Properties.Timestamp)
                .list();
        int overflow = allLogs.size() - MAX_LOGS_PER_DEVICE;
        for (int index = 0; index < overflow; index++) {
            deviceLogDao.delete(allLogs.get(index));
        }
        if (overflow > 0) {
            trace("模块日志已裁剪，deviceId=" + deviceId + ", removed=" + overflow);
        }
    }

    private DeviceSummary toSummary(TrackedDeviceEntity device) {
        return new DeviceSummary(
                device.getDeviceId(),
                device.getMacAddress(),
                device.getLastKnownIp(),
                safeInt(device.getLastKnownPort()),
                device.getLastLocalIp(),
                safeInt(device.getLastLocalPort()),
                device.getLastSeenAt(),
                device.getCurrentlyConnected(),
                safeInt(device.getCommunicationCount()),
                safeInt(device.getConnectionCount())
        );
    }

    private int safeInt(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.replaceAll("[\\r\\n]+$", "");
    }

    public static String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static String formatEndpoint(String remoteIp, int remotePort, String localIp, int localPort) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(remoteIp)) {
            builder.append("模块 ").append(remoteIp);
            if (remotePort > 0) {
                builder.append(":").append(remotePort);
            }
        }
        if (!TextUtils.isEmpty(localIp)) {
            if (builder.length() > 0) {
                builder.append(" -> ");
            }
            builder.append("手机 ").append(localIp);
            if (localPort > 0) {
                builder.append(":").append(localPort);
            }
        }
        return builder.toString();
    }

    private static String buildUnknownMacLabel(String lastKnownIp) {
        if (TextUtils.isEmpty(lastKnownIp)) {
            return "未识别 MAC";
        }
        return "未识别 MAC (" + lastKnownIp + ")";
    }

    private void trace(String message) {
        DLog.i(TAG, message);
    }
}

