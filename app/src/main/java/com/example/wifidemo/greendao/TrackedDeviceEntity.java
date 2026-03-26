package com.example.wifidemo.greendao;

public class TrackedDeviceEntity {
    private Long id;
    private String deviceId;
    private String macAddress;
    private String lastKnownIp;
    private Integer lastKnownPort;
    private String lastLocalIp;
    private Integer lastLocalPort;
    private long lastSeenAt;
    private boolean currentlyConnected;
    private long communicationCount;
    private long connectionCount;

    public TrackedDeviceEntity() {
    }

    public TrackedDeviceEntity(
            Long id,
            String deviceId,
            String macAddress,
            String lastKnownIp,
            Integer lastKnownPort,
            String lastLocalIp,
            Integer lastLocalPort,
            long lastSeenAt,
            boolean currentlyConnected,
            long communicationCount,
            long connectionCount
    ) {
        this.id = id;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getLastKnownIp() {
        return lastKnownIp;
    }

    public void setLastKnownIp(String lastKnownIp) {
        this.lastKnownIp = lastKnownIp;
    }

    public Integer getLastKnownPort() {
        return lastKnownPort;
    }

    public void setLastKnownPort(Integer lastKnownPort) {
        this.lastKnownPort = lastKnownPort;
    }

    public String getLastLocalIp() {
        return lastLocalIp;
    }

    public void setLastLocalIp(String lastLocalIp) {
        this.lastLocalIp = lastLocalIp;
    }

    public Integer getLastLocalPort() {
        return lastLocalPort;
    }

    public void setLastLocalPort(Integer lastLocalPort) {
        this.lastLocalPort = lastLocalPort;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean getCurrentlyConnected() {
        return currentlyConnected;
    }

    public void setCurrentlyConnected(boolean currentlyConnected) {
        this.currentlyConnected = currentlyConnected;
    }

    public long getCommunicationCount() {
        return communicationCount;
    }

    public void setCommunicationCount(long communicationCount) {
        this.communicationCount = communicationCount;
    }

    public long getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(long connectionCount) {
        this.connectionCount = connectionCount;
    }
}
