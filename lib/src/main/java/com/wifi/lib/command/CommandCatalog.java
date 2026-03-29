package com.wifi.lib.command;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.DLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * App 侧预留的指令目录。
 */
public final class CommandCatalog {
    private static final String TAG = "CommandCatalog";

    @NonNull
    private final List<CommandReservation> reservations;
    @NonNull
    private final Map<String, CommandReservation> reservationMap;

    private CommandCatalog(@NonNull List<CommandReservation> reservations) {
        List<CommandReservation> copiedReservations = new ArrayList<>(reservations);
        Collections.sort(copiedReservations, (left, right) -> left.getCode().compareTo(right.getCode()));
        this.reservations = Collections.unmodifiableList(copiedReservations);

        Map<String, CommandReservation> copiedMap = new LinkedHashMap<>();
        for (CommandReservation reservation : copiedReservations) {
            String code = reservation.getCodeValue();
            if (copiedMap.containsKey(code)) {
                throw new IllegalArgumentException("重复的指令预留编码: " + code);
            }
            copiedMap.put(code, reservation);
        }
        this.reservationMap = Collections.unmodifiableMap(copiedMap);
    }

    @NonNull
    public List<CommandReservation> getReservations() {
        return reservations;
    }

    @Nullable
    public CommandReservation findByCode(@NonNull String code) {
        return reservationMap.get(CommandCode.of(code).getValue());
    }

    @NonNull
    public ValidationResult validate(@NonNull CommandTable commandTable) {
        List<String> missingCodes = new ArrayList<>();
        List<String> unexpectedCodes = new ArrayList<>();
        List<String> unconfiguredCodes = new ArrayList<>();

        for (CommandReservation reservation : reservations) {
            CommandDefinition definition = commandTable.findByCode(reservation.getCodeValue());
            if (definition == null) {
                missingCodes.add(reservation.getCodeValue());
                continue;
            }
            if (!definition.isConfigured()) {
                unconfiguredCodes.add(definition.getCodeValue());
            }
        }

        for (CommandDefinition definition : commandTable.getDefinitions()) {
            if (reservationMap.containsKey(definition.getCodeValue())) {
                continue;
            }
            unexpectedCodes.add(definition.getCodeValue());
        }

        ValidationResult result = new ValidationResult(
                missingCodes,
                unexpectedCodes,
                unconfiguredCodes
        );
        DLog.i(TAG, "编码表校验完成: " + result.buildSummary());
        return result;
    }

    public static final class Builder {
        private final List<CommandReservation> reservations = new ArrayList<>();

        @NonNull
        public Builder addReservation(
                @NonNull String code,
                @NonNull String moduleName,
                @NonNull String subModuleName,
                @NonNull String actionName,
                @NonNull String codeExplanation,
                @NonNull String description
        ) {
            reservations.add(new CommandReservation(
                    code,
                    moduleName,
                    subModuleName,
                    actionName,
                    codeExplanation,
                    description
            ));
            return this;
        }

        @NonNull
        public Builder addReservation(
                @NonNull String code,
                @NonNull String moduleName,
                @NonNull String subModuleName,
                @NonNull String actionName
        ) {
            return addReservation(
                    code,
                    moduleName,
                    subModuleName,
                    actionName,
                    buildDefaultCodeExplanation(moduleName, subModuleName, actionName),
                    ""
            );
        }

        @NonNull
        public Builder addReservation(
                @NonNull String code,
                @NonNull String moduleName,
                @NonNull String subModuleName,
                @NonNull String actionName,
                @NonNull String description
        ) {
            return addReservation(
                    code,
                    moduleName,
                    subModuleName,
                    actionName,
                    buildDefaultCodeExplanation(moduleName, subModuleName, actionName),
                    description
            );
        }

        @NonNull
        private static String buildDefaultCodeExplanation(
                @NonNull String moduleName,
                @NonNull String subModuleName,
                @NonNull String actionName
        ) {
            return moduleName + "/" + subModuleName + "/" + actionName;
        }

        @NonNull
        public CommandCatalog build() {
            return new CommandCatalog(reservations);
        }
    }

    public static final class ValidationResult {
        @NonNull
        private final List<String> missingCodes;
        @NonNull
        private final List<String> unexpectedCodes;
        @NonNull
        private final List<String> unconfiguredCodes;

        private ValidationResult(
                @NonNull List<String> missingCodes,
                @NonNull List<String> unexpectedCodes,
                @NonNull List<String> unconfiguredCodes
        ) {
            this.missingCodes = Collections.unmodifiableList(new ArrayList<>(missingCodes));
            this.unexpectedCodes = Collections.unmodifiableList(new ArrayList<>(unexpectedCodes));
            this.unconfiguredCodes = Collections.unmodifiableList(new ArrayList<>(unconfiguredCodes));
        }

        public boolean hasIssues() {
            return !missingCodes.isEmpty()
                    || !unexpectedCodes.isEmpty();
        }

        public boolean hasUnconfiguredCodes() {
            return !unconfiguredCodes.isEmpty();
        }

        @NonNull
        public List<String> getMissingCodes() {
            return missingCodes;
        }

        @NonNull
        public List<String> getUnexpectedCodes() {
            return unexpectedCodes;
        }

        @NonNull
        public List<String> getUnconfiguredCodes() {
            return unconfiguredCodes;
        }

        @NonNull
        public String buildSummary() {
            return "missing=" + missingCodes.size()
                    + ", unexpected=" + unexpectedCodes.size()
                    + ", unconfigured=" + unconfiguredCodes.size();
        }
    }
}
