package com.wifi.lib.log;

import androidx.annotation.NonNull;

public interface JLogObserver {
    void onLog(@NonNull JLogEntry entry);
}
