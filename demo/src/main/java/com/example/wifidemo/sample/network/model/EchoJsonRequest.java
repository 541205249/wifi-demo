package com.example.wifidemo.sample.network.model;

import java.util.ArrayList;
import java.util.List;

public class EchoJsonRequest {
    private String module;
    private String deviceId;
    private String commandCode;
    private String operator;
    private String requestAt;
    private List<String> checkpoints = new ArrayList<>();

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(String commandCode) {
        this.commandCode = commandCode;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(String requestAt) {
        this.requestAt = requestAt;
    }

    public List<String> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<String> checkpoints) {
        this.checkpoints = checkpoints;
    }
}
