package com.wifi.lib.command;

import androidx.annotation.NonNull;

public interface InboundCommandHandler {
    void onCommand(@NonNull InboundCommand command);
}
