package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

public interface ProtocolUseCase {
    void onDispatch(@NonNull ProtocolDispatchContext context);
}
