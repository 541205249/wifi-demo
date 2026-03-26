package com.wifi.optometry.ui.state;

import com.wifi.optometry.domain.model.ConnectedDeviceInfo;

import java.util.List;

public interface DeviceServiceGateway {
    boolean isServerRunning();

    String getLocalIpAddress();

    int getServerPort();

    List<ConnectedDeviceInfo> getConnectedDevices();

    void startServer();

    void stopServer();

    void broadcastMessage(String message);

    void sendMessageToClient(String clientId, String message);
}
