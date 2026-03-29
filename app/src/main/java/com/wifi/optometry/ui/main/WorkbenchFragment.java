package com.wifi.optometry.ui.main;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wifi.optometry.R;
import com.wifi.optometry.databinding.DialogDeviceBindBinding;
import com.wifi.optometry.databinding.DialogMenuPanelBinding;
import com.wifi.optometry.databinding.DialogPasswordGateBinding;
import com.wifi.optometry.databinding.DialogProgramDetailBinding;
import com.wifi.optometry.databinding.DialogProgramPickerBinding;
import com.wifi.optometry.databinding.DialogStepLengthBinding;
import com.wifi.optometry.databinding.DialogSubjectManageBinding;
import com.wifi.optometry.databinding.DialogTimeLanguageBinding;
import com.wifi.optometry.databinding.FragmentWorkbenchBinding;
import com.wifi.optometry.databinding.ViewTimeLanguageAdjustRowBinding;
import com.wifi.optometry.domain.model.ClinicSettings;
import com.wifi.optometry.domain.model.ConnectedDeviceInfo;
import com.wifi.optometry.domain.model.DeviceUiState;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.KnownDeviceSummary;
import com.wifi.optometry.domain.model.LensMeasurement;
import com.wifi.optometry.domain.model.PatientProfile;
import com.wifi.optometry.domain.model.VisionChart;
import com.wifi.optometry.ui.shared.SimpleTextWatcher;
import com.wifi.optometry.util.ClinicFormatters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class WorkbenchFragment extends BaseClinicFragment<FragmentWorkbenchBinding> {
    private static final String FUNCTION_NPC = "npc";
    private static final String FUNCTION_NPA = "npa";
    private static final String FUNCTION_NRA = "nra";
    private static final String FUNCTION_PRA = "pra";
    private static final String FUNCTION_ACA_BI = "aca_bi";
    private static final String FUNCTION_ACA_TARGET = "aca_target";
    private static final String FUNCTION_ACA = "aca";
    private static final String FUNCTION_AMP_RIGHT = "amp_right";
    private static final String FUNCTION_AMP_LEFT = "amp_left";
    private static final double[] SPH_STEP_OPTIONS = {0.12d, 0.25d};
    private static final double[] SPH_SHIFT_OPTIONS = {0.50d, 1.00d, 3.00d};
    private static final double[] CYL_STEP_OPTIONS = {0.25d, 0.50d};
    private static final double[] CYL_SHIFT_OPTIONS = {1.00d, 2.00d, 3.00d};
    private static final double[] AXIS_STEP_OPTIONS = {1.00d, 5.00d};
    private static final double[] AXIS_SHIFT_OPTIONS = {15.00d, 30.00d};
    private static final double[] PRISM_STEP_OPTIONS = {0.10d, 0.50d};
    private static final double[] PRISM_SHIFT_OPTIONS = {1.00d, 2.00d, 3.00d};
    private static final double[] PD_STEP_OPTIONS = {0.10d, 0.50d};
    private static final double[] PD_SHIFT_OPTIONS = {1.00d, 2.00d, 3.00d, 5.00d};
    private static final String[] LANGUAGE_OPTIONS = {"中文", "English"};
    private static final String[] DISPLAY_DURATION_OPTIONS = {"显示", "隐藏"};
    private static final SimpleDateFormat CLOCK_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private static final class MetricRowHolder {
        private final LinearLayout row;
        private final TextView label;
        private final TextView rightValue;
        private final TextView leftValue;

        private MetricRowHolder(LinearLayout row, @Nullable TextView label, TextView rightValue, TextView leftValue) {
            this.row = row;
            this.label = label;
            this.rightValue = rightValue;
            this.leftValue = leftValue;
        }
    }

    private interface TimeLanguageSelectionListener {
        void onItemSelected(String value);
    }

    private final List<VisionChart> chartList = new ArrayList<>();
    private final List<ExamProgram> programList = new ArrayList<>();
    private final List<PatientProfile> patientList = new ArrayList<>();
    private final LinkedHashMap<ExamSession.MeasurementField, MetricRowHolder> metricRows = new LinkedHashMap<>();
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTicker = new Runnable() {
        @Override
        public void run() {
            renderClock();
            clockHandler.postDelayed(this, 1000L);
        }
    };

    private ExamSession session;
    private ExamStep currentStep;
    private DeviceUiState deviceUiState;
    private ClinicSettings settings;

    private AlertDialog subjectDialog;
    private DialogSubjectManageBinding subjectDialogBinding;
    private boolean subjectCreateMode = true;
    private boolean subjectFormInitialized;

    private AlertDialog deviceDialog;
    private DialogDeviceBindBinding deviceDialogBinding;

    private AlertDialog programDialog;
    private DialogProgramPickerBinding programDialogBinding;
    private AlertDialog programDetailDialog;
    private DialogProgramDetailBinding programDetailDialogBinding;
    private ExamProgram detailProgram;
    private int detailProgramStepIndex;

    private AlertDialog stepLengthDialog;
    private DialogStepLengthBinding stepLengthDialogBinding;
    private ClinicSettings draftStepLengthSettings;

    private AlertDialog timeLanguageDialog;
    private DialogTimeLanguageBinding timeLanguageDialogBinding;
    private ClinicSettings draftTimeLanguageSettings;

    private AlertDialog passwordDialog;
    private DialogPasswordGateBinding passwordDialogBinding;

    private AlertDialog menuDialog;
    private DialogMenuPanelBinding menuDialogBinding;

    @Nullable
    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        bindMetricRows();
        bindMainActions();
        renderClock();
        clockHandler.post(clockTicker);
    }

    @Override
    protected void observeUi() {
        clinicViewModel.getCharts().observe(getViewLifecycleOwner(), charts -> {
            chartList.clear();
            if (charts != null) {
                chartList.addAll(charts);
            }
            renderAll();
        });
        clinicViewModel.getPrograms().observe(getViewLifecycleOwner(), programs -> {
            programList.clear();
            if (programs != null) {
                programList.addAll(programs);
            }
            renderAll();
        });
        clinicViewModel.getPatients().observe(getViewLifecycleOwner(), patients -> {
            patientList.clear();
            if (patients != null) {
                patientList.addAll(patients);
            }
            renderAll();
        });
        clinicViewModel.getSession().observe(getViewLifecycleOwner(), examSession -> {
            session = examSession;
            renderAll();
        });
        clinicViewModel.getCurrentStep().observe(getViewLifecycleOwner(), examStep -> {
            currentStep = examStep;
            renderAll();
        });
        clinicViewModel.getDeviceUiState().observe(getViewLifecycleOwner(), state -> {
            deviceUiState = state;
            renderAll();
        });
        clinicViewModel.getSettings().observe(getViewLifecycleOwner(), clinicSettings -> {
            settings = clinicSettings;
            renderAll();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clockHandler.removeCallbacks(clockTicker);
        dismissManagedDialog(subjectDialog);
        dismissManagedDialog(deviceDialog);
        dismissManagedDialog(programDialog);
        dismissManagedDialog(programDetailDialog);
        dismissManagedDialog(stepLengthDialog);
        dismissManagedDialog(timeLanguageDialog);
        dismissManagedDialog(passwordDialog);
        dismissManagedDialog(menuDialog);
        subjectDialogBinding = null;
        deviceDialogBinding = null;
        programDialogBinding = null;
        programDetailDialogBinding = null;
        detailProgram = null;
        stepLengthDialogBinding = null;
        draftStepLengthSettings = null;
        timeLanguageDialogBinding = null;
        draftTimeLanguageSettings = null;
        passwordDialogBinding = null;
        menuDialogBinding = null;
    }

    private void bindMetricRows() {
        registerMetricRow(ExamSession.MeasurementField.SPH, binding.rowMetricSph, null, binding.tvValueSphRight, binding.tvValueSphLeft);
        registerMetricRow(ExamSession.MeasurementField.CYL, binding.rowMetricCyl, null, binding.tvValueCylRight, binding.tvValueCylLeft);
        registerMetricRow(ExamSession.MeasurementField.AXIS, binding.rowMetricAxis, null, binding.tvValueAxisRight, binding.tvValueAxisLeft);
        registerMetricRow(ExamSession.MeasurementField.ADD, binding.rowMetricAdd, null, binding.tvValueAddRight, binding.tvValueAddLeft);
        registerMetricRow(ExamSession.MeasurementField.VA, binding.rowMetricVa, null, binding.tvValueVaRight, binding.tvValueVaLeft);
        registerMetricRow(ExamSession.MeasurementField.X, binding.rowMetricX, binding.tvMetricXLabel, binding.tvValueXRight, binding.tvValueXLeft);
        registerMetricRow(ExamSession.MeasurementField.Y, binding.rowMetricY, binding.tvMetricYLabel, binding.tvValueYRight, binding.tvValueYLeft);
    }

    private void registerMetricRow(
            ExamSession.MeasurementField field,
            LinearLayout row,
            @Nullable TextView label,
            TextView rightValue,
            TextView leftValue
    ) {
        metricRows.put(field, new MetricRowHolder(row, label, rightValue, leftValue));
        row.setOnClickListener(v -> clinicViewModel.selectField(field));
    }

    private void bindMainActions() {
        binding.btnTopClear.setOnClickListener(v -> clearCurrentSelection());
        binding.btnTopMenu.setOnClickListener(v -> showPasswordDialog());
        binding.btnTopPrismMode.setOnClickListener(v -> clinicViewModel.togglePrismMode());
        binding.btnTopCylinderMode.setOnClickListener(v -> clinicViewModel.toggleCylMode());
        binding.btnTopDistanceMode.setOnClickListener(v -> toggleDistanceMode());

        binding.btnActionSubject.setOnClickListener(v -> showSubjectDialog());
        binding.btnActionHelp.setOnClickListener(v -> showChartHelp());
        binding.btnActionIn.setOnClickListener(v -> handleLensInserted());
        binding.btnActionPrint.setOnClickListener(v -> handlePrint());
        binding.btnActionShift.setOnClickListener(v -> toggleShift());
        binding.btnActionReport.setOnClickListener(v -> saveCurrentReport());

        binding.cardEyeRight.setOnClickListener(v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.RIGHT));
        binding.cardEyeLeft.setOnClickListener(v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.LEFT));
        binding.tvEyeBoth.setOnClickListener(v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.BOTH));

        binding.btnAdjustPlus.setOnClickListener(v -> clinicViewModel.adjustMeasurement(true, false));
        binding.btnAdjustMinus.setOnClickListener(v -> clinicViewModel.adjustMeasurement(false, false));
        binding.btnShiftPlus.setOnClickListener(v -> handleShiftButton(true));
        binding.btnShiftMinus.setOnClickListener(v -> handleShiftButton(false));

        binding.btnPrevStep.setOnClickListener(v -> clinicViewModel.moveToPreviousStep());
        binding.btnNextStep.setOnClickListener(v -> clinicViewModel.moveToNextStep());
        binding.btnSkipStep.setOnClickListener(v -> clinicViewModel.skipCurrentStep());
        binding.btnSelectProgram.setOnClickListener(v -> showProgramDialog());
        binding.tvStepLength.setOnClickListener(v -> showStepLengthDialog());

        binding.btnFarMode.setOnClickListener(v -> changeDistanceMode(ExamSession.DistanceMode.FAR));
        binding.btnNearMode.setOnClickListener(v -> changeDistanceMode(ExamSession.DistanceMode.NEAR));
        binding.btnLampToggle.setOnClickListener(v -> toggleLamp());
        binding.btnChartHelp.setOnClickListener(v -> showChartHelp());

        binding.btnToolPlus.setOnClickListener(v -> handleToolAdjust(true));
        binding.btnToolMinus.setOnClickListener(v -> handleToolAdjust(false));
        binding.btnToolExtraPrimary.setOnClickListener(v -> handleToolExtraPrimary());
        binding.btnToolExtraSecondary.setOnClickListener(v -> handleToolExtraSecondary());
    }

    private void renderAll() {
        if (binding == null) {
            return;
        }
        renderTopBar();
        renderLeftRail();
        renderEyeSelection();
        renderMetrics();
        renderPreviewAndPrograms();
        renderRightPanel();
        renderToolTabs();
        renderToolOverlay();
        renderManagedDialogs();
    }

    private void renderTopBar() {
        binding.tvTopPdValue.setText("PD = " + resolveTotalPd());
        binding.btnTopPrismMode.setText(session != null && session.getPrismMode() == ExamSession.PrismMode.POLAR ? "R - θ" : "X - Y");
        binding.btnTopCylinderMode.setText(session != null && session.isCylMinusMode() ? "C -" : "C +");
        binding.btnTopDistanceMode.setText(session != null && session.getDistanceMode() == ExamSession.DistanceMode.NEAR ? "近距" : "远距");
        applyBadge(binding.chipStatusCv, false);
        applyBadge(binding.chipStatusCp, session != null && session.isCpLinked());
        applyBadge(binding.chipStatusLm, session != null && session.getLensDataSource() == ExamSession.LensDataSource.LM);
        applyBadge(binding.chipStatusAr, session != null && session.getLensDataSource() == ExamSession.LensDataSource.AR);
        applyBadge(binding.chipStatusPrinter, false);
    }

    private void renderLeftRail() {
        boolean deviceReady = isMainDeviceReady();
        binding.btnActionIn.setEnabled(deviceReady);
        binding.btnActionPrint.setEnabled(session != null && session.getPatient() != null);
        binding.btnActionIn.setText(session != null && session.isLensInserted() ? "IN 已置入" : "IN");
        binding.btnActionShift.setText(session != null && session.isShiftEnabled() ? "SHIFT 开" : "SHIFT");
        binding.tvLeftStepName.setText(resolveLeftStepLabel());
    }

    private void renderEyeSelection() {
        boolean rightSelected = session != null && session.getActiveEye() == ExamSession.EyeSelection.RIGHT;
        boolean leftSelected = session != null && session.getActiveEye() == ExamSession.EyeSelection.LEFT;
        boolean bothSelected = session != null && session.getActiveEye() == ExamSession.EyeSelection.BOTH;
        int selectedColor = ContextCompat.getColor(requireContext(), R.color.console_blue_bar);
        int normalColor = ContextCompat.getColor(requireContext(), android.R.color.white);
        binding.cardEyeRight.setCardBackgroundColor(rightSelected || bothSelected ? selectedColor : normalColor);
        binding.cardEyeLeft.setCardBackgroundColor(leftSelected || bothSelected ? selectedColor : normalColor);
        binding.tvEyeBoth.setTextColor(ContextCompat.getColor(requireContext(),
                bothSelected ? R.color.console_blue_bar : R.color.console_text_light));
    }

    private void renderMetrics() {
        LensMeasurement rightMeasurement = resolveMeasurement(true);
        LensMeasurement leftMeasurement = resolveMeasurement(false);
        updateMetricLabel(ExamSession.MeasurementField.X, session != null && session.getPrismMode() == ExamSession.PrismMode.POLAR ? "R" : "X");
        updateMetricLabel(ExamSession.MeasurementField.Y, session != null && session.getPrismMode() == ExamSession.PrismMode.POLAR ? "θ" : "Y");
        renderMetricValue(ExamSession.MeasurementField.SPH, rightMeasurement, leftMeasurement);
        renderMetricValue(ExamSession.MeasurementField.CYL, rightMeasurement, leftMeasurement);
        renderMetricValue(ExamSession.MeasurementField.AXIS, rightMeasurement, leftMeasurement);
        renderMetricValue(ExamSession.MeasurementField.ADD, rightMeasurement, leftMeasurement);
        renderMetricValue(ExamSession.MeasurementField.VA, rightMeasurement, leftMeasurement);
        renderMetricValue(ExamSession.MeasurementField.X, rightMeasurement, leftMeasurement);
        renderMetricValue(ExamSession.MeasurementField.Y, rightMeasurement, leftMeasurement);

        ExamSession.MeasurementField selectedField = session == null ? null : session.getSelectedField();
        int active = ContextCompat.getColor(requireContext(), R.color.console_blue_dark);
        int normal = android.graphics.Color.TRANSPARENT;
        for (ExamSession.MeasurementField field : metricRows.keySet()) {
            MetricRowHolder holder = metricRows.get(field);
            if (holder != null) {
                holder.row.setBackgroundColor(field == selectedField ? active : normal);
            }
        }
    }

    private void renderPreviewAndPrograms() {
        renderDataSources();
        VisionChart selectedChart = findSelectedChart();
        if (selectedChart != null) {
            binding.ivChartPreview.setImageResource(selectedChart.getImageResId());
        }
        binding.btnSelectProgram.setText(resolveProgramName());
        binding.tvProgramProgress.setText(resolveProgramProgress());
        binding.tvStepLength.setText("步长 " + resolveCurrentStepText());
    }

    private void renderRightPanel() {
        renderEyeModes();
        renderLensModes();
        binding.btnFarMode.setEnabled(canSwitchDistanceMode(ExamSession.DistanceMode.FAR));
        binding.btnNearMode.setEnabled(canSwitchDistanceMode(ExamSession.DistanceMode.NEAR));
        applyConsoleButton(binding.btnFarMode, session != null && session.getDistanceMode() == ExamSession.DistanceMode.FAR);
        applyConsoleButton(binding.btnNearMode, session != null && session.getDistanceMode() == ExamSession.DistanceMode.NEAR);
        applyConsoleButton(binding.btnLampToggle, session != null && session.getFunctionalTests().isNearLampOn());
        binding.btnLampToggle.setText(session != null && session.getFunctionalTests().isNearLampOn() ? "关灯" : "开灯");
        binding.tvCurrentSubjectName.setText(resolveSubjectName());
        binding.tvCurrentSubjectMeta.setText(resolveSubjectMeta());
        binding.tvStepTitle.setText(resolveStepTitle());
        binding.tvStepMeta.setText(resolveStepMeta());
        binding.tvServerStatus.setText(resolveServerStatus());
        binding.tvBoundDeviceStatus.setText(resolveBoundDeviceStatus());
        renderChartCatalog();
    }

    private void renderChartCatalog() {
        binding.layoutChartCatalog.removeAllViews();
        for (VisionChart chart : chartList) {
            binding.layoutChartCatalog.addView(createChartCatalogItem(chart));
        }
    }

    private void renderDataSources() {
        binding.layoutDataSources.removeAllViews();
        for (ExamSession.LensDataSource source : ExamSession.LensDataSource.values()) {
            binding.layoutDataSources.addView(createInlineChip(
                    resolveLensDataSourceLabel(source),
                    session != null && session.getLensDataSource() == source,
                    v -> clinicViewModel.setLensDataSource(source)
            ));
        }
    }

    private void renderEyeModes() {
        binding.layoutEyeModes.removeAllViews();
        binding.layoutEyeModes.addView(createInlineChip("右眼", session != null && session.getActiveEye() == ExamSession.EyeSelection.RIGHT,
                v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.RIGHT)));
        binding.layoutEyeModes.addView(createInlineChip("左眼", session != null && session.getActiveEye() == ExamSession.EyeSelection.LEFT,
                v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.LEFT)));
        binding.layoutEyeModes.addView(createInlineChip("双眼", session != null && session.getActiveEye() == ExamSession.EyeSelection.BOTH,
                v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.BOTH)));
    }

    private void renderLensModes() {
        binding.layoutLensModes.removeAllViews();
        binding.layoutLensModes.addView(createInlineChip("显右", session != null && session.getLensVisibility() == ExamSession.EyeSelection.RIGHT,
                v -> clinicViewModel.setLensVisibility(ExamSession.EyeSelection.RIGHT)));
        binding.layoutLensModes.addView(createInlineChip("显左", session != null && session.getLensVisibility() == ExamSession.EyeSelection.LEFT,
                v -> clinicViewModel.setLensVisibility(ExamSession.EyeSelection.LEFT)));
        binding.layoutLensModes.addView(createInlineChip("显双", session != null && session.getLensVisibility() == ExamSession.EyeSelection.BOTH,
                v -> clinicViewModel.setLensVisibility(ExamSession.EyeSelection.BOTH)));
    }

    private void renderToolTabs() {
        binding.layoutToolTabs.removeAllViews();
        for (ExamSession.ToolType toolType : ExamSession.ToolType.values()) {
            if (toolType == ExamSession.ToolType.NONE) {
                continue;
            }
            binding.layoutToolTabs.addView(createInlineChip(
                    resolveToolLabel(toolType),
                    session != null && session.getActiveTool() == toolType,
                    v -> clinicViewModel.selectActiveTool(toolType)
            ));
        }
    }

    private void renderToolOverlay() {
        ExamSession.ToolType toolType = session == null || session.getActiveTool() == null
                ? ExamSession.ToolType.NONE : session.getActiveTool();
        if (toolType == ExamSession.ToolType.NONE) {
            binding.cardToolOverlay.setVisibility(View.GONE);
            return;
        }
        binding.cardToolOverlay.setVisibility(View.VISIBLE);
        binding.btnToolExtraPrimary.setVisibility(View.VISIBLE);
        binding.btnToolExtraSecondary.setVisibility(View.VISIBLE);
        binding.tvToolTitle.setText(resolveToolTitle(toolType));
        switch (toolType) {
            case NPC:
                binding.tvToolPrimaryValue.setText(ClinicFormatters.formatUnsigned(session.getFunctionalTests().getNpc()) + " cm");
                binding.tvToolSecondaryValue.setText(ClinicFormatters.formatUnsigned(session.getFunctionalTests().getNpc()) + " MA");
                binding.tvToolTertiaryValue.setText("0 △");
                binding.btnToolExtraPrimary.setVisibility(View.GONE);
                binding.btnToolExtraSecondary.setVisibility(View.GONE);
                break;
            case NPA:
                binding.tvToolPrimaryValue.setText(ClinicFormatters.formatUnsigned(session.getFunctionalTests().getNpa()) + " cm");
                binding.tvToolSecondaryValue.setText("近点");
                binding.tvToolTertiaryValue.setText("调节");
                binding.btnToolExtraPrimary.setVisibility(View.GONE);
                binding.btnToolExtraSecondary.setVisibility(View.GONE);
                break;
            case NRA:
                binding.tvToolPrimaryValue.setText(ClinicFormatters.formatUnsigned(session.getFunctionalTests().getNra()));
                binding.tvToolSecondaryValue.setText(safeNote(session.getFunctionalTests().getNraNote()));
                binding.tvToolTertiaryValue.setText("恢复点待记录");
                binding.btnToolExtraPrimary.setText("模糊");
                binding.btnToolExtraSecondary.setText("恢复");
                break;
            case PRA:
                binding.tvToolPrimaryValue.setText(ClinicFormatters.formatSigned(session.getFunctionalTests().getPra()));
                binding.tvToolSecondaryValue.setText(safeNote(session.getFunctionalTests().getPraNote()));
                binding.tvToolTertiaryValue.setText("恢复点待记录");
                binding.btnToolExtraPrimary.setText("模糊");
                binding.btnToolExtraSecondary.setText("恢复");
                break;
            case ACA:
                binding.tvToolPrimaryValue.setText("BU " + ClinicFormatters.formatUnsigned(session.getFunctionalTests().getAcaBi())
                        + "    " + ClinicFormatters.formatUnsigned(session.getFunctionalTests().getAcaTarget()) + "△");
                binding.tvToolSecondaryValue.setText("首次对齐");
                binding.tvToolTertiaryValue.setText(safeNote(session.getFunctionalTests().getAcaNote()));
                binding.btnToolExtraPrimary.setText("首次对齐");
                binding.btnToolExtraSecondary.setText("再次对齐");
                break;
            case AMP:
                binding.tvToolPrimaryValue.setText(ClinicFormatters.formatUnsigned(session.getFunctionalTests().getAmpRight()) + " CM");
                binding.tvToolSecondaryValue.setText("RAMP");
                binding.tvToolTertiaryValue.setText("LAMP " + ClinicFormatters.formatUnsigned(session.getFunctionalTests().getAmpLeft()) + " CM");
                binding.btnToolExtraPrimary.setText("左-");
                binding.btnToolExtraSecondary.setText("左+");
                break;
            default:
                break;
        }
        binding.tvToolHint.setText(resolveToolHint(toolType));
    }

    private void renderManagedDialogs() {
        if (subjectDialog != null && subjectDialog.isShowing()) {
            renderSubjectDialog();
        }
        if (deviceDialog != null && deviceDialog.isShowing()) {
            renderDeviceDialog();
        }
        if (programDialog != null && programDialog.isShowing()) {
            renderProgramDialog();
        }
        if (programDetailDialog != null && programDetailDialog.isShowing()) {
            renderProgramDetailDialog();
        }
        if (stepLengthDialog != null && stepLengthDialog.isShowing()) {
            renderStepLengthDialog();
        }
        if (timeLanguageDialog != null && timeLanguageDialog.isShowing()) {
            renderTimeLanguageDialog();
        }
    }

    private void handleShiftButton(boolean increase) {
        if (session != null && session.getActiveTool() != null && session.getActiveTool() != ExamSession.ToolType.NONE) {
            handleToolAdjust(increase);
            return;
        }
        clinicViewModel.adjustMeasurement(increase, true);
    }

    private void clearCurrentSelection() {
        if (session != null && session.getActiveTool() != null && session.getActiveTool() != ExamSession.ToolType.NONE) {
            clinicViewModel.selectActiveTool(ExamSession.ToolType.NONE);
            return;
        }
        showToast("当前版本先保留清除入口，字段清空会在下一轮补齐");
    }

    private void toggleShift() {
        clinicViewModel.toggleShiftEnabled();
    }

    private void toggleDistanceMode() {
        if (session == null) {
            return;
        }
        changeDistanceMode(session.getDistanceMode() == ExamSession.DistanceMode.FAR
                ? ExamSession.DistanceMode.NEAR : ExamSession.DistanceMode.FAR);
    }

    private void changeDistanceMode(ExamSession.DistanceMode targetMode) {
        if (session == null || session.getDistanceMode() == targetMode) {
            return;
        }
        if (!canSwitchDistanceMode(targetMode)) {
            showToast("当前步骤固定模式");
            return;
        }
        clinicViewModel.toggleDistanceMode();
    }

    private boolean canSwitchDistanceMode(ExamSession.DistanceMode targetMode) {
        if (currentStep == null || currentStep.getDistanceMode() == null || currentStep.getDistanceMode() == ExamStep.DistanceMode.BOTH) {
            return true;
        }
        return targetMode == ExamSession.DistanceMode.FAR && currentStep.getDistanceMode() == ExamStep.DistanceMode.FAR
                || targetMode == ExamSession.DistanceMode.NEAR && currentStep.getDistanceMode() == ExamStep.DistanceMode.NEAR;
    }

    private void handleLensInserted() {
        if (!isMainDeviceReady()) {
            showToast("请先启动服务并绑定在线主设备");
            return;
        }
        clinicViewModel.toggleLensInserted();
        showToast(session != null && !session.isLensInserted() ? "已取消镜片置入" : "已标记镜片置入");
    }

    private void toggleLamp() {
        if (!isMainDeviceReady()) {
            showToast("请先启动服务并绑定在线主设备");
            return;
        }
        clinicViewModel.toggleNearLamp();
    }

    private void handlePrint() {
        if (session == null || session.getPatient() == null) {
            showToast("请先绑定被测者");
            return;
        }
        if (session.isUnsavedChanges()) {
            clinicViewModel.saveCurrentReport();
        }
        showToast("打印预览和打印机联动将在下一轮补齐");
    }

    private void saveCurrentReport() {
        if (session == null || session.getPatient() == null) {
            showToast("请先绑定被测者");
            return;
        }
        clinicViewModel.saveCurrentReport();
        showToast("当前草稿已保存");
    }

    private void handleToolAdjust(boolean increase) {
        ExamSession.ToolType toolType = session == null || session.getActiveTool() == null
                ? ExamSession.ToolType.NONE : session.getActiveTool();
        switch (toolType) {
            case NPC:
                clinicViewModel.adjustFunctionalValue(FUNCTION_NPC, increase);
                break;
            case NPA:
                clinicViewModel.adjustFunctionalValue(FUNCTION_NPA, increase);
                break;
            case NRA:
                clinicViewModel.adjustFunctionalValue(FUNCTION_NRA, increase);
                break;
            case PRA:
                clinicViewModel.adjustFunctionalValue(FUNCTION_PRA, increase);
                break;
            case ACA:
                clinicViewModel.adjustFunctionalValue(FUNCTION_ACA_BI, increase);
                break;
            case AMP:
                clinicViewModel.adjustFunctionalValue(FUNCTION_AMP_RIGHT, increase);
                break;
            default:
                break;
        }
    }

    private void handleToolExtraPrimary() {
        ExamSession.ToolType toolType = session == null || session.getActiveTool() == null
                ? ExamSession.ToolType.NONE : session.getActiveTool();
        switch (toolType) {
            case NRA:
                clinicViewModel.markFunctionEvent(FUNCTION_NRA, "模糊");
                break;
            case PRA:
                clinicViewModel.markFunctionEvent(FUNCTION_PRA, "模糊");
                break;
            case ACA:
                clinicViewModel.markFunctionEvent(FUNCTION_ACA, "首次对齐");
                break;
            case AMP:
                clinicViewModel.adjustFunctionalValue(FUNCTION_AMP_LEFT, false);
                break;
            default:
                break;
        }
    }

    private void handleToolExtraSecondary() {
        ExamSession.ToolType toolType = session == null || session.getActiveTool() == null
                ? ExamSession.ToolType.NONE : session.getActiveTool();
        switch (toolType) {
            case NRA:
                clinicViewModel.markFunctionEvent(FUNCTION_NRA, "恢复");
                break;
            case PRA:
                clinicViewModel.markFunctionEvent(FUNCTION_PRA, "恢复");
                break;
            case ACA:
                clinicViewModel.markFunctionEvent(FUNCTION_ACA, "再次对齐");
                break;
            case AMP:
                clinicViewModel.adjustFunctionalValue(FUNCTION_AMP_LEFT, true);
                break;
            default:
                break;
        }
    }

    private void showChartHelp() {
        VisionChart chart = findSelectedChart();
        if (chart == null) {
            showToast("请先选择视标");
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(chart.getTitle())
                .setMessage(chart.getSubtitle() + "\n\n" + chart.getDescription())
                .setPositiveButton("关闭", null)
                .show();
    }

    private void showPasswordDialog() {
        if (passwordDialog != null && passwordDialog.isShowing()) {
            return;
        }
        passwordDialogBinding = DialogPasswordGateBinding.inflate(getLayoutInflater());
        passwordDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(passwordDialogBinding.getRoot())
                .create();
        passwordDialogBinding.btnPasswordCancel.setOnClickListener(v -> dismissManagedDialog(passwordDialog));
        passwordDialogBinding.btnPasswordConfirm.setOnClickListener(v -> {
            String password = passwordDialogBinding.etPasswordInput.getText() == null
                    ? "" : passwordDialogBinding.etPasswordInput.getText().toString().trim();
            if (TextUtils.isEmpty(password)) {
                showToast("请输入密码");
                return;
            }
            dismissManagedDialog(passwordDialog);
            showMenuDialog();
        });
        showManagedDialog(passwordDialog, 0.42f);
    }

    private void showMenuDialog() {
        if (menuDialog != null && menuDialog.isShowing()) {
            return;
        }
        menuDialogBinding = DialogMenuPanelBinding.inflate(getLayoutInflater());
        menuDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(menuDialogBinding.getRoot())
                .create();
        menuDialogBinding.btnMenuCharts.setOnClickListener(v -> showToast("图表设置将在下一轮补齐"));
        menuDialogBinding.btnMenuDeviceConnection.setOnClickListener(v -> {
            dismissManagedDialog(menuDialog);
            showDeviceDialog();
        });
        menuDialogBinding.btnMenuProgram.setOnClickListener(v -> {
            dismissManagedDialog(menuDialog);
            showProgramDialog();
        });
        menuDialogBinding.btnMenuTimeLanguage.setOnClickListener(v -> {
            dismissManagedDialog(menuDialog);
            showTimeLanguageDialog();
        });
        menuDialogBinding.btnMenuClose.setOnClickListener(v -> dismissManagedDialog(menuDialog));
        showManagedDialog(menuDialog, 0.56f);
    }

    private void showSubjectDialog() {
        if (subjectDialog != null && subjectDialog.isShowing()) {
            return;
        }
        subjectDialogBinding = DialogSubjectManageBinding.inflate(getLayoutInflater());
        subjectDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(subjectDialogBinding.getRoot())
                .create();
        subjectCreateMode = true;
        subjectFormInitialized = false;
        bindSubjectDialogActions();
        populateSubjectForm(session == null ? null : session.getPatient());
        renderSubjectDialog();
        showManagedDialog(subjectDialog, 0.76f);
    }

    private void bindSubjectDialogActions() {
        if (subjectDialogBinding == null) {
            return;
        }
        subjectDialogBinding.btnCloseSubjectDialog.setOnClickListener(v -> dismissManagedDialog(subjectDialog));
        subjectDialogBinding.btnSubjectTabCreate.setOnClickListener(v -> {
            subjectCreateMode = true;
            renderSubjectDialog();
        });
        subjectDialogBinding.btnSubjectTabSearch.setOnClickListener(v -> {
            subjectCreateMode = false;
            renderSubjectDialog();
        });
        subjectDialogBinding.btnSubjectImportCode.setOnClickListener(v -> promptSubjectImport());
        subjectDialogBinding.btnSubjectSave.setOnClickListener(v -> saveSubjectFromDialog());
        subjectDialogBinding.etSubjectSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderSubjectDialog();
            }
        });
    }

    private void renderSubjectDialog() {
        if (subjectDialogBinding == null) {
            return;
        }
        subjectDialogBinding.layoutSubjectCreate.setVisibility(subjectCreateMode ? View.VISIBLE : View.GONE);
        subjectDialogBinding.layoutSubjectSearch.setVisibility(subjectCreateMode ? View.GONE : View.VISIBLE);
        applyDialogTab(subjectDialogBinding.btnSubjectTabCreate, subjectCreateMode);
        applyDialogTab(subjectDialogBinding.btnSubjectTabSearch, !subjectCreateMode);
        if (!subjectFormInitialized) {
            populateSubjectForm(session == null ? null : session.getPatient());
        }
        renderSubjectSearchResults();
    }

    private void populateSubjectForm(@Nullable PatientProfile patient) {
        if (subjectDialogBinding == null) {
            return;
        }
        subjectDialogBinding.etSubjectName.setText(patient == null ? "" : patient.getName());
        subjectDialogBinding.etSubjectPhone.setText(patient == null ? "" : patient.getPhone());
        subjectDialogBinding.etSubjectBirthDate.setText(patient == null ? "" : patient.getBirthDate());
        subjectDialogBinding.etSubjectAddress.setText(patient == null ? "" : patient.getAddress());
        subjectDialogBinding.etSubjectRemark.setText(patient == null ? "" : patient.getNote());
        checkGender(patient == null ? "" : patient.getGender());
        subjectFormInitialized = true;
    }

    private void checkGender(String gender) {
        if (subjectDialogBinding == null) {
            return;
        }
        if ("男".equals(gender)) {
            subjectDialogBinding.groupSubjectGender.check(subjectDialogBinding.rbGenderMale.getId());
        } else if ("女".equals(gender)) {
            subjectDialogBinding.groupSubjectGender.check(subjectDialogBinding.rbGenderFemale.getId());
        } else {
            subjectDialogBinding.groupSubjectGender.check(subjectDialogBinding.rbGenderUnknown.getId());
        }
    }

    private void renderSubjectSearchResults() {
        if (subjectDialogBinding == null) {
            return;
        }
        String query = readText(subjectDialogBinding.etSubjectSearch).toLowerCase(Locale.getDefault());
        subjectDialogBinding.layoutSubjectSearchResults.removeAllViews();
        List<PatientProfile> filtered = new ArrayList<>();
        for (PatientProfile patient : patientList) {
            if (TextUtils.isEmpty(query)
                    || patient.getDisplayName().toLowerCase(Locale.getDefault()).contains(query)
                    || (!TextUtils.isEmpty(patient.getPhone()) && patient.getPhone().contains(query))) {
                filtered.add(patient);
            }
        }
        if (filtered.isEmpty()) {
            subjectDialogBinding.layoutSubjectSearchResults.addView(createHintText("没有匹配到被测者"));
            return;
        }
        for (PatientProfile patient : filtered) {
            subjectDialogBinding.layoutSubjectSearchResults.addView(createPatientResultCard(patient));
        }
    }

    private MaterialCardView createPatientResultCard(PatientProfile patient) {
        MaterialCardView card = createDialogCard();
        LinearLayout content = createDialogCardContent(card);
        content.addView(createDialogTitle(patient.getDisplayName()));
        content.addView(createDialogBody(patient.getPhone() + " | " + safeText(patient.getGender()) + " | " + safeText(patient.getBirthDate())));
        MaterialButton button = createDialogButton("绑定被测者", true);
        button.setOnClickListener(v -> confirmSwitchSubject(patient));
        content.addView(button);
        return card;
    }

    private void confirmSwitchSubject(PatientProfile patient) {
        if (session != null && session.isUnsavedChanges()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("切换被测者")
                    .setMessage("当前存在未保存草稿，确认切换？")
                    .setPositiveButton("继续", (dialog, which) -> selectPatientAndClose(patient.getId()))
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        selectPatientAndClose(patient.getId());
    }

    private void selectPatientAndClose(String patientId) {
        clinicViewModel.selectPatient(patientId);
        dismissManagedDialog(subjectDialog);
        showToast("已绑定被测者");
    }

    private void promptSubjectImport() {
        EditText input = new EditText(requireContext());
        input.setHint("姓名|电话|性别|出生日期|地址|备注");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("扫码导入")
                .setView(input)
                .setPositiveButton("导入", (dialog, which) -> {
                    clinicViewModel.importPatientFromCode(readText(input));
                    dismissManagedDialog(subjectDialog);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveSubjectFromDialog() {
        if (subjectDialogBinding == null) {
            return;
        }
        PatientProfile profile = session != null && session.getPatient() != null
                ? session.getPatient().copy() : new PatientProfile();
        profile.setName(readText(subjectDialogBinding.etSubjectName));
        profile.setPhone(readText(subjectDialogBinding.etSubjectPhone));
        profile.setGender(resolveCheckedGender());
        profile.setBirthDate(readText(subjectDialogBinding.etSubjectBirthDate));
        profile.setAddress(readText(subjectDialogBinding.etSubjectAddress));
        profile.setNote(readText(subjectDialogBinding.etSubjectRemark));
        if (TextUtils.isEmpty(profile.getName())) {
            showToast("请输入姓名");
            return;
        }
        clinicViewModel.savePatient(profile);
        dismissManagedDialog(subjectDialog);
        showToast("已保存并绑定被测者");
    }

    private String resolveCheckedGender() {
        if (subjectDialogBinding == null) {
            return "未知";
        }
        int checkedId = subjectDialogBinding.groupSubjectGender.getCheckedRadioButtonId();
        RadioButton radioButton = subjectDialogBinding.getRoot().findViewById(checkedId);
        return radioButton == null ? "未知" : String.valueOf(radioButton.getText());
    }

    private void showDeviceDialog() {
        if (deviceDialog != null && deviceDialog.isShowing()) {
            return;
        }
        deviceDialogBinding = DialogDeviceBindBinding.inflate(getLayoutInflater());
        deviceDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(deviceDialogBinding.getRoot())
                .create();
        bindDeviceDialogActions();
        renderDeviceDialog();
        showManagedDialog(deviceDialog, 0.84f);
    }

    private void bindDeviceDialogActions() {
        if (deviceDialogBinding == null) {
            return;
        }
        deviceDialogBinding.btnCloseDeviceDialog.setOnClickListener(v -> dismissManagedDialog(deviceDialog));
        deviceDialogBinding.btnToggleDeviceService.setOnClickListener(v -> {
            if (deviceUiState != null && deviceUiState.isServerRunning()) {
                clinicViewModel.stopServer();
            } else {
                clinicViewModel.startServer();
            }
        });
        deviceDialogBinding.btnRefreshDeviceState.setOnClickListener(v -> clinicViewModel.refreshDeviceState());
        deviceDialogBinding.btnQueryBoundDevice.setOnClickListener(v -> clinicViewModel.queryBoundMainDeviceInfo());
        deviceDialogBinding.btnUnbindDevice.setOnClickListener(v -> clinicViewModel.unbindMainDevice());
    }

    private void renderDeviceDialog() {
        if (deviceDialogBinding == null) {
            return;
        }
        boolean running = deviceUiState != null && deviceUiState.isServerRunning();
        deviceDialogBinding.tvDeviceServiceState.setText(running ? "服务状态: 运行中" : "服务状态: 已停止");
        deviceDialogBinding.tvDeviceServiceEndpoint.setText("监听地址: " + safeText(deviceUiState == null ? null : deviceUiState.getLocalIp())
                + ":" + (deviceUiState == null ? 0 : deviceUiState.getServerPort()));
        deviceDialogBinding.btnToggleDeviceService.setText(running ? "停止服务" : "启动服务");
        deviceDialogBinding.btnQueryBoundDevice.setEnabled(running && deviceUiState != null
                && !TextUtils.isEmpty(deviceUiState.getBoundDeviceClientId()));
        deviceDialogBinding.btnUnbindDevice.setEnabled(deviceUiState != null
                && !TextUtils.isEmpty(deviceUiState.getBoundDeviceLabel()));
        deviceDialogBinding.tvCurrentBoundDevice.setText(resolveBoundDeviceStatus());
        deviceDialogBinding.layoutOnlineDevices.removeAllViews();
        List<ConnectedDeviceInfo> onlineDevices = deviceUiState == null ? new ArrayList<>() : deviceUiState.getConnectedDevices();
        if (onlineDevices.isEmpty()) {
            deviceDialogBinding.layoutOnlineDevices.addView(createHintText("当前没有在线设备"));
        } else {
            for (ConnectedDeviceInfo device : onlineDevices) {
                deviceDialogBinding.layoutOnlineDevices.addView(createOnlineDeviceCard(device));
            }
        }
        deviceDialogBinding.layoutKnownDevices.removeAllViews();
        List<KnownDeviceSummary> knownDevices = deviceUiState == null ? new ArrayList<>() : deviceUiState.getKnownDevices();
        if (knownDevices.isEmpty()) {
            deviceDialogBinding.layoutKnownDevices.addView(createHintText("暂无历史设备记录"));
        } else {
            for (KnownDeviceSummary device : knownDevices) {
                deviceDialogBinding.layoutKnownDevices.addView(createKnownDeviceCard(device));
            }
        }
        deviceDialogBinding.tvDeviceLogs.setText(resolveDeviceLogOutput());
    }

    private MaterialCardView createOnlineDeviceCard(ConnectedDeviceInfo device) {
        MaterialCardView card = createDialogCard();
        LinearLayout content = createDialogCardContent(card);
        content.addView(createDialogTitle(device.getDisplayLabel()));
        content.addView(createDialogBody("MAC: " + safeText(device.getMacAddress())
                + "\nIP: " + safeText(device.getIpAddress()) + ":" + device.getPort()));
        boolean selected = deviceUiState != null
                && TextUtils.equals(device.getClientId(), deviceUiState.getBoundDeviceClientId());
        MaterialButton button = createDialogButton(selected ? "当前主设备" : "绑定为主设备", true);
        button.setEnabled(!selected);
        button.setOnClickListener(v -> {
            clinicViewModel.bindMainDevice(device.getClientId());
            clinicViewModel.queryBoundMainDeviceInfo();
            showToast("主设备绑定指令已发送");
        });
        content.addView(button);
        return card;
    }

    private MaterialCardView createKnownDeviceCard(KnownDeviceSummary device) {
        MaterialCardView card = createDialogCard();
        LinearLayout content = createDialogCardContent(card);
        content.addView(createDialogTitle(device.getDisplayLabel()));
        content.addView(createDialogBody("最近 IP: " + safeText(device.getLastKnownIp())
                + "\n通信 " + device.getCommunicationCount() + " 条 / 连接 " + device.getConnectionCount() + " 次"));
        return card;
    }

    private void showProgramDialog() {
        if (programDialog != null && programDialog.isShowing()) {
            return;
        }
        programDialogBinding = DialogProgramPickerBinding.inflate(getLayoutInflater());
        programDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(programDialogBinding.getRoot())
                .create();
        programDialogBinding.btnCreateProgramDialog.setOnClickListener(v -> showToast("新增程序将在后续版本开放"));
        programDialogBinding.btnCloseProgramDialog.setOnClickListener(v -> dismissManagedDialog(programDialog));
        renderProgramDialog();
        showManagedDialog(programDialog, 0.80f);
    }

    private void renderProgramDialog() {
        if (programDialogBinding == null) {
            return;
        }
        programDialogBinding.layoutProgramOptions.removeAllViews();
        if (programList.isEmpty()) {
            programDialogBinding.layoutProgramOptions.addView(createHintText("当前没有可用程序"));
            return;
        }
        for (ExamProgram program : programList) {
            programDialogBinding.layoutProgramOptions.addView(createProgramOptionCard(program));
        }
    }

    private MaterialCardView createProgramOptionCard(ExamProgram program) {
        MaterialCardView card = createDialogCard();
        boolean selected = session != null && TextUtils.equals(program.getId(), session.getCurrentProgramId());
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.console_blue_light : android.R.color.white));
        card.setStrokeColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.console_blue_dark : R.color.console_border));
        card.setRadius(dp(8));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.addView(content);

        LinearLayout textLayout = new LinearLayout(requireContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textLayout.setLayoutParams(textParams);
        content.addView(textLayout);

        TextView title = createText(requireContext(), program.getTitle(), 24,
                ContextCompat.getColor(requireContext(), R.color.console_text_dark), true);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        textLayout.addView(title);

        TextView summary = createText(requireContext(),
                program.getSummary() + " · " + program.getSteps().size() + " 步",
                15,
                ContextCompat.getColor(requireContext(),
                        selected ? R.color.console_blue_dark : R.color.console_text_soft),
                false);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        summaryParams.topMargin = dp(6);
        summary.setLayoutParams(summaryParams);
        summary.setGravity(Gravity.CENTER_HORIZONTAL);
        textLayout.addView(summary);

        if (selected) {
            TextView currentBadge = createText(requireContext(), "当前程序", 14,
                    ContextCompat.getColor(requireContext(), android.R.color.white), true);
            currentBadge.setBackgroundResource(R.drawable.bg_workbench_status_badge_online);
            currentBadge.setPadding(dp(10), dp(6), dp(10), dp(6));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            badgeParams.topMargin = dp(10);
            badgeParams.gravity = Gravity.CENTER_HORIZONTAL;
            currentBadge.setLayoutParams(badgeParams);
            textLayout.addView(currentBadge);
        }

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(actions);

        actions.addView(createProgramActionButton(
                R.drawable.ic_program_action_detail,
                "查看步骤",
                v -> showProgramDetailDialog(program)
        ));
        ImageButton selectButton = createProgramActionButton(
                R.drawable.ic_program_action_preview,
                selected ? "当前程序" : "设为当前程序",
                v -> {
                    if (selected) {
                        showToast("当前已在使用该程序");
                    } else {
                        confirmSwitchProgram(program.getId());
                    }
                }
        );
        LinearLayout.LayoutParams selectParams = (LinearLayout.LayoutParams) selectButton.getLayoutParams();
        selectParams.leftMargin = dp(12);
        selectButton.setLayoutParams(selectParams);
        if (selected) {
            selectButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.workbench_accent));
        }
        actions.addView(selectButton);
        return card;
    }

    private ImageButton createProgramActionButton(int iconRes, String description, View.OnClickListener listener) {
        ImageButton button = new ImageButton(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(54), dp(54));
        button.setLayoutParams(params);
        button.setBackgroundResource(R.drawable.bg_program_action_icon);
        button.setImageResource(iconRes);
        button.setColorFilter(ContextCompat.getColor(requireContext(), R.color.console_text_dark));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        return button;
    }

    private void showProgramDetailDialog(@Nullable ExamProgram program) {
        if (program == null) {
            return;
        }
        detailProgram = program;
        detailProgramStepIndex = resolveInitialProgramDetailStepIndex(program);
        if (programDetailDialog != null && programDetailDialog.isShowing()) {
            renderProgramDetailDialog();
            return;
        }
        programDetailDialogBinding = DialogProgramDetailBinding.inflate(getLayoutInflater());
        programDetailDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(programDetailDialogBinding.getRoot())
                .create();
        programDetailDialogBinding.btnProgramDetailClose.setOnClickListener(v -> dismissManagedDialog(programDetailDialog));
        programDetailDialogBinding.btnProgramDetailUse.setOnClickListener(v -> {
            if (detailProgram != null) {
                confirmSwitchProgram(detailProgram.getId());
            }
        });
        renderProgramDetailDialog();
        showManagedDialog(programDetailDialog, 0.92f);
    }

    private int resolveInitialProgramDetailStepIndex(@NonNull ExamProgram program) {
        if (session != null
                && TextUtils.equals(program.getId(), session.getCurrentProgramId())
                && session.getCurrentStepIndex() >= 0
                && session.getCurrentStepIndex() < program.getSteps().size()) {
            return session.getCurrentStepIndex();
        }
        return 0;
    }

    private void renderProgramDetailDialog() {
        if (programDetailDialogBinding == null || detailProgram == null) {
            return;
        }
        boolean currentProgram = session != null && TextUtils.equals(detailProgram.getId(), session.getCurrentProgramId());
        programDetailDialogBinding.tvProgramDetailTitle.setText(detailProgram.getTitle());
        programDetailDialogBinding.tvProgramDetailSummary.setText(detailProgram.getDescription());
        programDetailDialogBinding.tvProgramDetailStepCount.setText("步骤数: " + detailProgram.getSteps().size());
        programDetailDialogBinding.tvProgramDetailDefaultRule.setText(resolveProgramDefaultRule(detailProgram));
        programDetailDialogBinding.tvProgramDetailCurrentBadge.setText(currentProgram ? "当前程序执行中" : "未启用");
        programDetailDialogBinding.btnProgramDetailUse.setEnabled(!currentProgram);
        programDetailDialogBinding.btnProgramDetailUse.setText(currentProgram ? "当前程序" : "设为当前程序");

        programDetailDialogBinding.layoutProgramDetailSteps.removeAllViews();
        List<ExamStep> steps = detailProgram.getSteps();
        if (steps.isEmpty()) {
            programDetailDialogBinding.layoutProgramDetailSteps.addView(createHintText("当前程序尚未配置步骤"));
            programDetailDialogBinding.ivProgramDetailChart.setImageDrawable(null);
            programDetailDialogBinding.tvProgramDetailStepName.setText("未配置");
            programDetailDialogBinding.tvProgramDetailStepDescription.setText("请先补充流程步骤。");
            programDetailDialogBinding.layoutProgramDetailMeta.removeAllViews();
            programDetailDialogBinding.tvProgramDetailSkipRule.setText("跳过该步骤条件: 无");
            programDetailDialogBinding.tvProgramDetailStepNote.setText("未记录");
            return;
        }
        detailProgramStepIndex = Math.max(0, Math.min(detailProgramStepIndex, steps.size() - 1));
        for (int index = 0; index < steps.size(); index++) {
            programDetailDialogBinding.layoutProgramDetailSteps.addView(createProgramStepCard(detailProgram, index, steps.get(index)));
        }
        renderActiveProgramStep(steps.get(detailProgramStepIndex));
    }

    private MaterialCardView createProgramStepCard(ExamProgram program, int index, ExamStep step) {
        boolean selected = index == detailProgramStepIndex;
        MaterialCardView card = createDialogCard();
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.console_blue_light : android.R.color.white));
        card.setStrokeColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.console_blue_dark : R.color.console_border));
        card.setStrokeWidth(dp(selected ? 2 : 1));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(content);

        TextView stepNo = createText(requireContext(), String.valueOf(index + 1), 22,
                ContextCompat.getColor(requireContext(), R.color.console_text_dark), true);
        LinearLayout.LayoutParams stepNoParams = new LinearLayout.LayoutParams(dp(36),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        stepNo.setLayoutParams(stepNoParams);
        stepNo.setGravity(Gravity.CENTER);
        content.addView(stepNo);

        VisionChart chart = findChartById(step.getChartId());
        ImageView preview = new ImageView(requireContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(68), dp(68));
        previewParams.leftMargin = dp(10);
        preview.setLayoutParams(previewParams);
        preview.setBackgroundResource(R.drawable.bg_program_detail_preview_panel);
        preview.setPadding(dp(8), dp(8), dp(8), dp(8));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (chart != null) {
            preview.setImageResource(chart.getImageResId());
        }
        content.addView(preview);

        LinearLayout textLayout = new LinearLayout(requireContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textParams.leftMargin = dp(12);
        textLayout.setLayoutParams(textParams);
        content.addView(textLayout);

        TextView title = createText(requireContext(), step.getTitle(), 18,
                ContextCompat.getColor(requireContext(), R.color.console_text_dark), true);
        textLayout.addView(title);

        TextView meta = createText(requireContext(),
                resolveStepDistanceLabel(step.getDistanceMode()) + " / "
                        + resolveStepEyeScopeLabel(step.getEyeScope()) + " / "
                        + safeText(step.getTargetField()),
                14,
                ContextCompat.getColor(requireContext(), R.color.console_text_soft),
                false);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(4);
        meta.setLayoutParams(metaParams);
        textLayout.addView(meta);

        card.setOnClickListener(v -> {
            detailProgram = program;
            detailProgramStepIndex = index;
            renderProgramDetailDialog();
        });
        return card;
    }

    private void renderActiveProgramStep(@NonNull ExamStep step) {
        if (programDetailDialogBinding == null) {
            return;
        }
        VisionChart chart = findChartById(step.getChartId());
        if (chart != null) {
            programDetailDialogBinding.ivProgramDetailChart.setImageResource(chart.getImageResId());
        } else {
            programDetailDialogBinding.ivProgramDetailChart.setImageDrawable(null);
        }
        programDetailDialogBinding.tvProgramDetailStepName.setText(step.getTitle());
        programDetailDialogBinding.tvProgramDetailStepDescription.setText(step.getDescription());
        programDetailDialogBinding.tvProgramDetailSkipRule.setText(resolveStepSkipRule(step));
        programDetailDialogBinding.tvProgramDetailStepNote.setText(
                TextUtils.isEmpty(step.getNote()) ? safeText(step.getDescription()) : step.getNote());

        programDetailDialogBinding.layoutProgramDetailMeta.removeAllViews();
        programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                "视力表: " + (chart == null ? "--" : chart.getTitle())));
        programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                "远近模式: " + resolveStepDistanceLabel(step.getDistanceMode())));
        programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                "目标眼别: " + resolveStepEyeScopeLabel(step.getEyeScope())));
        programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                "数据来源: " + safeText(step.getSubjectSource()) + " / 字段 " + safeText(step.getTargetField())));
        if (!TextUtils.isEmpty(step.getFogOption())) {
            programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                    "雾视设定: " + step.getFogOption()));
        }
        if (!TextUtils.isEmpty(step.getNearLightOption())) {
            programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                    "视近灯: " + step.getNearLightOption()));
        }
        if (!TextUtils.isEmpty(step.getFunctionLabel())) {
            programDetailDialogBinding.layoutProgramDetailMeta.addView(createProgramMetaText(
                    "视功能: " + step.getFunctionLabel()));
        }
    }

    private TextView createProgramMetaText(String text) {
        TextView metaText = createText(requireContext(), text, 16,
                ContextCompat.getColor(requireContext(), R.color.console_text_dark), false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        metaText.setLayoutParams(params);
        return metaText;
    }

    private String resolveProgramDefaultRule(@NonNull ExamProgram program) {
        if (program.getSteps().isEmpty()) {
            return "未配置默认规则";
        }
        ExamStep firstStep = program.getSteps().get(0);
        return "默认: " + resolveStepDistanceLabel(firstStep.getDistanceMode())
                + " / " + resolveStepEyeScopeLabel(firstStep.getEyeScope());
    }

    private String resolveStepSkipRule(@NonNull ExamStep step) {
        if (TextUtils.isEmpty(step.getSkipField()) || step.getSkipComparator() == ExamStep.Comparator.NONE) {
            return "跳过该步骤条件: 无";
        }
        return "跳过该步骤条件: " + step.getSkipField() + " "
                + resolveComparatorLabel(step.getSkipComparator()) + " "
                + formatStepValue(step.getSkipThreshold());
    }

    private String resolveComparatorLabel(@Nullable ExamStep.Comparator comparator) {
        if (comparator == null) {
            return "--";
        }
        switch (comparator) {
            case EQ:
                return "=";
            case GT:
                return ">";
            case GTE:
                return ">=";
            case LT:
                return "<";
            case LTE:
                return "<=";
            case NONE:
            default:
                return "无";
        }
    }

    private String resolveStepDistanceLabel(@Nullable ExamStep.DistanceMode mode) {
        if (mode == null) {
            return "--";
        }
        switch (mode) {
            case FAR:
                return "远距";
            case NEAR:
                return "近距";
            case BOTH:
            default:
                return "远/近";
        }
    }

    private String resolveStepEyeScopeLabel(@Nullable ExamStep.EyeScope eyeScope) {
        if (eyeScope == null) {
            return "--";
        }
        switch (eyeScope) {
            case RIGHT:
                return "R";
            case LEFT:
                return "L";
            case BOTH:
            default:
                return "B";
        }
    }

    @Nullable
    private VisionChart findChartById(@Nullable String chartId) {
        if (TextUtils.isEmpty(chartId)) {
            return null;
        }
        for (VisionChart chart : chartList) {
            if (TextUtils.equals(chart.getId(), chartId)) {
                return chart;
            }
        }
        return null;
    }

    private void confirmSwitchProgram(String programId) {
        if (session != null && session.isUnsavedChanges()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("切换程序")
                    .setMessage("当前存在未保存草稿，确认切换？")
                    .setPositiveButton("继续", (dialog, which) -> selectProgramAndClose(programId))
                    .setNegativeButton("取消", null)
                    .show();
            return;
        }
        selectProgramAndClose(programId);
    }

    private void selectProgramAndClose(String programId) {
        clinicViewModel.selectProgram(programId);
        dismissManagedDialog(programDetailDialog);
        dismissManagedDialog(programDialog);
    }

    private void showTimeLanguageDialog() {
        if (timeLanguageDialog != null && timeLanguageDialog.isShowing()) {
            return;
        }
        timeLanguageDialogBinding = DialogTimeLanguageBinding.inflate(getLayoutInflater());
        timeLanguageDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(timeLanguageDialogBinding.getRoot())
                .create();
        draftTimeLanguageSettings = buildTimeLanguageDraft(settings);
        bindTimeLanguageDialogActions();
        renderTimeLanguageDialog();
        showManagedDialog(timeLanguageDialog, 0.88f);
    }

    private void bindTimeLanguageDialogActions() {
        if (timeLanguageDialogBinding == null) {
            return;
        }
        setupTimeLanguageSpinner(timeLanguageDialogBinding.spinnerLanguage, LANGUAGE_OPTIONS,
                value -> ensureDraftTimeLanguageSettings().setLanguage(value));
        setupTimeLanguageSpinner(timeLanguageDialogBinding.spinnerDurationVisibility, DISPLAY_DURATION_OPTIONS,
                value -> ensureDraftTimeLanguageSettings().setShowDisplayDuration(TextUtils.equals(value, "显示")));

        bindTimeAdjustRow(timeLanguageDialogBinding.layoutDateYearRow, "年", Calendar.YEAR);
        bindTimeAdjustRow(timeLanguageDialogBinding.layoutDateMonthRow, "月", Calendar.MONTH);
        bindTimeAdjustRow(timeLanguageDialogBinding.layoutDateDayRow, "日", Calendar.DAY_OF_MONTH);
        bindTimeAdjustRow(timeLanguageDialogBinding.layoutTimeHourRow, "时", Calendar.HOUR_OF_DAY);
        bindTimeAdjustRow(timeLanguageDialogBinding.layoutTimeMinuteRow, "分", Calendar.MINUTE);
        bindTimeAdjustRow(timeLanguageDialogBinding.layoutTimeSecondRow, "秒", Calendar.SECOND);

        timeLanguageDialogBinding.btnResetTimeLanguage.setOnClickListener(v -> {
            draftTimeLanguageSettings = buildTimeLanguageDraft(null);
            renderTimeLanguageDialog();
        });
        timeLanguageDialogBinding.btnConfirmTimeLanguage.setOnClickListener(v -> saveTimeLanguageSettings());
        timeLanguageDialogBinding.btnCancelTimeLanguage.setOnClickListener(v -> dismissManagedDialog(timeLanguageDialog));
    }

    private void setupTimeLanguageSpinner(Spinner spinner, String[] options, TimeLanguageSelectionListener listener) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.item_time_setting_spinner_selected,
                options
        );
        adapter.setDropDownViewResource(R.layout.item_time_setting_spinner_dropdown);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onItemSelected(options[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void bindTimeAdjustRow(ViewTimeLanguageAdjustRowBinding rowBinding, String label, int calendarField) {
        rowBinding.tvAdjustLabel.setText(label);
        rowBinding.btnAdjustUp.setOnClickListener(v -> adjustTimeLanguageValue(calendarField, true));
        rowBinding.btnAdjustDown.setOnClickListener(v -> adjustTimeLanguageValue(calendarField, false));
    }

    private void renderTimeLanguageDialog() {
        if (timeLanguageDialogBinding == null) {
            return;
        }
        ClinicSettings draft = ensureDraftTimeLanguageSettings();
        updateSpinnerSelection(timeLanguageDialogBinding.spinnerLanguage, draft.getLanguage(), LANGUAGE_OPTIONS);
        updateSpinnerSelection(timeLanguageDialogBinding.spinnerDurationVisibility,
                draft.isShowDisplayDuration() ? "显示" : "隐藏",
                DISPLAY_DURATION_OPTIONS);

        timeLanguageDialogBinding.layoutDateYearRow.tvAdjustValue.setText(String.valueOf(draft.getClockYear()));
        timeLanguageDialogBinding.layoutDateMonthRow.tvAdjustValue.setText(String.valueOf(draft.getClockMonth()));
        timeLanguageDialogBinding.layoutDateDayRow.tvAdjustValue.setText(String.valueOf(draft.getClockDay()));
        timeLanguageDialogBinding.layoutTimeHourRow.tvAdjustValue.setText(String.valueOf(draft.getClockHour()));
        timeLanguageDialogBinding.layoutTimeMinuteRow.tvAdjustValue.setText(String.valueOf(draft.getClockMinute()));
        timeLanguageDialogBinding.layoutTimeSecondRow.tvAdjustValue.setText(String.valueOf(draft.getClockSecond()));
        timeLanguageDialogBinding.tvDateUnitHint.setText("单位: " + safeText(draft.getDateUnit()));
        timeLanguageDialogBinding.tvTimeUnitHint.setText("单位: " + safeText(draft.getTimeUnit()));
    }

    private void updateSpinnerSelection(Spinner spinner, String targetValue, String[] options) {
        if (spinner == null || options == null) {
            return;
        }
        for (int index = 0; index < options.length; index++) {
            if (TextUtils.equals(options[index], targetValue)) {
                if (spinner.getSelectedItemPosition() != index) {
                    spinner.setSelection(index, false);
                }
                return;
            }
        }
        if (spinner.getSelectedItemPosition() != 0) {
            spinner.setSelection(0, false);
        }
    }

    private void adjustTimeLanguageValue(int calendarField, boolean increase) {
        Calendar calendar = buildDraftClockCalendar();
        calendar.add(calendarField, increase ? 1 : -1);
        applyCalendarToDraft(calendar);
        renderTimeLanguageDialog();
    }

    private void saveTimeLanguageSettings() {
        ClinicSettings draft = ensureDraftTimeLanguageSettings();
        String previousLanguage = settings == null ? "中文" : settings.getLanguage();
        boolean languageChanged = !TextUtils.equals(previousLanguage, draft.getLanguage());
        clinicViewModel.saveSettings(draft.copy());
        dismissManagedDialog(timeLanguageDialog);
        if (languageChanged) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("语言切换")
                    .setMessage("语言已切换为 " + draft.getLanguage() + "，是否立即应用界面文案？")
                    .setPositiveButton("立即应用", (dialog, which) ->
                            showToast("当前版本已保存语言设置，多语言文案将在下一轮完整接入"))
                    .setNegativeButton("稍后", (dialog, which) ->
                            showToast("时间与语言设置已保存"))
                    .show();
            return;
        }
        showToast("时间与语言设置已保存");
    }

    private ClinicSettings buildTimeLanguageDraft(@Nullable ClinicSettings source) {
        ClinicSettings draft = settings == null ? new ClinicSettings() : settings.copy();
        Calendar calendar = Calendar.getInstance();
        draft.setLanguage("中文");
        draft.setShowDisplayDuration(true);
        draft.setDateUnit("年/月/日");
        draft.setTimeUnit("时/分/秒");
        draft.setClockYear(calendar.get(Calendar.YEAR));
        draft.setClockMonth(calendar.get(Calendar.MONTH) + 1);
        draft.setClockDay(calendar.get(Calendar.DAY_OF_MONTH));
        draft.setClockHour(calendar.get(Calendar.HOUR_OF_DAY));
        draft.setClockMinute(calendar.get(Calendar.MINUTE));
        draft.setClockSecond(calendar.get(Calendar.SECOND));
        if (source != null) {
            draft.setLanguage(TextUtils.isEmpty(source.getLanguage()) ? "中文" : source.getLanguage());
            draft.setShowDisplayDuration(source.isShowDisplayDuration());
            draft.setDateUnit(TextUtils.isEmpty(source.getDateUnit()) ? "年/月/日" : source.getDateUnit());
            draft.setTimeUnit(TextUtils.isEmpty(source.getTimeUnit()) ? "时/分/秒" : source.getTimeUnit());
            if (source.getClockYear() > 0) {
                draft.setClockYear(source.getClockYear());
                draft.setClockMonth(source.getClockMonth());
                draft.setClockDay(source.getClockDay());
                draft.setClockHour(source.getClockHour());
                draft.setClockMinute(source.getClockMinute());
                draft.setClockSecond(source.getClockSecond());
            }
        }
        return draft;
    }

    private ClinicSettings ensureDraftTimeLanguageSettings() {
        if (draftTimeLanguageSettings == null) {
            draftTimeLanguageSettings = buildTimeLanguageDraft(settings);
        }
        return draftTimeLanguageSettings;
    }

    private Calendar buildDraftClockCalendar() {
        ClinicSettings draft = ensureDraftTimeLanguageSettings();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, draft.getClockYear());
        calendar.set(Calendar.MONTH, Math.max(0, draft.getClockMonth() - 1));
        calendar.set(Calendar.DAY_OF_MONTH, Math.max(1, draft.getClockDay()));
        calendar.set(Calendar.HOUR_OF_DAY, Math.max(0, draft.getClockHour()));
        calendar.set(Calendar.MINUTE, Math.max(0, draft.getClockMinute()));
        calendar.set(Calendar.SECOND, Math.max(0, draft.getClockSecond()));
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private void applyCalendarToDraft(Calendar calendar) {
        ClinicSettings draft = ensureDraftTimeLanguageSettings();
        draft.setClockYear(calendar.get(Calendar.YEAR));
        draft.setClockMonth(calendar.get(Calendar.MONTH) + 1);
        draft.setClockDay(calendar.get(Calendar.DAY_OF_MONTH));
        draft.setClockHour(calendar.get(Calendar.HOUR_OF_DAY));
        draft.setClockMinute(calendar.get(Calendar.MINUTE));
        draft.setClockSecond(calendar.get(Calendar.SECOND));
    }

    private void showStepLengthDialog() {
        if (stepLengthDialog != null && stepLengthDialog.isShowing()) {
            return;
        }
        stepLengthDialogBinding = DialogStepLengthBinding.inflate(getLayoutInflater());
        stepLengthDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(stepLengthDialogBinding.getRoot())
                .create();
        draftStepLengthSettings = buildStepLengthDraft(settings);
        bindStepLengthDialogActions();
        renderStepLengthDialog();
        showManagedDialog(stepLengthDialog, 0.86f);
    }

    private void bindStepLengthDialogActions() {
        if (stepLengthDialogBinding == null) {
            return;
        }
        bindStepButton(R.id.btnSphStepMinus, ExamSession.MeasurementField.SPH, false, false);
        bindStepButton(R.id.btnSphStepPlus, ExamSession.MeasurementField.SPH, false, true);
        bindStepButton(R.id.btnSphShiftMinus, ExamSession.MeasurementField.SPH, true, false);
        bindStepButton(R.id.btnSphShiftPlus, ExamSession.MeasurementField.SPH, true, true);

        bindStepButton(R.id.btnCylStepMinus, ExamSession.MeasurementField.CYL, false, false);
        bindStepButton(R.id.btnCylStepPlus, ExamSession.MeasurementField.CYL, false, true);
        bindStepButton(R.id.btnCylShiftMinus, ExamSession.MeasurementField.CYL, true, false);
        bindStepButton(R.id.btnCylShiftPlus, ExamSession.MeasurementField.CYL, true, true);

        bindStepButton(R.id.btnAxisStepMinus, ExamSession.MeasurementField.AXIS, false, false);
        bindStepButton(R.id.btnAxisStepPlus, ExamSession.MeasurementField.AXIS, false, true);
        bindStepButton(R.id.btnAxisShiftMinus, ExamSession.MeasurementField.AXIS, true, false);
        bindStepButton(R.id.btnAxisShiftPlus, ExamSession.MeasurementField.AXIS, true, true);

        bindStepButton(R.id.btnPrismStepMinus, ExamSession.MeasurementField.X, false, false);
        bindStepButton(R.id.btnPrismStepPlus, ExamSession.MeasurementField.X, false, true);
        bindStepButton(R.id.btnPrismShiftMinus, ExamSession.MeasurementField.X, true, false);
        bindStepButton(R.id.btnPrismShiftPlus, ExamSession.MeasurementField.X, true, true);

        bindStepButton(R.id.btnPdStepMinus, ExamSession.MeasurementField.PD, false, false);
        bindStepButton(R.id.btnPdStepPlus, ExamSession.MeasurementField.PD, false, true);
        bindStepButton(R.id.btnPdShiftMinus, ExamSession.MeasurementField.PD, true, false);
        bindStepButton(R.id.btnPdShiftPlus, ExamSession.MeasurementField.PD, true, true);

        stepLengthDialogBinding.btnResetStepLength.setOnClickListener(v -> {
            draftStepLengthSettings = buildStepLengthDraft(null);
            renderStepLengthDialog();
        });
        stepLengthDialogBinding.btnConfirmStepLength.setOnClickListener(v -> saveStepLengthSettings());
        stepLengthDialogBinding.btnCancelStepLength.setOnClickListener(v -> dismissManagedDialog(stepLengthDialog));
    }

    private void renderStepLengthDialog() {
        if (stepLengthDialogBinding == null || draftStepLengthSettings == null) {
            return;
        }
        setStepValueText(R.id.tvSphStepValue, draftStepLengthSettings.getSphStep());
        setStepValueText(R.id.tvSphShiftValue, draftStepLengthSettings.getSphShiftStep());
        setStepValueText(R.id.tvCylStepValue, draftStepLengthSettings.getCylStep());
        setStepValueText(R.id.tvCylShiftValue, draftStepLengthSettings.getCylShiftStep());
        setStepValueText(R.id.tvAxisStepValue, draftStepLengthSettings.getAxisStep());
        setStepValueText(R.id.tvAxisShiftValue, draftStepLengthSettings.getAxisShiftStep());
        setStepValueText(R.id.tvPrismStepValue, draftStepLengthSettings.getPrismStep());
        setStepValueText(R.id.tvPrismShiftValue, draftStepLengthSettings.getPrismShiftStep());
        setStepValueText(R.id.tvPdStepValue, draftStepLengthSettings.getPdStep());
        setStepValueText(R.id.tvPdShiftValue, draftStepLengthSettings.getPdShiftStep());
    }

    private void adjustStepLengthValue(ExamSession.MeasurementField field, boolean shift, boolean increase) {
        if (draftStepLengthSettings == null) {
            draftStepLengthSettings = buildStepLengthDraft(settings);
        }
        double currentValue = getStepSettingValue(draftStepLengthSettings, field, shift);
        double nextValue = resolveAdjustedStepValue(resolveStepOptions(field, shift), currentValue, increase);
        setStepSettingValue(draftStepLengthSettings, field, shift, nextValue);
        renderStepLengthDialog();
    }

    private void saveStepLengthSettings() {
        if (draftStepLengthSettings == null) {
            return;
        }
        clinicViewModel.saveSettings(draftStepLengthSettings.copy());
        dismissManagedDialog(stepLengthDialog);
        showToast("步长设置已保存");
    }

    private void bindStepButton(int viewId, ExamSession.MeasurementField field, boolean shift, boolean increase) {
        if (stepLengthDialogBinding == null) {
            return;
        }
        View target = stepLengthDialogBinding.getRoot().findViewById(viewId);
        if (target != null) {
            target.setOnClickListener(v -> adjustStepLengthValue(field, shift, increase));
        }
    }

    private void setStepValueText(int viewId, double value) {
        if (stepLengthDialogBinding == null) {
            return;
        }
        TextView textView = stepLengthDialogBinding.getRoot().findViewById(viewId);
        if (textView != null) {
            textView.setText(formatStepValue(value));
        }
    }

    private void renderMetricValue(ExamSession.MeasurementField field, LensMeasurement right, LensMeasurement left) {
        MetricRowHolder holder = metricRows.get(field);
        if (holder == null) {
            return;
        }
        holder.rightValue.setText(formatMeasurementValue(field, right));
        holder.leftValue.setText(formatMeasurementValue(field, left));
    }

    private void updateMetricLabel(ExamSession.MeasurementField field, String label) {
        MetricRowHolder holder = metricRows.get(field);
        if (holder != null && holder.label != null) {
            holder.label.setText(label);
        }
    }

    private LensMeasurement resolveMeasurement(boolean rightEye) {
        if (session == null) {
            return new LensMeasurement();
        }
        if (session.getLensDataSource() == ExamSession.LensDataSource.FINAL) {
            return rightEye ? session.getFinalRight() : session.getFinalLeft();
        }
        if (session.getDistanceMode() == ExamSession.DistanceMode.NEAR) {
            return rightEye ? session.getNearRight() : session.getNearLeft();
        }
        return rightEye ? session.getFarRight() : session.getFarLeft();
    }

    private String formatMeasurementValue(ExamSession.MeasurementField field, LensMeasurement measurement) {
        if (measurement == null) {
            return "--";
        }
        switch (field) {
            case SPH:
                return ClinicFormatters.formatSigned(measurement.getSph());
            case CYL:
                return ClinicFormatters.formatSigned(measurement.getCyl());
            case AXIS:
                return ClinicFormatters.formatAxis(measurement.getAxis());
            case ADD:
                return ClinicFormatters.formatSigned(measurement.getAdd());
            case VA:
                return ClinicFormatters.formatUnsigned(measurement.getVa());
            case X:
                return session != null && session.getPrismMode() == ExamSession.PrismMode.POLAR
                        ? ClinicFormatters.formatUnsigned(measurement.getPrismR())
                        : ClinicFormatters.formatSigned(measurement.getPrismX());
            case Y:
                return session != null && session.getPrismMode() == ExamSession.PrismMode.POLAR
                        ? ClinicFormatters.formatAxis(measurement.getPrismTheta())
                        : ClinicFormatters.formatSigned(measurement.getPrismY());
            default:
                return "--";
        }
    }

    private VisionChart findSelectedChart() {
        if (chartList.isEmpty()) {
            return null;
        }
        if (session == null || TextUtils.isEmpty(session.getSelectedChartId())) {
            return chartList.get(0);
        }
        for (VisionChart chart : chartList) {
            if (TextUtils.equals(chart.getId(), session.getSelectedChartId())) {
                return chart;
            }
        }
        return chartList.get(0);
    }

    private ExamProgram findCurrentProgram() {
        if (session == null) {
            return null;
        }
        for (ExamProgram program : programList) {
            if (TextUtils.equals(program.getId(), session.getCurrentProgramId())) {
                return program;
            }
        }
        return null;
    }

    private String resolveProgramName() {
        ExamProgram program = findCurrentProgram();
        return program == null ? "无程序" : program.getTitle();
    }

    private String resolveLeftStepLabel() {
        if (currentStep != null && !TextUtils.isEmpty(currentStep.getTitle())) {
            return currentStep.getTitle();
        }
        ExamProgram program = findCurrentProgram();
        return program == null ? "无程序" : program.getTitle();
    }

    private String resolveProgramProgress() {
        ExamProgram program = findCurrentProgram();
        if (program == null) {
            return "0/0";
        }
        return (session == null ? 0 : session.getCurrentStepIndex() + 1) + "/" + program.getSteps().size();
    }

    private String resolveSubjectName() {
        return session == null || session.getPatient() == null ? "未绑定被测者" : session.getPatient().getDisplayName();
    }

    private String resolveSubjectMeta() {
        if (session == null || session.getPatient() == null) {
            return "点击左侧“被测者”录入或检索";
        }
        PatientProfile patient = session.getPatient();
        return safeText(patient.getPhone()) + " | " + safeText(patient.getGender()) + " | " + safeText(patient.getBirthDate());
    }

    private String resolveStepTitle() {
        return currentStep == null ? "当前步骤" : currentStep.getTitle();
    }

    private String resolveStepMeta() {
        if (currentStep == null) {
            return "请选择程序后开始验光流程";
        }
        return currentStep.getDescription()
                + "\n数据源: " + resolveLensDataSourceLabel(session == null ? null : session.getLensDataSource())
                + " | " + (session == null ? "--" : session.getActiveEye().name())
                + " | " + (session != null && session.isUnsavedChanges() ? "草稿未保存" : "草稿已保存");
    }

    private String resolveServerStatus() {
        if (deviceUiState == null) {
            return "服务状态未知";
        }
        if (deviceUiState.isServerRunning()) {
            String status = "服务运行中  " + safeText(deviceUiState.getLocalIp()) + ":" + deviceUiState.getServerPort();
            if (!TextUtils.isEmpty(deviceUiState.getPendingMessage())) {
                status = status + "\n最近指令: " + deviceUiState.getPendingMessage();
            }
            return status;
        }
        return "服务已停止";
    }

    private String resolveBoundDeviceStatus() {
        if (deviceUiState == null || TextUtils.isEmpty(deviceUiState.getBoundDeviceLabel())) {
            return "未绑定主设备";
        }
        return "主设备: " + safeText(deviceUiState.getBoundDeviceLabel())
                + (deviceUiState.isBoundDeviceOnline() ? " (在线)" : " (离线)")
                + "\nMAC: " + safeText(deviceUiState.getBoundDeviceMacAddress());
    }

    private ClinicSettings buildStepLengthDraft(@Nullable ClinicSettings source) {
        ClinicSettings draft = settings == null ? new ClinicSettings() : settings.copy();
        draft.setSphStep(0.25d);
        draft.setSphShiftStep(1.00d);
        draft.setCylStep(0.25d);
        draft.setCylShiftStep(1.00d);
        draft.setAxisStep(5.00d);
        draft.setAxisShiftStep(15.00d);
        draft.setPrismStep(0.50d);
        draft.setPrismShiftStep(1.00d);
        draft.setPdStep(0.50d);
        draft.setPdShiftStep(1.00d);
        if (source != null) {
            draft.setSphStep(source.getSphStep());
            draft.setSphShiftStep(source.getSphShiftStep());
            draft.setCylStep(source.getCylStep());
            draft.setCylShiftStep(source.getCylShiftStep());
            draft.setAxisStep(source.getAxisStep());
            draft.setAxisShiftStep(source.getAxisShiftStep());
            draft.setPrismStep(source.getPrismStep());
            draft.setPrismShiftStep(source.getPrismShiftStep());
            draft.setPdStep(source.getPdStep());
            draft.setPdShiftStep(source.getPdShiftStep());
        }
        return draft;
    }

    private double[] resolveStepOptions(ExamSession.MeasurementField field, boolean shift) {
        switch (field) {
            case SPH:
                return shift ? SPH_SHIFT_OPTIONS : SPH_STEP_OPTIONS;
            case CYL:
                return shift ? CYL_SHIFT_OPTIONS : CYL_STEP_OPTIONS;
            case AXIS:
                return shift ? AXIS_SHIFT_OPTIONS : AXIS_STEP_OPTIONS;
            case PD:
                return shift ? PD_SHIFT_OPTIONS : PD_STEP_OPTIONS;
            case X:
            case Y:
            default:
                return shift ? PRISM_SHIFT_OPTIONS : PRISM_STEP_OPTIONS;
        }
    }

    private double resolveAdjustedStepValue(double[] options, double currentValue, boolean increase) {
        if (options == null || options.length == 0) {
            return currentValue;
        }
        int nearestIndex = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int index = 0; index < options.length; index++) {
            double distance = Math.abs(options[index] - currentValue);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = index;
            }
        }
        int nextIndex = increase ? Math.min(nearestIndex + 1, options.length - 1) : Math.max(nearestIndex - 1, 0);
        return options[nextIndex];
    }

    private double getStepSettingValue(ClinicSettings value, ExamSession.MeasurementField field, boolean shift) {
        switch (field) {
            case SPH:
                return shift ? value.getSphShiftStep() : value.getSphStep();
            case CYL:
                return shift ? value.getCylShiftStep() : value.getCylStep();
            case AXIS:
                return shift ? value.getAxisShiftStep() : value.getAxisStep();
            case PD:
                return shift ? value.getPdShiftStep() : value.getPdStep();
            case X:
            case Y:
            default:
                return shift ? value.getPrismShiftStep() : value.getPrismStep();
        }
    }

    private void setStepSettingValue(ClinicSettings value, ExamSession.MeasurementField field, boolean shift, double nextValue) {
        switch (field) {
            case SPH:
                if (shift) {
                    value.setSphShiftStep(nextValue);
                } else {
                    value.setSphStep(nextValue);
                }
                return;
            case CYL:
                if (shift) {
                    value.setCylShiftStep(nextValue);
                } else {
                    value.setCylStep(nextValue);
                }
                return;
            case AXIS:
                if (shift) {
                    value.setAxisShiftStep(nextValue);
                } else {
                    value.setAxisStep(nextValue);
                }
                return;
            case PD:
                if (shift) {
                    value.setPdShiftStep(nextValue);
                } else {
                    value.setPdStep(nextValue);
                }
                return;
            case X:
            case Y:
            default:
                if (shift) {
                    value.setPrismShiftStep(nextValue);
                } else {
                    value.setPrismStep(nextValue);
                }
        }
    }

    private String formatStepValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001d) {
            return String.format(Locale.getDefault(), "%.1f", value);
        }
        if (Math.abs(value * 10d - Math.rint(value * 10d)) < 0.001d) {
            return String.format(Locale.getDefault(), "%.1f", value);
        }
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    private String resolveCurrentStepText() {
        if (settings == null || session == null || session.getSelectedField() == null) {
            return "0.25";
        }
        double value;
        switch (session.getSelectedField()) {
            case CYL:
                value = session.isShiftEnabled() ? settings.getCylShiftStep() : settings.getCylStep();
                break;
            case AXIS:
                value = session.isShiftEnabled() ? settings.getAxisShiftStep() : settings.getAxisStep();
                break;
            case X:
            case Y:
                value = session.isShiftEnabled() ? settings.getPrismShiftStep() : settings.getPrismStep();
                break;
            default:
                value = session.isShiftEnabled() ? settings.getSphShiftStep() : settings.getSphStep();
                break;
        }
        return ClinicFormatters.formatUnsigned(value);
    }

    private String resolveTotalPd() {
        LensMeasurement right = resolveMeasurement(true);
        LensMeasurement left = resolveMeasurement(false);
        return ClinicFormatters.formatUnsigned(right.getPd() + left.getPd());
    }

    private String resolveLensDataSourceLabel(ExamSession.LensDataSource source) {
        if (source == null) {
            return "--";
        }
        switch (source) {
            case SUBJECTIVE:
                return "SubJ";
            case UNAIDED:
                return "Un-aided";
            default:
                return source.name();
        }
    }

    private String resolveToolLabel(ExamSession.ToolType toolType) {
        if (toolType == null) {
            return "--";
        }
        return toolType == ExamSession.ToolType.ACA ? "AC/A" : toolType.name();
    }

    private String resolveToolTitle(ExamSession.ToolType toolType) {
        switch (toolType) {
            case NPC:
                return "NPC: 集合近点";
            case NPA:
                return "NPA: 调节近点";
            case NRA:
                return "NRA: 负相对调节";
            case PRA:
                return "PRA: 正相对调节";
            case ACA:
                return "AC/A（梯度法）";
            case AMP:
                return "AMP检查";
            default:
                return "";
        }
    }

    private String resolveToolHint(ExamSession.ToolType toolType) {
        switch (toolType) {
            case NPC:
                return "本测试无需使用验光头，逐步将固视点靠近患者眼睛。";
            case NPA:
                return "逐步推进至视标模糊，记录近点距离。";
            case NRA:
                return "点 + 号直至模糊，点击模糊/恢复记录数据。";
            case PRA:
                return "点 - 号直至模糊，点击模糊/恢复记录数据。";
            case ACA:
                return "点号调整 BI 值，当视标对齐时记录首次/再次对齐。";
            case AMP:
                return "将视标缓慢向鼻根部移动，分别记录左右眼结果。";
            default:
                return "";
        }
    }

    private String resolveDeviceLogOutput() {
        if (deviceUiState == null || deviceUiState.getLogs().isEmpty()) {
            return "暂无连接日志";
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(8, deviceUiState.getLogs().size());
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(deviceUiState.getLogs().get(index));
        }
        return builder.toString();
    }

    private boolean isMainDeviceReady() {
        return deviceUiState != null && deviceUiState.isServerRunning() && deviceUiState.isBoundDeviceOnline();
    }

    private void applyBadge(TextView badge, boolean active) {
        badge.setBackgroundResource(active ? R.drawable.bg_workbench_status_badge_online : R.drawable.bg_workbench_status_badge_idle);
    }

    private void applyConsoleButton(TextView view, boolean active) {
        view.setBackgroundResource(active ? R.drawable.bg_workbench_action_button_selected : R.drawable.bg_workbench_action_button);
    }

    private void applyDialogTab(MaterialButton button, boolean active) {
        button.setBackgroundResource(active ? R.drawable.bg_workbench_tab_active : R.drawable.bg_workbench_tab_inactive);
        button.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.workbench_accent : R.color.console_text_dark));
    }

    private MaterialButton createInlineChip(String text, boolean selected, View.OnClickListener listener) {
        MaterialButton button = createActionButton(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        params.rightMargin = dp(8);
        button.setLayoutParams(params);
        button.setText(text);
        applyConsoleButton(button, selected);
        button.setOnClickListener(listener);
        return button;
    }

    private MaterialCardView createDialogCard() {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(10);
        card.setLayoutParams(params);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        card.setRadius(dp(10));
        card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.console_border));
        card.setStrokeWidth(dp(1));
        return card;
    }

    private LinearLayout createDialogCardContent(MaterialCardView card) {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.addView(content);
        return content;
    }

    private TextView createDialogTitle(String text) {
        return createText(requireContext(), text, 18, ContextCompat.getColor(requireContext(), R.color.console_text_dark), true);
    }

    private TextView createDialogBody(String text) {
        return createText(requireContext(), text, 13, ContextCompat.getColor(requireContext(), R.color.console_text_soft), false);
    }

    private MaterialButton createDialogButton(String text, boolean primary) {
        MaterialButton button = createActionButton(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(46)
        );
        params.topMargin = dp(10);
        button.setLayoutParams(params);
        button.setText(text);
        button.setBackgroundResource(primary ? R.drawable.bg_workbench_primary_button : R.drawable.bg_workbench_action_button);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setTextColor(ContextCompat.getColor(requireContext(),
                primary ? android.R.color.white : R.color.console_text_dark));
        return button;
    }

    private TextView createHintText(String message) {
        TextView textView = createDialogBody(message);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(dp(4), dp(18), dp(4), dp(18));
        return textView;
    }

    private MaterialCardView createChartCatalogItem(VisionChart chart) {
        boolean selected = session != null && TextUtils.equals(session.getSelectedChartId(), chart.getId());
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                selected ? android.R.color.white : R.color.console_panel));
        card.setStrokeColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.console_blue_bar : R.color.console_border));
        card.setStrokeWidth(dp(selected ? 2 : 1));
        card.setRadius(dp(8));
        card.setOnClickListener(v -> clinicViewModel.selectChart(chart.getId()));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.addView(content);

        ImageView preview = new ImageView(requireContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        preview.setLayoutParams(previewParams);
        preview.setBackgroundResource(R.drawable.bg_workbench_surface_panel);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setPadding(dp(4), dp(4), dp(4), dp(4));
        preview.setImageResource(chart.getImageResId());
        content.addView(preview);

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        textParams.leftMargin = dp(10);
        texts.setLayoutParams(textParams);
        content.addView(texts);

        TextView title = createText(requireContext(), chart.getTitle(), 18,
                ContextCompat.getColor(requireContext(), R.color.console_text_dark), true);
        texts.addView(title);

        TextView subtitle = createText(requireContext(), safeText(chart.getSubtitle()), 12,
                ContextCompat.getColor(requireContext(), R.color.console_text_soft), false);
        subtitle.setMaxLines(2);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(4);
        subtitle.setLayoutParams(subtitleParams);
        texts.addView(subtitle);

        TextView marker = createText(requireContext(), selected ? "当前" : "切换", 12,
                ContextCompat.getColor(requireContext(), selected ? R.color.workbench_accent : R.color.console_text_soft), true);
        marker.setPadding(dp(8), dp(6), dp(8), dp(6));
        marker.setBackgroundResource(selected ? R.drawable.bg_workbench_status_badge_online : R.drawable.bg_workbench_status_badge_idle);
        content.addView(marker);
        return card;
    }

    private String readText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String safeText(String value) {
        return TextUtils.isEmpty(value) ? "--" : value;
    }

    private String safeNote(String note) {
        return TextUtils.isEmpty(note) ? "未记录" : note;
    }

    private void renderClock() {
        if (binding != null) {
            binding.tvToolbarClock.setText(CLOCK_FORMAT.format(System.currentTimeMillis()));
        }
    }

    private void dismissManagedDialog(@Nullable AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private void showManagedDialog(AlertDialog dialog, float widthFraction) {
        if (dialog == null) {
            return;
        }
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * widthFraction);
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
    }
}
