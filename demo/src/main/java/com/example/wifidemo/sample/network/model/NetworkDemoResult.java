package com.example.wifidemo.sample.network.model;

import androidx.annotation.NonNull;

public class NetworkDemoResult {
    private final String scenarioTitle;
    private final String statusText;
    private final String requestPreview;
    private final String responsePreview;
    private final boolean success;

    public NetworkDemoResult(
            @NonNull String scenarioTitle,
            @NonNull String statusText,
            @NonNull String requestPreview,
            @NonNull String responsePreview,
            boolean success
    ) {
        this.scenarioTitle = scenarioTitle;
        this.statusText = statusText;
        this.requestPreview = requestPreview;
        this.responsePreview = responsePreview;
        this.success = success;
    }

    @NonNull
    public String getScenarioTitle() {
        return scenarioTitle;
    }

    @NonNull
    public String getStatusText() {
        return statusText;
    }

    @NonNull
    public String getRequestPreview() {
        return requestPreview;
    }

    @NonNull
    public String getResponsePreview() {
        return responsePreview;
    }

    public boolean isSuccess() {
        return success;
    }
}
