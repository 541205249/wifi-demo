package com.wifi.lib.command.ack;

import androidx.annotation.NonNull;

public enum AckChannel {
    COMMAND("CMD"),
    TRANSFER("TRANSFER"),
    STREAM("STREAM"),
    GENERIC("GENERIC");

    @NonNull
    private final String wireValue;

    AckChannel(@NonNull String wireValue) {
        this.wireValue = wireValue;
    }

    @NonNull
    public String getWireValue() {
        return wireValue;
    }

    @NonNull
    public static AckChannel fromWireValue(@NonNull String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        for (AckChannel channel : values()) {
            if (channel.wireValue.equalsIgnoreCase(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("无法识别的 ACK 类型: " + rawValue);
    }
}
