package com.wifi.lib.command;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class CommandDefinition {
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
    private final String sendCommand;
    @NonNull
    private final String receiveCommand;
    @NonNull
    private final CommandMatchMode receiveMatchMode;
    @NonNull
    private final String description;
    @NonNull
    private final String example;
    private final boolean enabled;
    @NonNull
    private final String remark;
    private final int order;
    @Nullable
    private final Pattern compiledReceivePattern;

    public CommandDefinition(
            @NonNull String code,
            @NonNull String moduleName,
            @NonNull String subModuleName,
            @NonNull String actionName,
            @Nullable String codeExplanation,
            @NonNull CommandDirection direction,
            @Nullable String sendCommand,
            @Nullable String receiveCommand,
            @NonNull CommandMatchMode receiveMatchMode,
            @Nullable String description,
            @Nullable String example,
            boolean enabled,
            @Nullable String remark,
            int order
    ) {
        this.code = CommandCode.of(code);
        this.moduleName = normalize(moduleName);
        this.subModuleName = normalize(subModuleName);
        this.actionName = normalize(actionName);
        this.codeExplanation = normalize(codeExplanation);
        this.direction = direction == null ? CommandDirection.BIDIRECTIONAL : direction;
        this.sendCommand = normalize(sendCommand);
        this.receiveCommand = normalize(receiveCommand);
        this.receiveMatchMode = receiveMatchMode == null ? CommandMatchMode.EXACT : receiveMatchMode;
        this.description = normalize(description);
        this.example = normalize(example);
        this.enabled = enabled;
        this.remark = normalize(remark);
        this.order = order;
        this.compiledReceivePattern = buildReceivePattern(this.receiveCommand, this.receiveMatchMode);
    }

    @Nullable
    private static Pattern buildReceivePattern(@NonNull String receiveCommand, @NonNull CommandMatchMode matchMode) {
        if (TextUtils.isEmpty(receiveCommand) || matchMode != CommandMatchMode.REGEX) {
            return null;
        }
        try {
            return Pattern.compile(receiveCommand);
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("接收命令正则无效: " + receiveCommand, exception);
        }
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
    public String getSendCommand() {
        return sendCommand;
    }

    @NonNull
    public String getReceiveCommand() {
        return receiveCommand;
    }

    @NonNull
    public CommandMatchMode getReceiveMatchMode() {
        return receiveMatchMode;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getExample() {
        return example;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @NonNull
    public String getRemark() {
        return remark;
    }

    public int getOrder() {
        return order;
    }

    public boolean isOutboundConfigured() {
        return direction.supportsOutbound() && !TextUtils.isEmpty(sendCommand);
    }

    public boolean isInboundConfigured() {
        return direction.supportsInbound() && !TextUtils.isEmpty(receiveCommand);
    }

    public boolean isConfigured() {
        if (direction == CommandDirection.OUTBOUND) {
            return isOutboundConfigured();
        }
        if (direction == CommandDirection.INBOUND) {
            return isInboundConfigured();
        }
        return isOutboundConfigured() || isInboundConfigured();
    }

    public boolean canSend() {
        return enabled && isOutboundConfigured();
    }

    public boolean canReceive() {
        return enabled && isInboundConfigured();
    }

    @NonNull
    public String getDisplayName() {
        return moduleName + "/" + subModuleName + "/" + actionName;
    }

    @Nullable
    MatchResult matchIncoming(@Nullable String rawMessage) {
        if (!canReceive() || rawMessage == null) {
            return null;
        }

        switch (receiveMatchMode) {
            case EXACT:
                return receiveCommand.equals(rawMessage)
                        ? new MatchResult(Collections.emptyList())
                        : null;
            case PREFIX:
                return rawMessage.startsWith(receiveCommand)
                        ? new MatchResult(Collections.emptyList())
                        : null;
            case CONTAINS:
                return rawMessage.contains(receiveCommand)
                        ? new MatchResult(Collections.emptyList())
                        : null;
            case REGEX:
                if (compiledReceivePattern == null) {
                    return null;
                }
                Matcher matcher = compiledReceivePattern.matcher(rawMessage);
                if (!matcher.matches()) {
                    return null;
                }
                List<String> groups = new ArrayList<>();
                for (int index = 1; index <= matcher.groupCount(); index++) {
                    groups.add(matcher.group(index));
                }
                return new MatchResult(groups);
            default:
                return null;
        }
    }

    static final class MatchResult {
        @NonNull
        private final List<String> groups;

        private MatchResult(@NonNull List<String> groups) {
            this.groups = Collections.unmodifiableList(new ArrayList<>(groups));
        }

        @NonNull
        List<String> getGroups() {
            return groups;
        }
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CommandDefinition)) {
            return false;
        }
        CommandDefinition other = (CommandDefinition) object;
        return Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
