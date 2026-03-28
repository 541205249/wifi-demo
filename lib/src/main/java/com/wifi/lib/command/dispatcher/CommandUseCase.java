package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

public interface CommandUseCase {
    void onCommand(@NonNull CommandDispatchContext context);
}
