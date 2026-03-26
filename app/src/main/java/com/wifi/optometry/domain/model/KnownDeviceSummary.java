package com.wifi.optometry.domain.model;

public class KnownDeviceSummary {
    private String deviceId;
    private String displayLabel;
    private String lastKnownIp;
    private long lastSeenAt;
    private boolean connected;
    private int communicationCount;
    private int connectionCount;

    public KnownDeviceSummary(
            String deviceId,
            String displayLabel,
            String lastKnownIp,
            long lastSeenAt,
            boolean connected,
            int communicationCount,
            int connectionCount
    ) {
        this.deviceId = deviceId;
        this.displayLabel = displayLabel;
        this.lastKnownIp = lastKnownIp;
        this.lastSeenAt = lastSeenAt;
        this.connected = connected;
        this.communicationCount = communicationCount;
        this.connectionCount = connectionCount;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getLastKnownIp() {
        return lastKnownIp;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isConnected() {
        return connected;
    }

    public int getCommunicationCount() {
        return communicationCount;
    }

    public int getConnectionCount() {
        return connectionCount;
    }
}
