package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

public interface StreamUseCase {
    void onStream(@NonNull StreamDispatchContext context);
}
