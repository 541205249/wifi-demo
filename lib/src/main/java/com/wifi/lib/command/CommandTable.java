package com.wifi.lib.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 已加载的编码表。
 */
public final class CommandTable {
    @NonNull
    private static final CommandTable EMPTY = new CommandTable("empty", 0L, Collections.emptyList());

    @NonNull
    private final String sourceName;
    private final long loadedAtMillis;
    @NonNull
    private final List<CommandDefinition> definitions;
    @NonNull
    private final Map<String, CommandDefinition> definitionMap;

    public CommandTable(@NonNull String sourceName, long loadedAtMillis, @NonNull List<CommandDefinition> definitions) {
        this.sourceName = sourceName.trim();
        this.loadedAtMillis = loadedAtMillis;

        List<CommandDefinition> copiedDefinitions = new ArrayList<>(definitions);
        Collections.sort(copiedDefinitions, (left, right) -> Integer.compare(left.getOrder(), right.getOrder()));
        this.definitions = Collections.unmodifiableList(copiedDefinitions);

        Map<String, CommandDefinition> copiedMap = new LinkedHashMap<>();
        for (CommandDefinition definition : copiedDefinitions) {
            String code = definition.getCodeValue();
            if (copiedMap.containsKey(code)) {
                throw new IllegalArgumentException("编码表中存在重复编码: " + code);
            }
            copiedMap.put(code, definition);
        }
        this.definitionMap = Collections.unmodifiableMap(copiedMap);
    }

    @NonNull
    public static CommandTable empty() {
        return EMPTY;
    }

    @NonNull
    public String getSourceName() {
        return sourceName;
    }

    public long getLoadedAtMillis() {
        return loadedAtMillis;
    }

    @NonNull
    public List<CommandDefinition> getDefinitions() {
        return definitions;
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    public int size() {
        return definitions.size();
    }

    @Nullable
    public CommandDefinition findByCode(@NonNull String code) {
        return definitionMap.get(CommandCode.of(code).getValue());
    }

    @Nullable
    public InboundCommand matchIncoming(@Nullable String rawMessage) {
        if (rawMessage == null) {
            return null;
        }

        for (CommandDefinition definition : definitions) {
            CommandDefinition.MatchResult matchResult = definition.matchIncoming(rawMessage);
            if (matchResult == null) {
                continue;
            }
            return new InboundCommand(definition, rawMessage, matchResult.getGroups(), System.currentTimeMillis());
        }
        return null;
    }
}
