package com.wifi.optometry.ui.state;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.wifi.lib.command.profile.OptometryCommandCodes;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseViewModel;
import com.wifi.optometry.communication.device.DeviceHistoryStore;
import com.wifi.optometry.data.ClinicRepository;
import com.wifi.optometry.domain.ExamWorkflowEngine;
import com.wifi.optometry.domain.model.ClinicSettings;
import com.wifi.optometry.domain.model.DeviceUiState;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.KnownDeviceSummary;
import com.wifi.optometry.domain.model.PatientProfile;
import com.wifi.optometry.domain.model.ReportRecord;
import com.wifi.optometry.domain.model.VisualFunctionMetric;
import com.wifi.optometry.domain.model.VisionChart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClinicViewModel extends BaseViewModel {
    private static final String TAG = "ClinicViewModel";
    private static final String PATIENT_IMPORT_SPLIT_REGEX = "[|,;]";

    private final ClinicRepository repository;
    private final DeviceHistoryStore deviceHistoryStore;
    private DeviceServiceGateway deviceServiceGateway;

    public ClinicViewModel(@NonNull Application application) {
        super(application);
        repository = ClinicRepository.getInstance();
        deviceHistoryStore = DeviceHistoryStore.getInstance(application);
        trace("ViewModel 初始化完成，开始同步设备状态");
        refreshDeviceState();
    }

    public LiveData<List<PatientProfile>> getPatients() {
        return repository.getPatientSearchLiveData();
    }

    public LiveData<List<VisionChart>> getCharts() {
        return repository.getChartListLiveData();
    }

    public LiveData<List<ExamProgram>> getPrograms() {
        return repository.getProgramListLiveData();
    }

    public LiveData<ExamSession> getSession() {
        return repository.getSessionLiveData();
    }

    public LiveData<ExamStep> getCurrentStep() {
        return repository.getCurrentStepLiveData();
    }

    public LiveData<List<VisualFunctionMetric>> getMetrics() {
        return repository.getCurrentMetricsLiveData();
    }

    public LiveData<List<ReportRecord>> getReports() {
        return repository.getReportHistoryLiveData();
    }

    public LiveData<String> getQrPayload() {
        return repository.getQrPayloadLiveData();
    }

    public LiveData<ClinicSettings> getSettings() {
        return repository.getSettingsLiveData();
    }

    public LiveData<DeviceUiState> getDeviceUiState() {
        return repository.getDeviceUiStateLiveData();
    }

    public void setDeviceServiceGateway(DeviceServiceGateway deviceServiceGateway) {
        this.deviceServiceGateway = deviceServiceGateway;
        trace("绑定 DeviceServiceGateway，网关可用=" + (deviceServiceGateway != null));
        refreshDeviceState();
    }

    public void refreshDeviceState() {
        boolean serverRunning = deviceServiceGateway != null && deviceServiceGateway.isServerRunning();
        String ipAddress = deviceServiceGateway != null ? deviceServiceGateway.getLocalIpAddress() : null;
        trace("刷新设备状态，serverRunning=" + serverRunning + ", ip=" + ipAddress);
        repository.updateServerState(serverRunning, ipAddress);
        repository.updateConnectedDevices(deviceServiceGateway != null
                ? deviceServiceGateway.getConnectedDevices() : new ArrayList<>());
        repository.updateKnownDevices(loadKnownDevices());
    }

    public int getProgressPercent() {
        ExamSession session = repository.getSessionLiveData().getValue();
        List<ExamProgram> programs = repository.getProgramListLiveData().getValue();
        if (session == null) {
            return 0;
        }
        return ExamWorkflowEngine.getProgressPercent(session, programs);
    }

    public void searchPatients(String query) {
        trace("收到患者搜索请求，query=" + query);
        repository.searchPatients(query);
    }

    public void savePatient(PatientProfile profile) {
        trace("收到患者保存请求，id=" + (profile == null ? null : profile.getId()));
        repository.savePatient(profile);
    }

    public void importPatientFromCode(String payload) {
        if (TextUtils.isEmpty(payload)) {
            return;
        }
        trace("收到扫码导入患者请求，payloadLength=" + payload.length());
        savePatient(parseImportedPatientProfile(payload));
    }

    private PatientProfile parseImportedPatientProfile(String payload) {
        String[] parts = payload.split(PATIENT_IMPORT_SPLIT_REGEX);
        PatientProfile profile = new PatientProfile();
        applyImportedPatientParts(profile, parts);
        return profile;
    }

    private void applyImportedPatientParts(PatientProfile profile, String[] parts) {
        if (parts.length > 0) {
            profile.setName(parts[0].trim());
        }
        if (parts.length > 1) {
            profile.setPhone(parts[1].trim());
        }
        if (parts.length > 2) {
            profile.setGender(parts[2].trim());
        }
        if (parts.length > 3) {
            profile.setBirthDate(parts[3].trim());
        }
        if (parts.length > 4) {
            profile.setAddress(parts[4].trim());
        }
        if (parts.length > 5) {
            profile.setNote(parts[5].trim());
        }
    }

    public void selectPatient(String patientId) {
        trace("切换当前患者，patientId=" + patientId);
        repository.selectPatient(patientId);
    }

    public void selectProgram(String programId) {
        trace("切换验光流程，programId=" + programId);
        repository.selectProgram(programId);
    }

    public void appendCustomProgramStep(String note) {
        trace("追加自定义流程节点，note=" + note);
        repository.appendCustomStep(note);
    }

    public void moveToNextStep() {
        trace("请求进入下一流程步骤");
        repository.moveToNextStep();
    }

    public void moveToPreviousStep() {
        trace("请求返回上一流程步骤");
        repository.moveToPreviousStep();
    }

    public void skipCurrentStep() {
        trace("请求跳过当前流程步骤");
        repository.skipCurrentStep();
    }

    public void updateCurrentStepNote(String note) {
        repository.updateCurrentStepNote(note);
    }

    public void updateSessionNote(String note) {
        repository.updateSessionNote(note);
    }

    public void selectChart(String chartId) {
        repository.selectChart(chartId);
    }

    public void selectField(ExamSession.MeasurementField field) {
        repository.selectField(field);
    }

    public void setLensDataSource(ExamSession.LensDataSource dataSource) {
        repository.setLensDataSource(dataSource);
    }

    public void setActiveEye(ExamSession.EyeSelection selection) {
        repository.setActiveEye(selection);
    }

    public void setLensVisibility(ExamSession.EyeSelection selection) {
        repository.setLensVisibility(selection);
    }

    public void selectActiveTool(ExamSession.ToolType toolType) {
        repository.setActiveTool(toolType);
    }

    public void toggleDistanceMode() {
        repository.toggleDistanceMode();
    }

    public void togglePrismMode() {
        repository.togglePrismMode();
    }

    public void toggleCylMode() {
        repository.toggleCylMode();
    }

    public void toggleCpLink() {
        repository.toggleCpLink();
    }

    public void toggleLensInserted() {
        repository.toggleLensInserted();
    }

    public void toggleShiftEnabled() {
        repository.toggleShiftEnabled();
    }

    public void toggleNearLamp() {
        repository.toggleNearLamp();
    }

    public void adjustMeasurement(boolean increase, boolean useShiftStep) {
        trace("调整验光值，increase=" + increase + ", shift=" + useShiftStep);
        repository.adjustSelectedMeasurement(increase, useShiftStep);
    }

    public void clearSelectedMeasurement() {
        trace("清空当前验光字段");
        repository.clearSelectedMeasurement();
    }

    public void adjustFunctionalValue(String key, boolean increase) {
        trace("调整视功能数值，key=" + key + ", increase=" + increase);
        repository.adjustFunctionalValue(key, increase);
    }

    public void markFunctionEvent(String key, String label) {
        trace("记录视功能事件，key=" + key + ", label=" + label);
        repository.markFunctionEvent(key, label);
    }

    public void saveSettings(ClinicSettings settings) {
        repository.saveSettings(settings);
    }

    public void saveCurrentReport() {
        trace("请求保存当前报告");
        repository.saveCurrentReport();
    }

    public void importLatestReport() {
        trace("请求导入最近一次报告");
        repository.importLatestReport();
    }

    public void createProgram(String title, String summary, String description, boolean copyCurrentProgram) {
        trace("请求创建新程序，title=" + title + ", copyCurrent=" + copyCurrentProgram);
        repository.createProgram(title, summary, description, copyCurrentProgram);
    }

    public void updatePendingMessage(String message) {
        trace("更新待发送消息，长度=" + (message == null ? 0 : message.length()));
        repository.setPendingDeviceMessage(message);
    }

    public void selectConnectedDevice(String clientId) {
        trace("选择在线模块，clientId=" + clientId);
        repository.selectConnectedDevice(clientId);
    }

    public void bindMainDevice(String clientId) {
        trace("绑定主设备，clientId=" + clientId);
        repository.bindMainDevice(clientId);
        appendDeviceConsole("已绑定当前主设备");
    }

    public void unbindMainDevice() {
        trace("解除主设备绑定");
        repository.unbindMainDevice();
        appendDeviceConsole("已解除主设备绑定");
    }

    public void startServer() {
        if (!ensureDeviceServiceGateway("服务尚未绑定，暂时无法启动监听")) {
            return;
        }
        trace("请求启动 WiFi 监听服务");
        deviceServiceGateway.startServer();
        appendConsoleAndRefreshDeviceState("已请求启动 WiFi 监听服务");
    }

    public void stopServer() {
        if (!ensureDeviceServiceGateway(null)) {
            return;
        }
        trace("请求停止 WiFi 监听服务");
        deviceServiceGateway.stopServer();
        appendConsoleAndRefreshDeviceState("已请求停止 WiFi 监听服务");
    }

    public void broadcastMessage(String message) {
        if (!ensureDeviceServiceGateway(null) || TextUtils.isEmpty(message)) {
            return;
        }
        trace("请求广播指令，长度=" + message.length());
        deviceServiceGateway.broadcastMessage(message);
        appendConsoleAndRefreshDeviceState("广播指令: " + message);
    }

    public void sendMessageToClient(String clientId, String message) {
        if (!ensureDeviceServiceGateway(null) || TextUtils.isEmpty(clientId) || TextUtils.isEmpty(message)) {
            return;
        }
        trace("请求定向发送，clientId=" + clientId + ", 长度=" + message.length());
        deviceServiceGateway.sendMessageToClient(clientId, message);
        appendConsoleAndRefreshDeviceState("发送到 " + clientId + ": " + message);
    }

    public void sendMessageToSelectedClient(String message) {
        DeviceUiState state = repository.getDeviceUiStateLiveData().getValue();
        if (state == null) {
            return;
        }
        sendMessageToClient(state.getSelectedClientId(), message);
    }

    public void queryBoundMainDeviceInfo() {
        DeviceUiState state = repository.getDeviceUiStateLiveData().getValue();
        if (state == null || TextUtils.isEmpty(state.getBoundCommandClientId())) {
            appendDeviceConsole("当前没有可查询的在线主设备");
            return;
        }
        if (!ensureDeviceServiceGateway("服务尚未绑定，暂时无法查询模块信息")) {
            return;
        }
        trace("向主设备发送模块信息查询，clientId=" + state.getBoundCommandClientId());
        deviceServiceGateway.sendCommandToClient(
                state.getBoundCommandClientId(),
                OptometryCommandCodes.CODE_QUERY_MODULE_INFO,
                null
        );
        appendDeviceConsole("已向主设备发送模块信息查询");
    }

    public void appendDeviceConsole(String line) {
        repository.appendDeviceLog("[" + DeviceHistoryStore.formatTimestamp(System.currentTimeMillis()) + "] " + line);
    }

    private void appendConsoleAndRefreshDeviceState(String line) {
        appendDeviceConsole(line);
        refreshDeviceState();
    }

    private boolean ensureDeviceServiceGateway(String unavailableMessage) {
        if (deviceServiceGateway != null) {
            return true;
        }
        if (!TextUtils.isEmpty(unavailableMessage)) {
            appendDeviceConsole(unavailableMessage);
        }
        return false;
    }

    private List<KnownDeviceSummary> loadKnownDevices() {
        List<DeviceHistoryStore.DeviceSummary> source = deviceHistoryStore.getKnownDevices();
        if (source == null || source.isEmpty()) {
            trace("加载已建档模块列表完成，数量=0");
            return Collections.emptyList();
        }
        List<KnownDeviceSummary> result = new ArrayList<>();
        for (DeviceHistoryStore.DeviceSummary device : source) {
            result.add(new KnownDeviceSummary(
                    device.getDeviceId(),
                    device.getSelectionLabel(),
                    device.getLastKnownIp(),
                    device.getLastSeenAt(),
                    device.isCurrentlyConnected(),
                    device.getCommunicationCount(),
                    device.getConnectionCount()
            ));
        }
        trace("加载已建档模块列表完成，数量=" + result.size());
        return result;
    }

    private void trace(String message) {
        DLog.i(TAG, message);
    }
}

