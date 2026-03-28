package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;

public enum TransferDirection {
    APP_TO_DEVICE("App->模块"),
    DEVICE_TO_APP("模块->App");

    @NonNull
    private final String label;

    TransferDirection(@NonNull String label) {
        this.label = label;
    }

    @NonNull
    public String getLabel() {
        return label;
    }
}
