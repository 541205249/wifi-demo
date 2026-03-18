package com.wifi.lib;

public interface MessageInform {
    void serviceCallback(boolean result, String data);

    void sendModuleCallback(boolean result, int pattern);
}
