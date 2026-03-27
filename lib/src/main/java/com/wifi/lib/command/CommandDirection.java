package com.wifi.lib.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public enum CommandDirection {
    OUTBOUND("发送"),
    INBOUND("接收"),
    BIDIRECTIONAL("双向");

    @NonNull
    private final String label;

    CommandDirection(@NonNull String label) {
        this.label = label;
    }

    public boolean supportsOutbound() {
        return this == OUTBOUND || this == BIDIRECTIONAL;
    }

    public boolean supportsInbound() {
        return this == INBOUND || this == BIDIRECTIONAL;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public static CommandDirection fromValue(@Nullable String rawValue) {
        if (rawValue == null) {
            return BIDIRECTIONAL;
        }

        String normalized = normalize(rawValue);
        if (normalized.isEmpty()) {
            return BIDIRECTIONAL;
        }

        if ("发送".equals(normalized)
                || "send".equals(normalized)
                || "outbound".equals(normalized)
                || "app2device".equals(normalized)
                || "apptodevice".equals(normalized)
                || "tx".equals(normalized)) {
            return OUTBOUND;
        }

        if ("接收".equals(normalized)
                || "receive".equals(normalized)
                || "inbound".equals(normalized)
                || "device2app".equals(normalized)
                || "devicetoapp".equals(normalized)
                || "rx".equals(normalized)) {
            return INBOUND;
        }

        if ("双向".equals(normalized)
                || "both".equals(normalized)
                || "bidirectional".equals(normalized)
                || "all".equals(normalized)) {
            return BIDIRECTIONAL;
        }

        throw new IllegalArgumentException("无法识别的指令方向: " + rawValue);
    }

    @NonNull
    private static String normalize(@NonNull String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}
