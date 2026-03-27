package com.wifi.optometry.ui.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.wifi.optometry.R;
import com.wifi.optometry.databinding.FragmentWorkbenchBinding;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.FunctionalTestState;
import com.wifi.optometry.domain.model.LensMeasurement;
import com.wifi.optometry.domain.model.PatientProfile;
import com.wifi.optometry.domain.model.VisionChart;
import com.wifi.optometry.util.ClinicFormatters;

import java.util.ArrayList;
import java.util.List;

public class WorkbenchFragment extends BaseClinicFragment<FragmentWorkbenchBinding> {
    private TextView tvPatientSummary;
    private TextView tvProgramSummary;
    private TextView tvStepSummary;
    private LinearProgressIndicator progressIndicator;
    private ChipGroup chipGroupCharts;
    private ChipGroup chipGroupFields;
    private ChipGroup chipGroupEyes;
    private ChipGroup chipGroupLens;
    private ImageView ivChartPreview;
    private TextView tvChartTitle;
    private TextView tvChartSubtitle;
    private TextView tvChartDescription;
    private TextView tvMeasurementMode;
    private TextView tvFarRight;
    private TextView tvFarLeft;
    private TextView tvNearRight;
    private TextView tvNearLeft;
    private TextView tvFinalRight;
    private TextView tvFinalLeft;
    private LinearLayout layoutModeButtons;
    private LinearLayout layoutFunctionRows;
    private EditText etSessionNote;
    private EditText etStepNote;

    private MaterialButton btnDistanceMode;
    private MaterialButton btnPrismMode;
    private MaterialButton btnCylMode;
    private MaterialButton btnCpLink;
    private MaterialButton btnLensInserted;
    private MaterialButton btnNearLamp;

    private TextView tvNpcValue;
    private TextView tvNpaValue;
    private TextView tvNraValue;
    private TextView tvPraValue;
    private TextView tvAcaValue;
    private TextView tvAmpValue;

    private final List<VisionChart> chartList = new ArrayList<>();
    private final List<ExamProgram> programList = new ArrayList<>();
    private ExamSession session;
    private ExamStep currentStep;
    private boolean updatingUi;

