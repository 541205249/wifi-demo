package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;

public interface StreamTransport {
    void send(@NonNull String frame);
}
