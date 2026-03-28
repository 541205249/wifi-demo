package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;

public interface TransferProgressListener {
    void onProgress(@NonNull TransferProgress progress);
}
