package com.example.wifidemo.sample.network.model;

import androidx.annotation.NonNull;

public class NetworkDemoUiState {
    private final String baseUrl;
    private final String statusText;
    private final String scenarioTitle;
    private final String requestPreview;
    private final String responsePreview;
    private final boolean lastSuccess;

    public NetworkDemoUiState(
            @NonNull String baseUrl,
            @NonNull String statusText,
            @NonNull String scenarioTitle,
            @NonNull String requestPreview,
            @NonNull String responsePreview,
            boolean lastSuccess
    ) {
        this.baseUrl = baseUrl;
        this.statusText = statusText;
        this.scenarioTitle = scenarioTitle;
        this.requestPreview = requestPreview;
        this.responsePreview = responsePreview;
        this.lastSuccess = lastSuccess;
    }

    @NonNull
    public static NetworkDemoUiState idle(@NonNull String baseUrl) {
        return new NetworkDemoUiState(
                baseUrl,
                "等待执行请求",
                "尚未执行请求",
                "点击上方任一按钮即可查看对应请求示例。",
                "这里会展示回显接口返回的结果、错误信息和关键头信息。",
                true
        );
    }

    @NonNull
    public String getBaseUrl() {
        return baseUrl;
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
    public String getRequestPreview() {
        return requestPreview;
    }

    @NonNull
    public String getResponsePreview() {
        return responsePreview;
    }

    public boolean isLastSuccess() {
        return lastSuccess;
    }
}