    @Nullable
    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        bindViews();
        bindStaticListeners();
        buildModeButtons();
        buildFunctionRows();
    }

    @Override
    protected void observeUi() {
        observeState();
    }

    private void bindViews() {
        tvPatientSummary = binding.tvPatientSummary;
        tvProgramSummary = binding.tvProgramSummary;
        tvStepSummary = binding.tvStepSummary;
        progressIndicator = binding.progressCurrentStep;
        chipGroupCharts = binding.chipGroupCharts;
        chipGroupFields = binding.chipGroupFields;
        chipGroupEyes = binding.chipGroupEyes;
        chipGroupLens = binding.chipGroupLens;
        ivChartPreview = binding.ivChartPreview;
        tvChartTitle = binding.tvChartTitle;
        tvChartSubtitle = binding.tvChartSubtitle;
        tvChartDescription = binding.tvChartDescription;
        tvMeasurementMode = binding.tvMeasurementMode;
        tvFarRight = binding.tvFarRight;
        tvFarLeft = binding.tvFarLeft;
        tvNearRight = binding.tvNearRight;
        tvNearLeft = binding.tvNearLeft;
        tvFinalRight = binding.tvFinalRight;
        tvFinalLeft = binding.tvFinalLeft;
        layoutModeButtons = binding.layoutModeButtons;
        layoutFunctionRows = binding.layoutFunctionRows;
        etSessionNote = binding.etSessionNote;
        etStepNote = binding.etStepNote;

        addChoiceChip(chipGroupFields, "SPH", ExamSession.MeasurementField.SPH.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.SPH));
        addChoiceChip(chipGroupFields, "CYL", ExamSession.MeasurementField.CYL.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.CYL));
        addChoiceChip(chipGroupFields, "AXIS", ExamSession.MeasurementField.AXIS.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.AXIS));
        addChoiceChip(chipGroupFields, "ADD", ExamSession.MeasurementField.ADD.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.ADD));
        addChoiceChip(chipGroupFields, "VA", ExamSession.MeasurementField.VA.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.VA));
        addChoiceChip(chipGroupFields, "X", ExamSession.MeasurementField.X.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.X));
        addChoiceChip(chipGroupFields, "Y", ExamSession.MeasurementField.Y.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.Y));
        addChoiceChip(chipGroupFields, "PD", ExamSession.MeasurementField.PD.name(), v -> clinicViewModel.selectField(ExamSession.MeasurementField.PD));

        addChoiceChip(chipGroupEyes, "右眼", ExamSession.EyeSelection.RIGHT.name(), v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.RIGHT));
        addChoiceChip(chipGroupEyes, "左眼", ExamSession.EyeSelection.LEFT.name(), v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.LEFT));
        addChoiceChip(chipGroupEyes, "双眼", ExamSession.EyeSelection.BOTH.name(), v -> clinicViewModel.setActiveEye(ExamSession.EyeSelection.BOTH));

        addChoiceChip(chipGroupLens, "显右", "lens_right", v -> clinicViewModel.setLensVisibility(ExamSession.EyeSelection.RIGHT));
        addChoiceChip(chipGroupLens, "显左", "lens_left", v -> clinicViewModel.setLensVisibility(ExamSession.EyeSelection.LEFT));
        addChoiceChip(chipGroupLens, "显双", "lens_both", v -> clinicViewModel.setLensVisibility(ExamSession.EyeSelection.BOTH));
    }

    private void bindStaticListeners() {
        binding.btnPrevStep.setOnClickListener(v -> clinicViewModel.moveToPreviousStep());
        binding.btnNextStep.setOnClickListener(v -> clinicViewModel.moveToNextStep());
        binding.btnChartHelp.setOnClickListener(v -> showChartHelp());
        binding.btnAdjustMinus.setOnClickListener(v -> clinicViewModel.adjustMeasurement(false, false));
        binding.btnAdjustPlus.setOnClickListener(v -> clinicViewModel.adjustMeasurement(true, false));
        binding.btnShiftMinus.setOnClickListener(v -> clinicViewModel.adjustMeasurement(false, true));
        binding.btnShiftPlus.setOnClickListener(v -> clinicViewModel.adjustMeasurement(true, true));
        binding.btnSaveSessionNote.setOnClickListener(v -> clinicViewModel.updateSessionNote(readText(etSessionNote)));
        binding.btnSaveStepNote.setOnClickListener(v -> clinicViewModel.updateCurrentStepNote(readText(etStepNote)));
    }

    private void observeState() {
        clinicViewModel.getCharts().observe(getViewLifecycleOwner(), charts -> {
            chartList.clear();
            if (charts != null) {
                chartList.addAll(charts);
            }
            chipGroupCharts.removeAllViews();
            for (VisionChart chart : chartList) {
                addChoiceChip(chipGroupCharts, chart.getTitle(), chart.getId(), clinicViewModel::selectChart);
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
        clinicViewModel.getSession().observe(getViewLifecycleOwner(), examSession -> {
            session = examSession;
            renderAll();
        });
        clinicViewModel.getCurrentStep().observe(getViewLifecycleOwner(), examStep -> {
            currentStep = examStep;
            renderAll();
        });
    }

    private void buildModeButtons() {
        btnDistanceMode = createModeButton(v -> clinicViewModel.toggleDistanceMode());
        btnPrismMode = createModeButton(v -> clinicViewModel.togglePrismMode());
        btnCylMode = createModeButton(v -> clinicViewModel.toggleCylMode());
        btnCpLink = createModeButton(v -> clinicViewModel.toggleCpLink());
        btnLensInserted = createModeButton(v -> clinicViewModel.toggleLensInserted());
        btnNearLamp = createModeButton(v -> clinicViewModel.toggleNearLamp());
        layoutModeButtons.addView(btnDistanceMode);
        layoutModeButtons.addView(btnPrismMode);
        layoutModeButtons.addView(btnCylMode);
        layoutModeButtons.addView(btnCpLink);
        layoutModeButtons.addView(btnLensInserted);
        layoutModeButtons.addView(btnNearLamp);
    }

    private void buildFunctionRows() {
        layoutFunctionRows.removeAllViews();
        tvNpcValue = addFunctionRow("NPC", v -> clinicViewModel.adjustFunctionalValue("npc", false), v -> clinicViewModel.adjustFunctionalValue("npc", true), null, null);
        tvNpaValue = addFunctionRow("NPA", v -> clinicViewModel.adjustFunctionalValue("npa", false), v -> clinicViewModel.adjustFunctionalValue("npa", true), null, null);
        tvNraValue = addFunctionRow("NRA", v -> clinicViewModel.adjustFunctionalValue("nra", false), v -> clinicViewModel.adjustFunctionalValue("nra", true),
                createExtraButton("模糊", value -> clinicViewModel.markFunctionEvent("nra", "模糊")),
                createExtraButton("恢复", value -> clinicViewModel.markFunctionEvent("nra", "恢复")));
        tvPraValue = addFunctionRow("PRA", v -> clinicViewModel.adjustFunctionalValue("pra", false), v -> clinicViewModel.adjustFunctionalValue("pra", true),
                createExtraButton("模糊", value -> clinicViewModel.markFunctionEvent("pra", "模糊")),
                createExtraButton("恢复", value -> clinicViewModel.markFunctionEvent("pra", "恢复")));
        tvAcaValue = addFunctionRow("AC/A", v -> clinicViewModel.adjustFunctionalValue("aca_bi", false), v -> clinicViewModel.adjustFunctionalValue("aca_bi", true),
                createExtraButton("目标+", value -> clinicViewModel.adjustFunctionalValue("aca_target", true)),
                createExtraButton("对齐", value -> clinicViewModel.markFunctionEvent("aca", "首次对齐")));
        tvAmpValue = addFunctionRow("AMP", v -> clinicViewModel.adjustFunctionalValue("amp_right", false), v -> clinicViewModel.adjustFunctionalValue("amp_right", true),
                createExtraButton("左-", value -> clinicViewModel.adjustFunctionalValue("amp_left", false)),
                createExtraButton("左+", value -> clinicViewModel.adjustFunctionalValue("amp_left", true)));
    }

    private void renderAll() {
        if (session == null) {
            return;
        }
        PatientProfile patient = session.getPatient();
        tvPatientSummary.setText(patient == null
                ? "未选择被测者，可到“被测者”页录入。"
                : patient.getDisplayName() + " | " + patient.getGender() + " | " + patient.getPhone());
        tvProgramSummary.setText(resolveProgramTitle(session.getCurrentProgramId()) + " | 进度 " + clinicViewModel.getProgressPercent() + "%");
        tvStepSummary.setText(currentStep == null ? "暂无步骤" : currentStep.getTitle() + "\n" + currentStep.getDescription());
        progressIndicator.setProgressCompat(clinicViewModel.getProgressPercent(), true);
        etSessionNote.setText(session.getNote());
        etStepNote.setText(currentStep == null ? "" : currentStep.getNote());

        VisionChart chart = findChart(session.getSelectedChartId());
        if (chart != null) {
            ivChartPreview.setImageResource(chart.getImageResId());
            tvChartTitle.setText(chart.getTitle());
            tvChartSubtitle.setText(chart.getSubtitle());
            tvChartDescription.setText(chart.getDescription());
            checkChip(chipGroupCharts, chart.getId());
        }

        checkChip(chipGroupFields, session.getSelectedField().name());
        checkChip(chipGroupEyes, session.getActiveEye().name());
        checkChip(chipGroupLens, session.getLensVisibility() == ExamSession.EyeSelection.RIGHT ? "lens_right"
                : session.getLensVisibility() == ExamSession.EyeSelection.LEFT ? "lens_left" : "lens_both");

        tvMeasurementMode.setText("调节项：" + session.getSelectedField().name()
                + " | 眼别：" + session.getActiveEye().name()
                + " | 镜片显示：" + session.getLensVisibility().name());
        tvFarRight.setText("远用右眼  " + buildLens(session.getFarRight()));
        tvFarLeft.setText("远用左眼  " + buildLens(session.getFarLeft()));
        tvNearRight.setText("近用右眼  " + buildLens(session.getNearRight()));
        tvNearLeft.setText("近用左眼  " + buildLens(session.getNearLeft()));
        tvFinalRight.setText("最终右眼  " + buildLens(session.getFinalRight()));
        tvFinalLeft.setText("最终左眼  " + buildLens(session.getFinalLeft()));

        btnDistanceMode.setText(session.getDistanceMode() == ExamSession.DistanceMode.FAR ? "远距" : "近距");
        btnPrismMode.setText(session.getPrismMode() == ExamSession.PrismMode.CARTESIAN ? "直角坐标" : "极坐标");
        btnCylMode.setText(session.isCylMinusMode() ? "C-" : "C+");
        btnCpLink.setText(session.isCpLinked() ? "CP 联动开" : "CP 联动关");
        btnLensInserted.setText(session.isLensInserted() ? "IN 已置入" : "IN 未置入");
        btnNearLamp.setText(session.getFunctionalTests().isNearLampOn() ? "近灯已开" : "近灯已关");

        FunctionalTestState tests = session.getFunctionalTests();
        tvNpcValue.setText(ClinicFormatters.formatUnsigned(tests.getNpc()) + " cm");
        tvNpaValue.setText(ClinicFormatters.formatUnsigned(tests.getNpa()) + " cm");
        tvNraValue.setText("+" + ClinicFormatters.formatUnsigned(tests.getNra()) + "D  " + safeNote(tests.getNraNote()));
        tvPraValue.setText(ClinicFormatters.formatSigned(tests.getPra()) + "D  " + safeNote(tests.getPraNote()));
        tvAcaValue.setText("BI " + ClinicFormatters.formatUnsigned(tests.getAcaBi()) + " / 目标 "
                + ClinicFormatters.formatUnsigned(tests.getAcaTarget()) + "  " + safeNote(tests.getAcaNote()));
        tvAmpValue.setText("R " + ClinicFormatters.formatUnsigned(tests.getAmpRight()) + " / L "
                + ClinicFormatters.formatUnsigned(tests.getAmpLeft()) + "  " + safeNote(tests.getAmpNote()));
    }

    private TextView addFunctionRow(String title, View.OnClickListener minus, View.OnClickListener plus,
                                    MaterialButton extra1, MaterialButton extra2) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dp(10);
        row.setLayoutParams(rowParams);

        row.addView(createText(requireContext(), title, 16, requireContext().getColor(R.color.brand_text_primary), true));
        TextView valueView = createText(requireContext(), "-", 14, requireContext().getColor(R.color.brand_text_secondary), false);
        row.addView(valueView);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(6), 0, 0);
        actions.addView(createRowButton("-", minus));
        actions.addView(createRowButton("+", plus));
        if (extra1 != null) {
            actions.addView(extra1);
        }
        if (extra2 != null) {
            actions.addView(extra2);
        }
        row.addView(actions);
        layoutFunctionRows.addView(row);
        return valueView;
    }

    private MaterialButton createRowButton(String text, View.OnClickListener listener) {
        MaterialButton button = createActionButton(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.rightMargin = dp(6);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    private MaterialButton createExtraButton(String text, ValueAction action) {
        MaterialButton button = createActionButton(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.rightMargin = dp(6);
        button.setLayoutParams(params);
        button.setOnClickListener(v -> action.invoke(text));
        return button;
    }

    private MaterialButton createModeButton(View.OnClickListener listener) {
        MaterialButton button = createActionButton("");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.rightMargin = dp(6);
        button.setLayoutParams(params);
        button.setOnClickListener(listener);
        return button;
    }

    private void addChoiceChip(ChipGroup group, String text, String tag, ValueAction action) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setTag(tag);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !updatingUi) {
                action.invoke(tag);
            }
        });
        group.addView(chip);
    }

    private void checkChip(ChipGroup group, String tag) {
        updatingUi = true;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                ((Chip) child).setChecked(TextUtils.equals(String.valueOf(child.getTag()), tag));
            }
        }
        updatingUi = false;
    }

    private void showChartHelp() {
        VisionChart chart = findChart(session == null ? null : session.getSelectedChartId());
        if (chart == null) {
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(chart.getTitle())
                .setMessage(chart.getSubtitle() + "\n\n" + chart.getDescription())
                .setPositiveButton("返回", null)
                .show();
    }

    private VisionChart findChart(String chartId) {
        for (VisionChart chart : chartList) {
            if (TextUtils.equals(chart.getId(), chartId)) {
                return chart;
            }
        }
        return chartList.isEmpty() ? null : chartList.get(0);
    }

    private String resolveProgramTitle(String programId) {
        for (ExamProgram program : programList) {
            if (TextUtils.equals(program.getId(), programId)) {
                return program.getTitle();
            }
        }
        return "未选择程序";
    }

    private String buildLens(LensMeasurement measurement) {
        return "S " + ClinicFormatters.formatSigned(measurement.getSph())
                + "  C " + ClinicFormatters.formatSigned(measurement.getCyl())
                + "  A " + ClinicFormatters.formatAxis(measurement.getAxis())
                + "  ADD " + ClinicFormatters.formatUnsigned(measurement.getAdd())
                + "  VA " + ClinicFormatters.formatUnsigned(measurement.getVa());
    }

    private String safeNote(String note) {
        return TextUtils.isEmpty(note) ? "" : "| " + note;
    }

    private String readText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private interface ValueAction {
        void invoke(String value);
    }
}
