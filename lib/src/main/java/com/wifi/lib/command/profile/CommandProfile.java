package com.wifi.lib.command.profile;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.wifi.lib.command.CommandCatalog;

public interface CommandProfile {
    @NonNull
    String getProfileId();

    @NonNull
    CommandCatalog getCatalog();

    @RawRes
    int getBuiltInTableResId();

    @NonNull
    String getBuiltInSourceLabel();
}
