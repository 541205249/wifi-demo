package com.wifi.lib.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public enum CommandMatchMode {
    EXACT("精确"),
    PREFIX("前缀"),
    CONTAINS("包含"),
    REGEX("正则");

    @NonNull
    private final String label;

    CommandMatchMode(@NonNull String label) {
        this.label = label;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public static CommandMatchMode fromValue(@Nullable String rawValue) {
        if (rawValue == null) {
            return EXACT;
        }

        String normalized = normalize(rawValue);
        if (normalized.isEmpty()) {
            return EXACT;
        }

        if ("精确".equals(normalized) || "exact".equals(normalized) || "equals".equals(normalized)) {
            return EXACT;
        }
        if ("前缀".equals(normalized) || "prefix".equals(normalized) || "startswith".equals(normalized)) {
            return PREFIX;
        }
        if ("包含".equals(normalized) || "contains".equals(normalized) || "contain".equals(normalized)) {
            return CONTAINS;
        }
        if ("正则".equals(normalized) || "regex".equals(normalized) || "regexp".equals(normalized)) {
            return REGEX;
        }

        throw new IllegalArgumentException("无法识别的接收匹配方式: " + rawValue);
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
