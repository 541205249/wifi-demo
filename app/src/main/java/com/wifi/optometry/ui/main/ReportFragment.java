package com.wifi.optometry.ui.main;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.wifi.optometry.R;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ReportRecord;
import com.wifi.optometry.domain.model.VisualFunctionMetric;
import com.wifi.optometry.util.ClinicFormatters;

import java.util.ArrayList;
import java.util.List;

public class ReportFragment extends BaseClinicFragment {
    private TextView tvCurrentReportHeader;
    private TextView tvVisionSummary;
    private TextView tvPrescriptionSummary;
    private TextView tvQrPayload;
    private LinearLayout layoutMetrics;
    private LinearLayout layoutHistory;

    private ExamSession session;
    private String qrPayload;
    private final List<ExamProgram> programs = new ArrayList<>();
    private final List<VisualFunctionMetric> metrics = new ArrayList<>();
    private final List<ReportRecord> reportHistory = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindSharedViewModel();

        tvCurrentReportHeader = view.findViewById(R.id.tvCurrentReportHeader);
        tvVisionSummary = view.findViewById(R.id.tvVisionSummary);
        tvPrescriptionSummary = view.findViewById(R.id.tvPrescriptionSummary);
        tvQrPayload = view.findViewById(R.id.tvQrPayload);
        layoutMetrics = view.findViewById(R.id.layoutMetrics);
        layoutHistory = view.findViewById(R.id.layoutHistory);

        view.findViewById(R.id.btnSaveReport).setOnClickListener(v -> {
            clinicViewModel.saveCurrentReport();
            showToast("当前验光结果已存入报告历史");
        });
        view.findViewById(R.id.btnImportLatestReport).setOnClickListener(v -> {
            clinicViewModel.importLatestReport();
            showToast("已导入最近一次报告到当前会话");
        });
        view.findViewById(R.id.btnShowQrPayload).setOnClickListener(v -> showQrDialog());
        view.findViewById(R.id.btnPrintPreview).setOnClickListener(v ->
                showToast("纯 WiFi 版本暂未接入打印机，已保留报告预览入口"));

        clinicViewModel.getSession().observe(getViewLifecycleOwner(), examSession -> {
            session = examSession;
            renderCurrentReport();
        });
        clinicViewModel.getPrograms().observe(getViewLifecycleOwner(), examPrograms -> {
            programs.clear();
            if (examPrograms != null) {
                programs.addAll(examPrograms);
            }
            renderCurrentReport();
        });
        clinicViewModel.getMetrics().observe(getViewLifecycleOwner(), visualFunctionMetrics -> {
            metrics.clear();
            if (visualFunctionMetrics != null) {
                metrics.addAll(visualFunctionMetrics);
            }
            renderMetrics();
        });
        clinicViewModel.getReports().observe(getViewLifecycleOwner(), reportRecords -> {
            reportHistory.clear();
            if (reportRecords != null) {
                reportHistory.addAll(reportRecords);
            }
            renderHistory();
        });
        clinicViewModel.getQrPayload().observe(getViewLifecycleOwner(), payload -> {
            qrPayload = payload;
            tvQrPayload.setText(TextUtils.isEmpty(payload) ? "暂无二维码内容" : payload);
        });
    }

    private void renderCurrentReport() {
        if (session == null) {
            return;
        }
        String patientName = session.getPatient() == null ? "未命名被测者" : session.getPatient().getDisplayName();
        String programName = resolveProgramTitle(session.getCurrentProgramId());
        tvCurrentReportHeader.setText("患者：" + patientName
                + "\n程序：" + programName
                + "\n生成时间：" + ClinicFormatters.formatTimestamp(System.currentTimeMillis()));
        tvVisionSummary.setText("视力\nR 远用 VA " + ClinicFormatters.formatUnsigned(session.getFinalRight().getVa())
                + " / L 远用 VA " + ClinicFormatters.formatUnsigned(session.getFinalLeft().getVa())
                + "\nR 近用 VA " + ClinicFormatters.formatUnsigned(session.getNearRight().getVa())
                + " / L 近用 VA " + ClinicFormatters.formatUnsigned(session.getNearLeft().getVa()));
        tvPrescriptionSummary.setText("处方\n"
                + "右眼：" + ClinicFormatters.buildLensSummary(session.getFinalRight())
                + "\n左眼：" + ClinicFormatters.buildLensSummary(session.getFinalLeft())
                + "\n备注：" + (TextUtils.isEmpty(session.getNote()) ? "无" : session.getNote()));
    }

    private void renderMetrics() {
        layoutMetrics.removeAllViews();
        if (metrics.isEmpty()) {
            layoutMetrics.addView(createText(requireContext(), "当前没有视功能指标。", 14,
                    requireContext().getColor(R.color.brand_text_secondary), false));
            return;
        }
        for (VisualFunctionMetric metric : metrics) {
            com.google.android.material.card.MaterialCardView cardView = createCard();
            LinearLayout content = createCardContent(cardView);
            content.addView(createText(requireContext(), metric.getGroupTitle() + " | " + metric.getItemTitle(), 16,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(),
                    metric.getMeasureLabel() + "：" + metric.getResultValue()
                            + "\n参考：" + metric.getReferenceValue()
                            + "\n状态：" + metric.getStatus()
                            + (TextUtils.isEmpty(metric.getNote()) ? "" : "\n记录：" + metric.getNote()),
                    14,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false));
            layoutMetrics.addView(cardView);
        }
    }

    private void renderHistory() {
        layoutHistory.removeAllViews();
        if (reportHistory.isEmpty()) {
            layoutHistory.addView(createText(requireContext(), "还没有保存过报告。", 14,
                    requireContext().getColor(R.color.brand_text_secondary), false));
            return;
        }
        int limit = Math.min(reportHistory.size(), 8);
        for (int index = 0; index < limit; index++) {
            ReportRecord record = reportHistory.get(index);
            com.google.android.material.card.MaterialCardView cardView = createCard();
            LinearLayout content = createCardContent(cardView);
            content.addView(createText(requireContext(), record.getPatientName() + " | " + record.getProgramName(), 16,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(),
                    "时间：" + ClinicFormatters.formatTimestamp(record.getCreatedAt())
                            + "\n视力：" + record.getVisionSummary()
                            + "\n处方：" + record.getPrescriptionSummary(),
                    14,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false));
            layoutHistory.addView(cardView);
        }
    }

    private void showQrDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("报告二维码内容")
                .setMessage(TextUtils.isEmpty(qrPayload) ? "暂无二维码内容" : qrPayload)
                .setPositiveButton("关闭", null)
                .show();
    }

    private String resolveProgramTitle(String programId) {
        for (ExamProgram program : programs) {
            if (TextUtils.equals(program.getId(), programId)) {
                return program.getTitle();
            }
        }
        return "未选择程序";
    }
}
