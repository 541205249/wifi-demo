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

        if (matchesAlias(normalized, "精确", "exact", "equals")) {
            return EXACT;
        }
        if (matchesAlias(normalized, "前缀", "prefix", "startswith")) {
            return PREFIX;
        }
        if (matchesAlias(normalized, "包含", "contains", "contain")) {
            return CONTAINS;
        }
        if (matchesAlias(normalized, "正则", "regex", "regexp")) {
            return REGEX;
        }

        throw new IllegalArgumentException("无法识别的接收匹配方式: " + rawValue);
    }

    private static boolean matchesAlias(@NonNull String normalized, @NonNull String... aliases) {
        for (String alias : aliases) {
            if (alias.equals(normalized)) {
                return true;
            }
        }
        return false;
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
