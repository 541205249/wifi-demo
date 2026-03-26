package com.example.wifidemo.clinic.model;

public class ConnectedDeviceInfo {
    private String clientId;
    private String displayLabel;
    private String macAddress;
    private String ipAddress;
    private int port;
    private long connectedAt;

    public ConnectedDeviceInfo(
            String clientId,
            String displayLabel,
            String macAddress,
            String ipAddress,
            int port,
            long connectedAt
    ) {
        this.clientId = clientId;
        this.displayLabel = displayLabel;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.port = port;
        this.connectedAt = connectedAt;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public long getConnectedAt() {
        return connectedAt;
    }
}
