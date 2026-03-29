package com.example.wifidemo.sample.communication.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CommunicationDemoUiState {
    @NonNull
    private final String statusText;
    @NonNull
    private final String scenarioTitle;
    @NonNull
    private final String meaningText;
    @NonNull
    private final String exampleText;
    @NonNull
    private final String flowText;
    @NonNull
    private final String consoleText;
    private final boolean lastSuccess;

    public CommunicationDemoUiState(
            @Nullable String statusText,
            @Nullable String scenarioTitle,
            @Nullable String meaningText,
            @Nullable String exampleText,
            @Nullable String flowText,
            @Nullable String consoleText,
            boolean lastSuccess
    ) {
        this.statusText = normalize(statusText);
        this.scenarioTitle = normalize(scenarioTitle);
        this.meaningText = normalize(meaningText);
        this.exampleText = normalize(exampleText);
        this.flowText = normalize(flowText);
        this.consoleText = normalize(consoleText);
        this.lastSuccess = lastSuccess;
    }

    @NonNull
    public String getStatusText() {
        return statusText;
    }

    @NonNull
    public String getScenarioTitle() {
        return scenarioTitle;
    }

    @NonNull
    public String getMeaningText() {
        return meaningText;
    }

    @NonNull
    public String getExampleText() {
        return exampleText;
    }

    @NonNull
    public String getFlowText() {
        return flowText;
    }

    @NonNull
    public String getConsoleText() {
        return consoleText;
    }

    public boolean isLastSuccess() {
        return lastSuccess;
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value;
    }
}
