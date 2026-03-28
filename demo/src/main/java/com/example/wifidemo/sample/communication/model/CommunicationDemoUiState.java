package com.example.wifidemo.sample.communication.model;

import androidx.annotation.NonNull;

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
            @NonNull String statusText,
            @NonNull String scenarioTitle,
            @NonNull String meaningText,
            @NonNull String exampleText,
            @NonNull String flowText,
            @NonNull String consoleText,
            boolean lastSuccess
    ) {
        this.statusText = statusText == null ? "" : statusText;
        this.scenarioTitle = scenarioTitle == null ? "" : scenarioTitle;
        this.meaningText = meaningText == null ? "" : meaningText;
        this.exampleText = exampleText == null ? "" : exampleText;
        this.flowText = flowText == null ? "" : flowText;
        this.consoleText = consoleText == null ? "" : consoleText;
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
}
