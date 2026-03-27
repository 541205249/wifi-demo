package com.example.wifidemo.sample.brvah.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.example.wifidemo.clinic.model.ReportRecord;
import com.example.wifidemo.databinding.ItemBrvahReportBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportRecordAdapter extends BaseQuickAdapter<ReportRecord, BindingHolder<ItemBrvahReportBinding>> {
    public interface OnReportActionClickListener {
        void onActionClick(@NonNull ReportRecord item);
    }

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
    private OnReportActionClickListener onReportActionClickListener;

    public void setOnReportActionClickListener(OnReportActionClickListener onReportActionClickListener) {
        this.onReportActionClickListener = onReportActionClickListener;
    }

    @NonNull
    @Override
    protected BindingHolder<ItemBrvahReportBinding> onCreateViewHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent,
            int viewType
    ) {
        return new BindingHolder<>(ItemBrvahReportBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull BindingHolder<ItemBrvahReportBinding> holder, int position, ReportRecord item) {
        String metric = item.getMetrics().isEmpty()
                ? "暂无附加指标"
                : item.getMetrics().get(0).getGroupTitle() + " · " + item.getMetrics().get(0).getResultValue();
        ItemBrvahReportBinding binding = holder.binding;
        binding.tvReportPatient.setText(item.getPatientName());
        binding.tvReportProgram.setText(item.getProgramName() + " · " + timeFormat.format(new Date(item.getCreatedAt())));
        binding.tvReportVision.setText(item.getVisionSummary());
        binding.tvReportPrescription.setText(item.getPrescriptionSummary());
        binding.tvReportMetric.setText(metric);
        binding.btnReportAction.setText("分享");
        binding.btnReportAction.setOnClickListener(v -> {
            if (onReportActionClickListener != null) {
                onReportActionClickListener.onActionClick(item);
            }
        });
    }
}
