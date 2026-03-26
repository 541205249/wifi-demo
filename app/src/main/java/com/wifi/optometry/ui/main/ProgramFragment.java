package com.wifi.optometry.ui.main;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wifi.optometry.R;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;

import java.util.ArrayList;
import java.util.List;

public class ProgramFragment extends BaseClinicFragment {
    private TextView tvCurrentProgram;
    private TextView tvCurrentStep;
    private LinearLayout layoutPrograms;
    private LinearLayout layoutSteps;

    private final List<ExamProgram> programList = new ArrayList<>();
    private ExamSession session;
    private ExamStep currentStepData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_program, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindSharedViewModel();
        tvCurrentProgram = view.findViewById(R.id.tvCurrentProgram);
        tvCurrentStep = view.findViewById(R.id.tvCurrentStep);
        layoutPrograms = view.findViewById(R.id.layoutPrograms);
        layoutSteps = view.findViewById(R.id.layoutProgramSteps);

        view.findViewById(R.id.btnAddCustomStep).setOnClickListener(v -> showCustomStepDialog());

        clinicViewModel.getPrograms().observe(getViewLifecycleOwner(), programs -> {
            programList.clear();
            if (programs != null) {
                programList.addAll(programs);
            }
            renderPrograms();
            renderSteps();
        });
        clinicViewModel.getSession().observe(getViewLifecycleOwner(), examSession -> {
            session = examSession;
            renderHeader();
            renderPrograms();
            renderSteps();
        });
        clinicViewModel.getCurrentStep().observe(getViewLifecycleOwner(), examStep -> {
            currentStepData = examStep;
            renderHeader();
            renderSteps();
        });
    }

    private void renderHeader() {
        ExamProgram currentProgram = findProgram(session == null ? null : session.getCurrentProgramId());
        tvCurrentProgram.setText(currentProgram == null
                ? "当前程序：未选择"
                : "当前程序：" + currentProgram.getTitle() + "\n" + currentProgram.getDescription());
        tvCurrentStep.setText(currentStepData == null
                ? "当前步骤：暂无"
                : "当前步骤：" + currentStepData.getTitle() + "\n"
                + currentStepData.getDescription() + "\n"
                + "视标：" + currentStepData.getChartId() + " | 眼别：" + currentStepData.getEyeScope().name()
                + " | 距离：" + currentStepData.getDistanceMode().name()
                + " | 调节项：" + currentStepData.getTargetField());
    }

    private void renderPrograms() {
        layoutPrograms.removeAllViews();
        for (ExamProgram program : programList) {
            com.google.android.material.card.MaterialCardView cardView = createCard();
            if (session != null && TextUtils.equals(program.getId(), session.getCurrentProgramId())) {
                cardView.setStrokeWidth(dp(2));
                cardView.setStrokeColor(requireContext().getColor(R.color.brand_secondary));
            }
            LinearLayout content = createCardContent(cardView);
            content.addView(createText(requireContext(), program.getTitle(), 18,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(), program.getSummary(), 14,
                    requireContext().getColor(R.color.brand_secondary), true));
            content.addView(createText(requireContext(), program.getDescription(), 14,
                    requireContext().getColor(R.color.brand_text_secondary), false));
            content.addView(createText(requireContext(), "步骤数：" + program.getSteps().size(), 13,
                    requireContext().getColor(R.color.brand_text_secondary), false));

            com.google.android.material.button.MaterialButton selectButton = createActionButton("设为当前程序");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(12);
            selectButton.setLayoutParams(params);
            selectButton.setOnClickListener(v -> clinicViewModel.selectProgram(program.getId()));
            content.addView(selectButton);
            layoutPrograms.addView(cardView);
        }
    }

    private void renderSteps() {
        layoutSteps.removeAllViews();
        ExamProgram currentProgram = findProgram(session == null ? null : session.getCurrentProgramId());
        if (currentProgram == null) {
            return;
        }
        for (int index = 0; index < currentProgram.getSteps().size(); index++) {
            ExamStep step = currentProgram.getSteps().get(index);
            com.google.android.material.card.MaterialCardView cardView = createCard();
            if (session != null && index == session.getCurrentStepIndex()) {
                cardView.setStrokeWidth(dp(2));
                cardView.setStrokeColor(requireContext().getColor(R.color.brand_primary));
            }
            LinearLayout content = createCardContent(cardView);
            content.addView(createText(requireContext(), (index + 1) + ". " + step.getTitle(), 17,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(), step.getDescription(), 14,
                    requireContext().getColor(R.color.brand_text_secondary), false));
            content.addView(createText(requireContext(),
                    "视标：" + step.getChartId()
                            + " | 距离：" + step.getDistanceMode().name()
                            + " | 眼别：" + step.getEyeScope().name()
                            + " | 数据源：" + step.getSubjectSource()
                            + " | 目标：" + step.getTargetField(),
                    13,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false));
            content.addView(createText(requireContext(),
                    "雾视：" + step.getFogOption() + " | 近灯：" + step.getNearLightOption()
                            + " | 视功能：" + step.getFunctionLabel()
                            + (TextUtils.isEmpty(step.getNote()) ? "" : "\n说明：" + step.getNote()),
                    13,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false));
            layoutSteps.addView(cardView);
        }
    }

    private void showCustomStepDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("输入自定义步骤说明");
        input.setMinLines(3);
        input.setPadding(dp(12), dp(12), dp(12), dp(12));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("新增自定义步骤")
                .setView(input)
                .setPositiveButton("新增", (dialog, which) -> clinicViewModel.appendCustomProgramStep(
                        input.getText() == null ? "" : input.getText().toString().trim()))
                .setNegativeButton("取消", null)
                .show();
    }

    private ExamProgram findProgram(String programId) {
        for (ExamProgram program : programList) {
            if (TextUtils.equals(program.getId(), programId)) {
                return program;
            }
        }
        return null;
    }
}
