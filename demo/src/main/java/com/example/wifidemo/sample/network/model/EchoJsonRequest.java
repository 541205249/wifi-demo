package com.example.wifidemo.sample.network.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EchoJsonRequest {
    private String module;
    private String deviceId;
    private String commandCode;
    private String operator;
    private String requestAt;
    private final List<String> checkpoints = new ArrayList<>();

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = normalize(module);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = normalize(deviceId);
    }

    public String getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(String commandCode) {
        this.commandCode = normalize(commandCode);
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = normalize(operator);
    }

    public String getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(String requestAt) {
        this.requestAt = normalize(requestAt);
    }

    @NonNull
    public List<String> getCheckpoints() {
        return new ArrayList<>(checkpoints);
    }

    public void setCheckpoints(@Nullable List<String> checkpoints) {
        this.checkpoints.clear();
        if (checkpoints != null) {
            this.checkpoints.addAll(checkpoints);
        }
    }

    @NonNull
    private String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
