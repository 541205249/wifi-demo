package com.wifi.lib.command;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * App 侧提前预留的指令位，只关心编码和业务语义，不关心真实命令内容。
 */
public final class CommandReservation {
    @NonNull
    private final CommandCode code;
    @NonNull
    private final String moduleName;
    @NonNull
    private final String subModuleName;
    @NonNull
    private final String actionName;
    @NonNull
    private final String codeExplanation;
    @NonNull
    private final CommandDirection direction;
    @NonNull
    private final String description;

    public CommandReservation(
            @NonNull String code,
            @NonNull String moduleName,
            @NonNull String subModuleName,
            @NonNull String actionName,
            @NonNull String codeExplanation,
            @NonNull CommandDirection direction,
            @NonNull String description
    ) {
        this.code = CommandCode.of(code);
        this.moduleName = normalize(moduleName);
        this.subModuleName = normalize(subModuleName);
        this.actionName = normalize(actionName);
        this.codeExplanation = normalize(codeExplanation);
        this.direction = direction == null ? CommandDirection.BIDIRECTIONAL : direction;
        this.description = normalize(description);
    }

    @NonNull
    public CommandCode getCode() {
        return code;
    }

    @NonNull
    public String getCodeValue() {
        return code.getValue();
    }

    @NonNull
    public String getModuleName() {
        return moduleName;
    }

    @NonNull
    public String getSubModuleName() {
        return subModuleName;
    }

    @NonNull
    public String getActionName() {
        return actionName;
    }

    @NonNull
    public String getCodeExplanation() {
        return codeExplanation;
    }

    @NonNull
    public CommandDirection getDirection() {
        return direction;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getDisplayName() {
        return moduleName + "/" + subModuleName + "/" + actionName;
    }

    @NonNull
    private static String normalize(@NonNull String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CommandReservation)) {
            return false;
        }
        CommandReservation other = (CommandReservation) object;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
