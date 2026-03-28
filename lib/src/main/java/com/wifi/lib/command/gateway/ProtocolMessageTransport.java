package com.wifi.lib.command.gateway;

import androidx.annotation.NonNull;

public interface ProtocolMessageTransport {
    void send(@NonNull String rawMessage);
}
