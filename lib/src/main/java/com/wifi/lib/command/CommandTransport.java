package com.wifi.lib.command;

import androidx.annotation.NonNull;

public interface CommandTransport {
    void send(@NonNull OutboundCommand command);
}
