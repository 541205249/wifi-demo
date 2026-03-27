package com.wifi.lib.command;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OutboundCommand {
    @NonNull
    private final CommandDefinition definition;
    @NonNull
    private final String rawMessage;
    @NonNull
    private final Map<String, String> arguments;
    private final long resolvedAtMillis;

    OutboundCommand(
            @NonNull CommandDefinition definition,
            @NonNull String rawMessage,
            @NonNull Map<String, String> arguments,
            long resolvedAtMillis
    ) {
        this.definition = definition;
        this.rawMessage = rawMessage;
        this.arguments = Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
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
    public Map<String, String> getArguments() {
        return arguments;
    }

    public long getResolvedAtMillis() {
        return resolvedAtMillis;
    }
}
