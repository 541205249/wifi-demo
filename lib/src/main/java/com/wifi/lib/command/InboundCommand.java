package com.wifi.lib.command;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InboundCommand {
    @NonNull
    private final CommandDefinition definition;
    @NonNull
    private final String rawMessage;
    @NonNull
    private final List<String> regexGroups;
    private final long resolvedAtMillis;

    InboundCommand(
            @NonNull CommandDefinition definition,
            @NonNull String rawMessage,
            @NonNull List<String> regexGroups,
            long resolvedAtMillis
    ) {
        this.definition = definition;
        this.rawMessage = rawMessage;
        this.regexGroups = Collections.unmodifiableList(new ArrayList<>(regexGroups));
        this.resolvedAtMillis = resolvedAtMillis;
    }

    @NonNull
    public CommandDefinition getDefinition() {
        return definition;
    }

    @NonNull
    public String getCode() {
        return definition.getCodeValue();
    }

    @NonNull
    public String getRawMessage() {
        return rawMessage;
    }

    @NonNull
    public List<String> getRegexGroups() {
        return regexGroups;
    }

    public long getResolvedAtMillis() {
        return resolvedAtMillis;
    }
}
