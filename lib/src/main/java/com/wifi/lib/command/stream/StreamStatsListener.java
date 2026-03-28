package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;

public interface StreamStatsListener {
    void onStats(@NonNull StreamStats stats);
}
