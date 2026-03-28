package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

public interface TransferUseCase {
    void onTransfer(@NonNull TransferDispatchContext context);
}
