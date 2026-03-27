package com.wifi.lib.command;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Objects;

/**
 * 六位指令编码，结构固定为：大模块(2) + 子模块(2) + 动作(2)。
 */
public final class CommandCode implements Comparable<CommandCode> {
    public static final int CODE_LENGTH = 6;
    public static final int SEGMENT_LENGTH = 2;

    @NonNull
    private final String value;

    private CommandCode(@NonNull String value) {
        this.value = value;
    }

    @NonNull
    public static CommandCode of(@NonNull String rawValue) {
        String value = rawValue.trim();
        if (!isValid(value)) {
            throw new IllegalArgumentException("指令编码必须是 6 位数字，当前值: " + rawValue);
        }
        return new CommandCode(value);
    }

    public static boolean isValid(String rawValue) {
        if (rawValue == null) {
            return false;
        }
        String value = rawValue.trim();
        if (value.length() != CODE_LENGTH) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @NonNull
    public String getModuleCode() {
        return value.substring(0, SEGMENT_LENGTH);
    }

    @NonNull
    public String getSubModuleCode() {
        return value.substring(SEGMENT_LENGTH, SEGMENT_LENGTH * 2);
    }

    @NonNull
    public String getActionCode() {
        return value.substring(SEGMENT_LENGTH * 2);
    }

    @NonNull
    public String toSegmentDisplay() {
        return String.format(
                Locale.getDefault(),
                "%s-%s-%s",
                getModuleCode(),
                getSubModuleCode(),
                getActionCode()
        );
    }

    @Override
    public int compareTo(@NonNull CommandCode other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CommandCode)) {
            return false;
        }
        CommandCode other = (CommandCode) object;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return value;
    }
}
