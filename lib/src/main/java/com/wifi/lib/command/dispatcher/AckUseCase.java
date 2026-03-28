package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

public interface AckUseCase {
    void onAck(@NonNull AckDispatchContext context);
}
