package com.wifi.lib.command;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Objects;

/**
 * 指令编码格式固定为：方向前缀(1) + 大模块(2) + 子模块(2) + 动作(2)。
 * 例如：s100101 / r120202
 */
public final class CommandCode implements Comparable<CommandCode> {
    public static final int BUSINESS_CODE_LENGTH = 6;
    public static final int CODE_LENGTH = 7;
    public static final int SEGMENT_LENGTH = 2;

    @NonNull
    private final String value;
    @NonNull
    private final CommandDirection direction;

    private CommandCode(@NonNull String value, @NonNull CommandDirection direction) {
        this.value = value;
        this.direction = direction;
    }

    @NonNull
    public static CommandCode of(@NonNull String rawValue) {
        String value = normalize(rawValue);
        if (!isValid(value)) {
            throw new IllegalArgumentException("指令编码必须是 s/r + 6 位数字，当前值: " + rawValue);
        }
        return new CommandCode(value, CommandDirection.fromCodePrefix(value.charAt(0)));
    }

    public static boolean isValid(String rawValue) {
        if (rawValue == null) {
            return false;
        }
        String value = normalize(rawValue);
        if (value.length() != CODE_LENGTH) {
            return false;
        }
        try {
            CommandDirection.fromCodePrefix(value.charAt(0));
        } catch (IllegalArgumentException exception) {
            return false;
        }
        for (int index = 1; index < value.length(); index++) {
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
    public CommandDirection getDirection() {
        return direction;
    }

    public char getDirectionPrefix() {
        return value.charAt(0);
    }

    @NonNull
    public String getBusinessCode() {
        return value.substring(1);
    }

    @NonNull
    public String getModuleCode() {
        return getBusinessCode().substring(0, SEGMENT_LENGTH);
    }

    @NonNull
    public String getSubModuleCode() {
        return getBusinessCode().substring(SEGMENT_LENGTH, SEGMENT_LENGTH * 2);
    }

    @NonNull
    public String getActionCode() {
        return getBusinessCode().substring(SEGMENT_LENGTH * 2);
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
        int businessCompare = getBusinessCode().compareTo(other.getBusinessCode());
        if (businessCompare != 0) {
            return businessCompare;
        }
        return Character.compare(getDirectionPrefix(), other.getDirectionPrefix());
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

    @NonNull
    private static String normalize(@NonNull String rawValue) {
        return rawValue.trim().toLowerCase(Locale.ROOT);
    }
}
