package com.wifi.optometry.ui.state;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

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

public class ClinicViewModel extends AndroidViewModel {
    private final ClinicRepository repository;
    private final DeviceHistoryStore deviceHistoryStore;
    private DeviceServiceGateway deviceServiceGateway;

    public ClinicViewModel(@NonNull Application application) {
        super(application);
        repository = ClinicRepository.getInstance();
        deviceHistoryStore = DeviceHistoryStore.getInstance(application);
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
        refreshDeviceState();
    }

    public void refreshDeviceState() {
        boolean serverRunning = deviceServiceGateway != null && deviceServiceGateway.isServerRunning();
        String ipAddress = deviceServiceGateway != null ? deviceServiceGateway.getLocalIpAddress() : null;
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
        repository.searchPatients(query);
    }

    public void savePatient(PatientProfile profile) {
        repository.savePatient(profile);
    }

    public void importPatientFromCode(String payload) {
        if (TextUtils.isEmpty(payload)) {
            return;
        }
        String[] parts = payload.split("[|,;]");
        PatientProfile profile = new PatientProfile();
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
        savePatient(profile);
    }

    public void selectPatient(String patientId) {
        repository.selectPatient(patientId);
    }

    public void selectProgram(String programId) {
        repository.selectProgram(programId);
    }

    public void appendCustomProgramStep(String note) {
        repository.appendCustomStep(note);
    }

    public void moveToNextStep() {
        repository.moveToNextStep();
    }

    public void moveToPreviousStep() {
        repository.moveToPreviousStep();
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

    public void setActiveEye(ExamSession.EyeSelection selection) {
        repository.setActiveEye(selection);
    }

    public void setLensVisibility(ExamSession.EyeSelection selection) {
        repository.setLensVisibility(selection);
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
        repository.adjustSelectedMeasurement(increase, useShiftStep);
    }

    public void adjustFunctionalValue(String key, boolean increase) {
        repository.adjustFunctionalValue(key, increase);
    }

    public void markFunctionEvent(String key, String label) {
        repository.markFunctionEvent(key, label);
    }

    public void saveSettings(ClinicSettings settings) {
        repository.saveSettings(settings);
    }

    public void saveCurrentReport() {
        repository.saveCurrentReport();
    }

    public void importLatestReport() {
        repository.importLatestReport();
    }

    public void updatePendingMessage(String message) {
        repository.setPendingDeviceMessage(message);
    }

    public void selectConnectedDevice(String clientId) {
        repository.selectConnectedDevice(clientId);
    }

    public void startServer() {
        if (deviceServiceGateway == null) {
            appendDeviceConsole("服务尚未绑定，暂时无法启动监听");
            return;
        }
        deviceServiceGateway.startServer();
        appendDeviceConsole("已请求启动 WiFi 监听服务");
        refreshDeviceState();
    }

    public void stopServer() {
        if (deviceServiceGateway == null) {
            return;
        }
        deviceServiceGateway.stopServer();
        appendDeviceConsole("已请求停止 WiFi 监听服务");
        refreshDeviceState();
    }

    public void broadcastMessage(String message) {
        if (deviceServiceGateway == null || TextUtils.isEmpty(message)) {
            return;
        }
        deviceServiceGateway.broadcastMessage(message);
        appendDeviceConsole("广播指令: " + message);
        refreshDeviceState();
    }

    public void sendMessageToClient(String clientId, String message) {
        if (deviceServiceGateway == null || TextUtils.isEmpty(clientId) || TextUtils.isEmpty(message)) {
            return;
        }
        deviceServiceGateway.sendMessageToClient(clientId, message);
        appendDeviceConsole("发送到 " + clientId + ": " + message);
        refreshDeviceState();
    }

    public void sendMessageToSelectedClient(String message) {
        DeviceUiState state = repository.getDeviceUiStateLiveData().getValue();
        if (state == null) {
            return;
        }
        sendMessageToClient(state.getSelectedClientId(), message);
    }

    public void appendDeviceConsole(String line) {
        repository.appendDeviceLog("[" + DeviceHistoryStore.formatTimestamp(System.currentTimeMillis()) + "] " + line);
    }

    private List<KnownDeviceSummary> loadKnownDevices() {
        List<DeviceHistoryStore.DeviceSummary> source = deviceHistoryStore.getKnownDevices();
        if (source == null || source.isEmpty()) {
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
        return result;
    }
}
