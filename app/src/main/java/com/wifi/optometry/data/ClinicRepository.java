package com.wifi.optometry.data;

import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseRepository;
import com.wifi.optometry.communication.ServerConstance;
import com.wifi.optometry.domain.ExamWorkflowEngine;
import com.wifi.optometry.domain.model.ClinicSettings;
import com.wifi.optometry.domain.model.ConnectedDeviceInfo;
import com.wifi.optometry.domain.model.DeviceUiState;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.FunctionalTestState;
import com.wifi.optometry.domain.model.KnownDeviceSummary;
import com.wifi.optometry.domain.model.LensMeasurement;
import com.wifi.optometry.domain.model.PatientProfile;
import com.wifi.optometry.domain.model.ReportRecord;
import com.wifi.optometry.domain.model.VisualFunctionMetric;
import com.wifi.optometry.domain.model.VisionChart;
import com.wifi.optometry.util.ClinicFormatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClinicRepository extends BaseRepository {
    private static final String TAG = "ClinicRepository";
    private static final String FUNCTION_KEY_NPC = "npc";
    private static final String FUNCTION_KEY_NPA = "npa";
    private static final String FUNCTION_KEY_NRA = "nra";
    private static final String FUNCTION_KEY_PRA = "pra";
    private static final String FUNCTION_KEY_ACA_BI = "aca_bi";
    private static final String FUNCTION_KEY_ACA_TARGET = "aca_target";
    private static final String FUNCTION_KEY_AMP_RIGHT = "amp_right";
    private static final String FUNCTION_KEY_AMP_LEFT = "amp_left";
    private static final String FUNCTION_NOTE_KEY_ACA = "aca";
    private static final String FUNCTION_NOTE_KEY_AMP = "amp";
    private static volatile ClinicRepository instance;

    private interface DeviceUiStateChange {
        void apply(DeviceUiState state);
    }

    private final MutableLiveData<List<PatientProfile>> patientListLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PatientProfile>> patientSearchLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<VisionChart>> chartListLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ExamProgram>> programListLiveData = new MutableLiveData<>();
    private final MutableLiveData<ExamSession> sessionLiveData = new MutableLiveData<>();
    private final MutableLiveData<ExamStep> currentStepLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<VisualFunctionMetric>> currentMetricsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ReportRecord>> reportHistoryLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> qrPayloadLiveData = new MutableLiveData<>();
    private final MutableLiveData<ClinicSettings> settingsLiveData = new MutableLiveData<>();
    private final MutableLiveData<DeviceUiState> deviceUiStateLiveData = new MutableLiveData<>();

    private ClinicRepository() {
        List<PatientProfile> patients = ExamSeedData.createPatients();
        List<VisionChart> charts = ExamSeedData.createCharts();
        List<ExamProgram> programs = ExamSeedData.createPrograms();
        ClinicSettings settings = ExamSeedData.createSettings();
        ExamSession session = ExamSeedData.createInitialSession(patients.isEmpty() ? null : patients.get(0));

        patientListLiveData.setValue(patients);
        patientSearchLiveData.setValue(new ArrayList<>(patients));
        chartListLiveData.setValue(charts);
        programListLiveData.setValue(programs);
        settingsLiveData.setValue(settings);
        sessionLiveData.setValue(session);

        DeviceUiState deviceUiState = new DeviceUiState();
        deviceUiState.setServerPort(ServerConstance.SERVER_PORT);
        deviceUiStateLiveData.setValue(deviceUiState);

        List<ReportRecord> history = new ArrayList<>();
        history.add(createReportRecord(session, programs));
        reportHistoryLiveData.setValue(history);
        rebuildDerivedData();
        trace("Repository 初始化完成，patients=" + patients.size()
                + ", charts=" + charts.size()
                + ", programs=" + programs.size());
    }

    public static ClinicRepository getInstance() {
        if (instance == null) {
            synchronized (ClinicRepository.class) {
                if (instance == null) {
                    instance = new ClinicRepository();
                }
            }
        }
        return instance;
    }

    public LiveData<List<PatientProfile>> getPatientListLiveData() {
        return patientListLiveData;
    }

    public LiveData<List<PatientProfile>> getPatientSearchLiveData() {
        return patientSearchLiveData;
    }

    public LiveData<List<VisionChart>> getChartListLiveData() {
        return chartListLiveData;
    }

    public LiveData<List<ExamProgram>> getProgramListLiveData() {
        return programListLiveData;
    }

    public LiveData<ExamSession> getSessionLiveData() {
        return sessionLiveData;
    }

    public LiveData<ExamStep> getCurrentStepLiveData() {
        return currentStepLiveData;
    }

    public LiveData<List<VisualFunctionMetric>> getCurrentMetricsLiveData() {
        return currentMetricsLiveData;
    }

    public LiveData<List<ReportRecord>> getReportHistoryLiveData() {
        return reportHistoryLiveData;
    }

    public LiveData<String> getQrPayloadLiveData() {
        return qrPayloadLiveData;
    }

    public LiveData<ClinicSettings> getSettingsLiveData() {
        return settingsLiveData;
    }

    public LiveData<DeviceUiState> getDeviceUiStateLiveData() {
        return deviceUiStateLiveData;
    }

    public void searchPatients(String query) {
        List<PatientProfile> all = safeList(patientListLiveData.getValue());
        if (TextUtils.isEmpty(query)) {
            patientSearchLiveData.setValue(new ArrayList<>(all));
            trace("患者搜索重置为全量列表，数量=" + all.size());
            return;
        }

        String lower = query.trim().toLowerCase(Locale.getDefault());
        List<PatientProfile> result = new ArrayList<>();
        for (PatientProfile patient : all) {
            if ((patient.getName() != null && patient.getName().toLowerCase(Locale.getDefault()).contains(lower))
                    || (patient.getPhone() != null && patient.getPhone().contains(query.trim()))) {
                result.add(patient);
            }
        }
        patientSearchLiveData.setValue(result);
        trace("患者搜索完成，query=" + query + ", result=" + result.size());
    }

    public void savePatient(PatientProfile editedProfile) {
        if (editedProfile == null) {
            return;
        }

        List<PatientProfile> patients = safeList(patientListLiveData.getValue());
        boolean updated = false;
        for (int index = 0; index < patients.size(); index++) {
            if (TextUtils.equals(patients.get(index).getId(), editedProfile.getId())) {
                patients.set(index, editedProfile.copy());
                updated = true;
                break;
            }
        }
        if (!updated) {
            if (TextUtils.isEmpty(editedProfile.getId())) {
                editedProfile.setId("P" + String.format(Locale.getDefault(), "%03d", patients.size() + 1));
            }
            patients.add(0, editedProfile.copy());
        }
        patientListLiveData.setValue(patients);
        patientSearchLiveData.setValue(new ArrayList<>(patients));

        ExamSession session = requireSession();
        session.setPatient(editedProfile.copy());
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("患者资料已保存，id=" + editedProfile.getId() + ", total=" + patients.size());
    }

    public void selectPatient(String patientId) {
        if (TextUtils.isEmpty(patientId)) {
            return;
        }
        for (PatientProfile patient : safeList(patientListLiveData.getValue())) {
            if (patientId.equals(patient.getId())) {
                ExamSession session = requireSession();
                session.setPatient(patient.copy());
                sessionLiveData.setValue(session);
                rebuildDerivedData();
                trace("已切换当前患者，patientId=" + patientId);
                return;
            }
        }
    }

    public void selectChart(String chartId) {
        ExamSession session = requireSession();
        session.setSelectedChartId(chartId);
        sessionLiveData.setValue(session);
        trace("已切换视标，chartId=" + chartId);
    }

    public void selectField(ExamSession.MeasurementField field) {
        ExamSession session = requireSession();
        session.setSelectedField(field);
        sessionLiveData.setValue(session);
        trace("已切换验光字段，field=" + field);
    }

    public void setActiveEye(ExamSession.EyeSelection selection) {
        ExamSession session = requireSession();
        session.setActiveEye(selection);
        sessionLiveData.setValue(session);
        trace("已切换生效眼别，selection=" + selection);
    }

    public void setLensVisibility(ExamSession.EyeSelection selection) {
        ExamSession session = requireSession();
        session.setLensVisibility(selection);
        sessionLiveData.setValue(session);
        trace("已切换镜片显示范围，selection=" + selection);
    }

    public void toggleDistanceMode() {
        ExamSession session = requireSession();
        session.setDistanceMode(session.getDistanceMode() == ExamSession.DistanceMode.FAR
                ? ExamSession.DistanceMode.NEAR : ExamSession.DistanceMode.FAR);
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("已切换远近模式，current=" + session.getDistanceMode());
    }

    public void togglePrismMode() {
        ExamSession session = requireSession();
        session.setPrismMode(session.getPrismMode() == ExamSession.PrismMode.CARTESIAN
                ? ExamSession.PrismMode.POLAR : ExamSession.PrismMode.CARTESIAN);
        sessionLiveData.setValue(session);
        trace("已切换棱镜模式，current=" + session.getPrismMode());
    }

    public void toggleCylMode() {
        ExamSession session = requireSession();
        session.setCylMinusMode(!session.isCylMinusMode());
        normalizeCylinderSign(session.getFarRight(), session.isCylMinusMode());
        normalizeCylinderSign(session.getFarLeft(), session.isCylMinusMode());
        normalizeCylinderSign(session.getNearRight(), session.isCylMinusMode());
        normalizeCylinderSign(session.getNearLeft(), session.isCylMinusMode());
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("已切换柱镜记法，minusMode=" + session.isCylMinusMode());
    }

    public void toggleCpLink() {
        ExamSession session = requireSession();
        session.setCpLinked(!session.isCpLinked());
        sessionLiveData.setValue(session);
        trace("已切换 CP 联动，enabled=" + session.isCpLinked());
    }

    public void toggleLensInserted() {
        ExamSession session = requireSession();
        session.setLensInserted(!session.isLensInserted());
        sessionLiveData.setValue(session);
        trace("已切换镜片插入状态，inserted=" + session.isLensInserted());
    }

    public void toggleShiftEnabled() {
        ExamSession session = requireSession();
        session.setShiftEnabled(!session.isShiftEnabled());
        sessionLiveData.setValue(session);
        trace("已切换 Shift 步进，enabled=" + session.isShiftEnabled());
    }

    public void toggleNearLamp() {
        ExamSession session = requireSession();
        session.getFunctionalTests().setNearLampOn(!session.getFunctionalTests().isNearLampOn());
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("已切换近用灯状态，enabled=" + session.getFunctionalTests().isNearLampOn());
    }

    public void adjustSelectedMeasurement(boolean increase, boolean useShiftStep) {
        ExamSession session = requireSession();
        ClinicSettings settings = requireSettings();
        double delta = resolveFieldStep(session.getSelectedField(), settings, useShiftStep || session.isShiftEnabled());
        if (!increase) {
            delta = -delta;
        }
        applyMeasurementDelta(session, delta);
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("验光值调整完成，field=" + session.getSelectedField()
                + ", delta=" + delta
                + ", eye=" + session.getActiveEye()
                + ", mode=" + session.getDistanceMode());
    }

    public void adjustFunctionalValue(String key, boolean increase) {
        ExamSession session = requireSession();
        FunctionalTestState tests = session.getFunctionalTests();
        double direction = increase ? 1d : -1d;
        if (FUNCTION_KEY_NPC.equals(key)) {
            tests.setNpc(Math.max(0, tests.getNpc() + direction));
        } else if (FUNCTION_KEY_NPA.equals(key)) {
            tests.setNpa(Math.max(0, tests.getNpa() + direction));
        } else if (FUNCTION_KEY_NRA.equals(key)) {
            tests.setNra(tests.getNra() + direction * 0.25d);
        } else if (FUNCTION_KEY_PRA.equals(key)) {
            tests.setPra(Math.min(0, tests.getPra() + direction * 0.25d));
        } else if (FUNCTION_KEY_ACA_BI.equals(key)) {
            tests.setAcaBi(Math.max(0, tests.getAcaBi() + direction * 0.5d));
        } else if (FUNCTION_KEY_ACA_TARGET.equals(key)) {
            tests.setAcaTarget(Math.max(0, tests.getAcaTarget() + direction * 0.5d));
        } else if (FUNCTION_KEY_AMP_RIGHT.equals(key)) {
            tests.setAmpRight(Math.max(0, tests.getAmpRight() + direction));
        } else if (FUNCTION_KEY_AMP_LEFT.equals(key)) {
            tests.setAmpLeft(Math.max(0, tests.getAmpLeft() + direction));
        }
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("视功能数值调整完成，key=" + key + ", increase=" + increase);
    }

    public void markFunctionEvent(String key, String label) {
        ExamSession session = requireSession();
        FunctionalTestState tests = session.getFunctionalTests();
        String note = label + " " + ClinicFormatters.formatTimestamp(System.currentTimeMillis());
        if (FUNCTION_KEY_NRA.equals(key)) {
            tests.setNraNote(note);
        } else if (FUNCTION_KEY_PRA.equals(key)) {
            tests.setPraNote(note);
        } else if (FUNCTION_NOTE_KEY_ACA.equals(key)) {
            tests.setAcaNote(note);
        } else if (FUNCTION_NOTE_KEY_AMP.equals(key)) {
            tests.setAmpNote(note);
        }
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("视功能事件已标记，key=" + key + ", label=" + label);
    }

    public void selectProgram(String programId) {
        ExamSession session = requireSession();
        session.setCurrentProgramId(programId);
        session.setCurrentStepIndex(0);
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("已切换验光流程，programId=" + programId);
    }

    public void moveToNextStep() {
        ExamSession session = requireSession();
        ExamWorkflowEngine.moveNext(session, safeList(programListLiveData.getValue()));
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("流程推进到下一步，index=" + session.getCurrentStepIndex());
    }

    public void moveToPreviousStep() {
        ExamSession session = requireSession();
        ExamWorkflowEngine.movePrevious(session, safeList(programListLiveData.getValue()));
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("流程回退到上一步，index=" + session.getCurrentStepIndex());
    }

    public void appendCustomStep(String note) {
        List<ExamProgram> programs = safeList(programListLiveData.getValue());
        ExamProgram program = findProgram(requireSession().getCurrentProgramId(), programs);
        if (program == null) {
            return;
        }
        String stepId = "custom_" + System.currentTimeMillis();
        program.getSteps().add(new ExamStep(
                stepId,
                "自定义步骤",
                TextUtils.isEmpty(note) ? "门店自定义流程节点" : note,
                requireSession().getSelectedChartId(),
                requireSession().getDistanceMode() == ExamSession.DistanceMode.FAR ? ExamStep.DistanceMode.FAR : ExamStep.DistanceMode.NEAR,
                toScope(requireSession().getActiveEye()),
                "Final",
                requireSession().getSelectedField().name(),
                "",
                requireSession().getFunctionalTests().isNearLampOn() ? "开" : "关",
                "",
                "",
                ExamStep.Comparator.NONE,
                0,
                note
        ));
        programListLiveData.setValue(programs);
        rebuildDerivedData();
        trace("已追加自定义流程节点，stepId=" + stepId);
    }

    public void updateCurrentStepNote(String note) {
        ExamStep step = currentStepLiveData.getValue();
        if (step == null) {
            return;
        }
        step.setNote(note);
        currentStepLiveData.setValue(step);
        trace("已更新当前步骤备注");
    }

    public void updateSessionNote(String note) {
        ExamSession session = requireSession();
        session.setNote(note);
        sessionLiveData.setValue(session);
        trace("已更新会话备注");
    }

    public void saveSettings(ClinicSettings settings) {
        settingsLiveData.setValue(settings.copy());
        trace("门店设置已保存");
    }

    public void saveCurrentReport() {
        List<ReportRecord> history = safeList(reportHistoryLiveData.getValue());
        history.add(0, createReportRecord(requireSession(), safeList(programListLiveData.getValue())));
        reportHistoryLiveData.setValue(history);
        trace("当前报告已保存，history=" + history.size());
    }

    public void importLatestReport() {
        List<ReportRecord> history = safeList(reportHistoryLiveData.getValue());
        if (history.isEmpty()) {
            return;
        }
        ReportRecord latest = history.get(0);
        ExamSession session = requireSession();
        if (session.getPatient() == null) {
            session.setPatient(new PatientProfile());
        }
        session.getPatient().setName(latest.getPatientName());
        copyLens(latest.getFinalRight(), session.getFinalRight());
        copyLens(latest.getFinalLeft(), session.getFinalLeft());
        session.setNote("已导入最近一次报告：" + ClinicFormatters.formatTimestamp(latest.getCreatedAt()));
        sessionLiveData.setValue(session);
        rebuildDerivedData();
        trace("最近一次报告已导入，reportId=" + latest.getId());
    }

    public void updateServerState(boolean running, String ipAddress) {
        updateDeviceUiState(state -> {
            state.setServerRunning(running);
            state.setLocalIp(ipAddress);
        }, "设备服务状态已同步，running=" + running + ", ip=" + ipAddress);
    }

    public void setPendingDeviceMessage(String message) {
        updateDeviceUiState(
                state -> state.setPendingMessage(message),
                "待发送消息已更新，length=" + (message == null ? 0 : message.length())
        );
    }

    public void selectConnectedDevice(String clientId) {
        updateDeviceUiState(
                state -> state.setSelectedClientId(clientId),
                "当前在线模块已切换，clientId=" + clientId
        );
    }

    public void updateConnectedDevices(List<ConnectedDeviceInfo> devices) {
        updateDeviceUiState(state -> {
            state.getConnectedDevices().clear();
            state.getConnectedDevices().addAll(devices);
            if (TextUtils.isEmpty(state.getSelectedClientId()) && !devices.isEmpty()) {
                state.setSelectedClientId(devices.get(0).getClientId());
            }
        }, "在线模块列表已更新，count=" + devices.size());
    }

    public void updateKnownDevices(List<KnownDeviceSummary> devices) {
        updateDeviceUiState(state -> {
            state.getKnownDevices().clear();
            state.getKnownDevices().addAll(devices);
        }, "已建档模块列表已更新，count=" + devices.size());
    }

    public void appendDeviceLog(String line) {
        updateDeviceUiState(state -> {
            state.getLogs().add(0, line);
            while (state.getLogs().size() > 120) {
                state.getLogs().remove(state.getLogs().size() - 1);
            }
        }, null);
    }

    private void rebuildDerivedData() {
        currentStepLiveData.setValue(ExamWorkflowEngine.getCurrentStep(requireSession(), safeList(programListLiveData.getValue())));
        currentMetricsLiveData.setValue(buildVisualMetrics(requireSession()));
        qrPayloadLiveData.setValue(buildQrPayload(requireSession(), safeList(programListLiveData.getValue())));
        ExamStep currentStep = currentStepLiveData.getValue();
        trace("派生数据已重建，currentStep=" + (currentStep == null ? "none" : currentStep.getId()));
    }

    private void applyMeasurementDelta(ExamSession session, double delta) {
        if (session.getActiveEye() == ExamSession.EyeSelection.RIGHT || session.getActiveEye() == ExamSession.EyeSelection.BOTH) {
            applyMeasurementDelta(session, session.getDistanceMode(), session.getFarRight(), session.getNearRight(), delta);
        }
        if (session.getActiveEye() == ExamSession.EyeSelection.LEFT || session.getActiveEye() == ExamSession.EyeSelection.BOTH) {
            applyMeasurementDelta(session, session.getDistanceMode(), session.getFarLeft(), session.getNearLeft(), delta);
        }
        syncFinalPrescription(session);
    }

    private void applyMeasurementDelta(
            ExamSession session,
            ExamSession.DistanceMode distanceMode,
            LensMeasurement farMeasurement,
            LensMeasurement nearMeasurement,
            double delta
    ) {
        LensMeasurement target = distanceMode == ExamSession.DistanceMode.FAR ? farMeasurement : nearMeasurement;
        switch (session.getSelectedField()) {
            case SPH:
                target.setSph(target.getSph() + delta);
                break;
            case CYL:
                double magnitude = Math.max(0, Math.abs(target.getCyl()) + delta);
                target.setCyl(session.isCylMinusMode() ? -magnitude : magnitude);
                break;
            case AXIS:
                double axis = target.getAxis() + delta;
                while (axis < 0) {
                    axis += 180;
                }
                while (axis > 180) {
                    axis -= 180;
                }
                target.setAxis(axis);
                break;
            case ADD:
                target.setAdd(Math.max(0, target.getAdd() + delta));
                break;
            case VA:
                target.setVa(Math.max(0, target.getVa() + delta));
                break;
            case X:
                if (session.getPrismMode() == ExamSession.PrismMode.CARTESIAN) {
                    target.setPrismX(target.getPrismX() + delta);
                } else {
                    target.setPrismR(Math.max(0, target.getPrismR() + delta));
                }
                break;
            case Y:
                if (session.getPrismMode() == ExamSession.PrismMode.CARTESIAN) {
                    target.setPrismY(target.getPrismY() + delta);
                } else {
                    target.setPrismTheta((target.getPrismTheta() + delta + 360) % 360);
                }
                break;
            case PD:
                target.setPd(Math.max(0, target.getPd() + delta));
                break;
            default:
                break;
        }
    }

    private void syncFinalPrescription(ExamSession session) {
        copyLens(session.getFarRight(), session.getFinalRight());
        copyLens(session.getFarLeft(), session.getFinalLeft());
        session.getFinalRight().setAdd(session.getNearRight().getAdd());
        session.getFinalLeft().setAdd(session.getNearLeft().getAdd());
    }

    private void normalizeCylinderSign(LensMeasurement measurement, boolean minusMode) {
        measurement.setCyl(minusMode ? -Math.abs(measurement.getCyl()) : Math.abs(measurement.getCyl()));
    }

    private double resolveFieldStep(ExamSession.MeasurementField field, ClinicSettings settings, boolean shift) {
        switch (field) {
            case CYL:
                return shift ? settings.getCylShiftStep() : settings.getCylStep();
            case AXIS:
                return shift ? settings.getAxisShiftStep() : settings.getAxisStep();
            case X:
            case Y:
                return shift ? settings.getPrismShiftStep() : settings.getPrismStep();
            case PD:
                return shift ? settings.getPdShiftStep() : settings.getPdStep();
            case SPH:
            case ADD:
            case VA:
            default:
                return shift ? settings.getSphShiftStep() : settings.getSphStep();
        }
    }

    private List<VisualFunctionMetric> buildVisualMetrics(ExamSession session) {
        List<VisualFunctionMetric> metrics = new ArrayList<>();
        FunctionalTestState tests = session.getFunctionalTests();
        metrics.add(metric("融合", "WORTH 4 检测", "结果", "四点融合", "", "稳定", ""));
        metrics.add(metric("融合", "立体视觉", "结果", "80''", "", "良好", ""));
        metrics.add(metric("眼位", "远距隐斜", "水平", "BI 2", "BO1~BI3", "参考内", ""));
        metrics.add(metric("眼位", "近距隐斜", "水平", "BI 4", "0~BI6", "参考内", ""));
        metrics.add(metric("融像", "远距水平融像发散", "破裂", "BI 7", "BI5~BI9", "参考内", ""));
        metrics.add(metric("融像", "远距水平融像集合", "破裂", "BO 18", "BO15~BO23", "参考内", ""));
        metrics.add(metric("调节", "NPC 近点集合", "近距离点", ClinicFormatters.formatUnsigned(tests.getNpc()) + " cm", "2.5~7.5cm", tests.getNpc() <= 7.5 ? "参考内" : "偏离", ""));
        metrics.add(metric("调节", "NPA 近点调节", "结果", ClinicFormatters.formatUnsigned(tests.getNpa()) + " cm", "", "记录", ""));
        metrics.add(metric("调节", "NRA 负相对调节", "Bin 模糊", "+" + ClinicFormatters.formatUnsigned(tests.getNra()) + "D", "+1.75~+2.25D", tests.getNra() >= 1.75 && tests.getNra() <= 2.25 ? "参考内" : "偏离", tests.getNraNote()));
        metrics.add(metric("调节", "PRA 正相对调节", "Bin 模糊", ClinicFormatters.formatSigned(tests.getPra()) + "D", "-1.75D~-3.00D", tests.getPra() <= -1.75 && tests.getPra() >= -3.00 ? "参考内" : "偏离", tests.getPraNote()));
        metrics.add(metric("调节", "AC/A（梯度法）", "结果", ClinicFormatters.formatUnsigned(tests.getAcaTarget()) + " △D", "3△D~5△D", tests.getAcaTarget() >= 3 && tests.getAcaTarget() <= 5 ? "参考内" : "偏离", tests.getAcaNote()));
        metrics.add(metric("调节", "AMP 检查", "右/左", ClinicFormatters.formatUnsigned(tests.getAmpRight()) + " / " + ClinicFormatters.formatUnsigned(tests.getAmpLeft()), "", "记录", tests.getAmpNote()));
        return metrics;
    }

    private VisualFunctionMetric metric(String group, String title, String label, String result, String reference, String status, String note) {
        return new VisualFunctionMetric(group, title, label, result, reference, status, note);
    }

    private ReportRecord createReportRecord(ExamSession session, List<ExamProgram> programs) {
        syncFinalPrescription(session);
        ReportRecord record = new ReportRecord(
                "R" + System.currentTimeMillis(),
                session.getPatient() != null ? session.getPatient().getDisplayName() : "未命名被测者",
                resolveProgramTitle(session.getCurrentProgramId(), programs),
                System.currentTimeMillis(),
                "远用矫正视力 R " + ClinicFormatters.formatUnsigned(session.getFinalRight().getVa())
                        + " / L " + ClinicFormatters.formatUnsigned(session.getFinalLeft().getVa()),
                ClinicFormatters.buildPrescriptionSummary(session.getFinalRight(), session.getFinalLeft()),
                buildQrPayload(session, programs),
                session.getFinalRight().copy(),
                session.getFinalLeft().copy()
        );
        record.getMetrics().addAll(buildVisualMetrics(session));
        return record;
    }

    private String buildQrPayload(ExamSession session, List<ExamProgram> programs) {
        return "患者:" + (session.getPatient() == null ? "未命名" : session.getPatient().getDisplayName())
                + "\n程序:" + resolveProgramTitle(session.getCurrentProgramId(), programs)
                + "\nR:" + ClinicFormatters.buildLensSummary(session.getFinalRight())
                + "\nL:" + ClinicFormatters.buildLensSummary(session.getFinalLeft());
    }

    private String resolveProgramTitle(String programId, List<ExamProgram> programs) {
        ExamProgram program = findProgram(programId, programs);
        return program == null ? "未选择程序" : program.getTitle();
    }

    private ExamProgram findProgram(String programId, List<ExamProgram> programs) {
        if (programs == null) {
            return null;
        }
        for (ExamProgram program : programs) {
            if (TextUtils.equals(program.getId(), programId)) {
                return program;
            }
        }
        return null;
    }

    private DeviceUiState requireDeviceState() {
        DeviceUiState state = deviceUiStateLiveData.getValue();
        if (state == null) {
            state = new DeviceUiState();
            state.setServerPort(ServerConstance.SERVER_PORT);
            deviceUiStateLiveData.setValue(state);
        }
        return state;
    }

    private void updateDeviceUiState(DeviceUiStateChange change, String traceMessage) {
        DeviceUiState state = requireDeviceState();
        change.apply(state);
        deviceUiStateLiveData.setValue(state);
        if (!TextUtils.isEmpty(traceMessage)) {
            trace(traceMessage);
        }
    }

    private ExamSession requireSession() {
        ExamSession session = sessionLiveData.getValue();
        if (session == null) {
            session = ExamSeedData.createInitialSession(null);
            sessionLiveData.setValue(session);
        }
        return session;
    }

    private ClinicSettings requireSettings() {
        ClinicSettings settings = settingsLiveData.getValue();
        if (settings == null) {
            settings = ExamSeedData.createSettings();
            settingsLiveData.setValue(settings);
        }
        return settings;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? new ArrayList<>() : source;
    }

    private void copyLens(LensMeasurement from, LensMeasurement to) {
        if (from == null || to == null) {
            return;
        }
        to.setSph(from.getSph());
        to.setCyl(from.getCyl());
        to.setAxis(from.getAxis());
        to.setAdd(from.getAdd());
        to.setVa(from.getVa());
        to.setPd(from.getPd());
        to.setPrismX(from.getPrismX());
        to.setPrismY(from.getPrismY());
        to.setPrismR(from.getPrismR());
        to.setPrismTheta(from.getPrismTheta());
    }

    private ExamStep.EyeScope toScope(ExamSession.EyeSelection eyeSelection) {
        if (eyeSelection == ExamSession.EyeSelection.LEFT) {
            return ExamStep.EyeScope.LEFT;
        }
        if (eyeSelection == ExamSession.EyeSelection.BOTH) {
            return ExamStep.EyeScope.BOTH;
        }
        return ExamStep.EyeScope.RIGHT;
    }

    private void trace(String message) {
        DLog.i(TAG, message);
    }
}

