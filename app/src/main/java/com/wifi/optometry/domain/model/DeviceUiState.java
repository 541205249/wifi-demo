package com.wifi.optometry.domain.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class DeviceUiState {
    private boolean serverRunning;
    private String localIp;
    private int serverPort;
    private String selectedClientId;
    private String pendingMessage;
    private String boundDeviceClientId;
    private String boundDeviceMacAddress;
    private String boundDeviceLabel;
    private boolean boundDeviceOnline;
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

    public String getBoundDeviceClientId() {
        return boundDeviceClientId;
    }

    public void setBoundDeviceClientId(String boundDeviceClientId) {
        this.boundDeviceClientId = boundDeviceClientId;
    }

    public String getBoundDeviceMacAddress() {
        return boundDeviceMacAddress;
    }

    public void setBoundDeviceMacAddress(String boundDeviceMacAddress) {
        this.boundDeviceMacAddress = boundDeviceMacAddress;
    }

    public String getBoundDeviceLabel() {
        return boundDeviceLabel;
    }

    public void setBoundDeviceLabel(String boundDeviceLabel) {
        this.boundDeviceLabel = boundDeviceLabel;
    }

    public boolean isBoundDeviceOnline() {
        return boundDeviceOnline;
    }

    public void setBoundDeviceOnline(boolean boundDeviceOnline) {
        this.boundDeviceOnline = boundDeviceOnline;
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

    public void replaceConnectedDevices(List<ConnectedDeviceInfo> devices) {
        connectedDevices.clear();
        if (devices != null) {
            connectedDevices.addAll(devices);
        }
        refreshBoundDevice(devices);
    }

    public void replaceKnownDevices(List<KnownDeviceSummary> devices) {
        knownDevices.clear();
        if (devices != null) {
            knownDevices.addAll(devices);
        }
    }

    public void appendLog(String line, int maxLogCount) {
        if (line == null) {
            return;
        }
        logs.add(0, line);
        while (maxLogCount >= 0 && logs.size() > maxLogCount) {
            logs.remove(logs.size() - 1);
        }
    }

    public void bindMainDevice(ConnectedDeviceInfo device) {
        if (device == null) {
            return;
        }
        boundDeviceClientId = device.getClientId();
        if (!TextUtils.isEmpty(device.getMacAddress())) {
            boundDeviceMacAddress = device.getMacAddress();
        }
        boundDeviceLabel = device.getDisplayLabel();
        boundDeviceOnline = true;
    }

    public void clearBoundMainDevice() {
        boundDeviceClientId = null;
        boundDeviceMacAddress = null;
        boundDeviceLabel = null;
        boundDeviceOnline = false;
    }

    public String getBoundCommandClientId() {
        return boundDeviceOnline ? boundDeviceClientId : null;
    }

    public void refreshBoundDevice(List<ConnectedDeviceInfo> devices) {
        boundDeviceOnline = false;
        if (TextUtils.isEmpty(boundDeviceClientId) && TextUtils.isEmpty(boundDeviceMacAddress)) {
            return;
        }
        if (devices == null || devices.isEmpty()) {
            return;
        }
        for (ConnectedDeviceInfo device : devices) {
            if (!matchesBoundDevice(device)) {
                continue;
            }
            boundDeviceClientId = device.getClientId();
            if (!TextUtils.isEmpty(device.getMacAddress())) {
                boundDeviceMacAddress = device.getMacAddress();
            }
            boundDeviceLabel = device.getDisplayLabel();
            boundDeviceOnline = true;
            return;
        }
    }

    private boolean matchesBoundDevice(ConnectedDeviceInfo device) {
        if (device == null) {
            return false;
        }
        if (!TextUtils.isEmpty(boundDeviceMacAddress)
                && TextUtils.equals(boundDeviceMacAddress, device.getMacAddress())) {
            return true;
        }
        return TextUtils.equals(boundDeviceClientId, device.getClientId());
    }
}
