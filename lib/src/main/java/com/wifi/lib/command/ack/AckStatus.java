package com.wifi.lib.command.ack;

import androidx.annotation.NonNull;

public enum AckStatus {
    SUCCESS("ACK+"),
    FAILURE("ERR+");

    @NonNull
    private final String prefix;

    AckStatus(@NonNull String prefix) {
        this.prefix = prefix;
    }

    @NonNull
    public String getPrefix() {
        return prefix;
    }

    @NonNull
    public static AckStatus fromRawMessage(@NonNull String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        for (AckStatus status : values()) {
            if (message.startsWith(status.prefix)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无法识别的 ACK 前缀: " + rawMessage);
    }
}
