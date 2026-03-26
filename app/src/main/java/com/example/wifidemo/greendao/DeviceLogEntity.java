package com.example.wifidemo.greendao;

public class DeviceLogEntity {
    private Long id;
    private String deviceId;
    private String category;
    private String action;
    private String message;
    private String remoteIp;
    private Integer remotePort;
    private String localIp;
    private Integer localPort;
    private long timestamp;

    public DeviceLogEntity() {
    }

    public DeviceLogEntity(
            Long id,
            String deviceId,
            String category,
            String action,
            String message,
            String remoteIp,
            Integer remotePort,
            String localIp,
            Integer localPort,
            long timestamp
    ) {
        this.id = id;
        this.deviceId = deviceId;
        this.category = category;
        this.action = action;
        this.message = message;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.localIp = localIp;
        this.localPort = localPort;
        this.timestamp = timestamp;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
