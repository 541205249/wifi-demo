package com.example.wifidemo.sample.brvah.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.example.wifidemo.R;
import com.example.wifidemo.clinic.model.KnownDeviceSummary;
import com.example.wifidemo.databinding.ItemBrvahDeviceBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KnownDeviceAdapter extends BaseQuickAdapter<KnownDeviceSummary, BindingHolder<ItemBrvahDeviceBinding>> {
    public interface OnDeviceActionClickListener {
        void onActionClick(@NonNull KnownDeviceSummary item);
    }

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnDeviceActionClickListener onDeviceActionClickListener;

    public void setOnDeviceActionClickListener(OnDeviceActionClickListener onDeviceActionClickListener) {
        this.onDeviceActionClickListener = onDeviceActionClickListener;
    }

    @NonNull
    @Override
    protected BindingHolder<ItemBrvahDeviceBinding> onCreateViewHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent,
            int viewType
    ) {
        return new BindingHolder<>(ItemBrvahDeviceBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull BindingHolder<ItemBrvahDeviceBinding> holder, int position, KnownDeviceSummary item) {
        ItemBrvahDeviceBinding binding = holder.binding;
        binding.tvDeviceName.setText(item.getDisplayLabel());
        binding.tvDeviceMac.setText(item.getDeviceId());
        binding.tvDeviceIp.setText("IP " + item.getLastKnownIp() + " · 最近在线 " + timeFormat.format(new Date(item.getLastSeenAt())));
        binding.tvDeviceStats.setText("通信 " + item.getCommunicationCount() + " 次 · 连接 " + item.getConnectionCount() + " 次");
        binding.tvDeviceState.setText(item.isConnected() ? "在线" : "离线");
        binding.btnDeviceAction.setText(item.isConnected() ? "查看记录" : "查看档案");
        binding.tvDeviceState.setBackgroundResource(item.isConnected() ? R.drawable.bg_brvah_tag_success : R.drawable.bg_brvah_tag_warning);
        binding.tvDeviceState.setTextColor(ContextCompat.getColor(
                binding.getRoot().getContext(),
                item.isConnected() ? R.color.brand_success : R.color.brand_warning
        ));
        binding.btnDeviceAction.setOnClickListener(v -> {
            if (onDeviceActionClickListener != null) {
                onDeviceActionClickListener.onActionClick(item);
            }
        });
    }
}
