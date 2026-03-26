package com.example.wifidemo.clinic.model;

import java.util.ArrayList;
import java.util.List;

public class DeviceUiState {
    private boolean serverRunning;
    private String localIp;
    private int serverPort;
    private String selectedClientId;
    private String pendingMessage;
    private final List<ConnectedDeviceInfo> connectedDevices = new ArrayList<>();
    private final List<KnownDeviceSummary> knownDevices = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();

    public boolean isServerRunning() {
        return serverRunning;
    }

    public void setServerRunning(boolean serverRunning) {
        this.serverRunning = serverRunning;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getSelectedClientId() {
        return selectedClientId;
    }

    public void setSelectedClientId(String selectedClientId) {
        this.selectedClientId = selectedClientId;
    }

    public String getPendingMessage() {
        return pendingMessage;
    }

    public void setPendingMessage(String pendingMessage) {
        this.pendingMessage = pendingMessage;
    }

    public List<ConnectedDeviceInfo> getConnectedDevices() {
        return connectedDevices;
    }

    public List<KnownDeviceSummary> getKnownDevices() {
        return knownDevices;
    }

    public List<String> getLogs() {
        return logs;
    }
}
