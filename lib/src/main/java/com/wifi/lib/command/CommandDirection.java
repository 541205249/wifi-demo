package com.wifi.lib.command;

import androidx.annotation.NonNull;

public enum CommandDirection {
    OUTBOUND("发送", 's'),
    INBOUND("接收", 'r');

    @NonNull
    private final String label;
    private final char codePrefix;

    CommandDirection(@NonNull String label, char codePrefix) {
        this.label = label;
        this.codePrefix = codePrefix;
    }

    public boolean supportsOutbound() {
        return this == OUTBOUND;
    }

    public boolean supportsInbound() {
        return this == INBOUND;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public char getCodePrefix() {
        return codePrefix;
    }

    @NonNull
    public static CommandDirection fromCodePrefix(char rawPrefix) {
        char normalizedPrefix = Character.toLowerCase(rawPrefix);
        if (normalizedPrefix == 's') {
            return OUTBOUND;
        }
        if (normalizedPrefix == 'r') {
            return INBOUND;
        }
        throw new IllegalArgumentException("无法识别的编码方向前缀: " + rawPrefix);
    }
}
