package com.wifi.optometry.ui.device;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.wifi.lib.baseui.BaseVBActivity;
import com.wifi.optometry.communication.device.DeviceHistoryStore;
import com.wifi.optometry.R;
import com.wifi.optometry.databinding.ActivityDeviceHistoryBinding;

import java.util.List;

public class DeviceHistoryActivity extends BaseVBActivity<ActivityDeviceHistoryBinding> {
    public static final String EXTRA_DEVICE_ID = "extra_device_id";

    private DeviceHistoryStore deviceHistoryStore;
    private String deviceId;

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        deviceHistoryStore = DeviceHistoryStore.getInstance(this);
        deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, "未找到设备标识", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setSupportActionBar(binding.topAppBar);
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
        binding.topAppBar.setTitle("设备记录");
        binding.rgFilter.setOnCheckedChangeListener((group, checkedId) -> renderHistory());
        renderHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderHistory();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void renderHistory() {
        DeviceHistoryStore.DeviceSummary summary = deviceHistoryStore.getDeviceSummary(deviceId);
        if (summary == null) {
            binding.tvDeviceTitle.setText(deviceId);
            binding.tvDeviceSummary.setText("暂无该设备记录");
            binding.tvLogs.setText("暂无记录");
            return;
        }

        binding.tvDeviceTitle.setText(summary.getPrimaryLabel());
        binding.tvDeviceSummary.setText(buildSummaryText(summary));
        binding.tvLogs.setText(buildLogText(deviceHistoryStore.getLogs(deviceId, resolveFilter())));
    }

    private DeviceHistoryStore.LogFilter resolveFilter() {
        int checkedId = binding.rgFilter.getCheckedRadioButtonId();
        if (checkedId == R.id.rbCommunicationOnly) {
            return DeviceHistoryStore.LogFilter.COMMUNICATION;
        }
        if (checkedId == R.id.rbConnectionOnly) {
            return DeviceHistoryStore.LogFilter.CONNECTION;
        }
        return DeviceHistoryStore.LogFilter.ALL;
    }

    private String buildSummaryText(DeviceHistoryStore.DeviceSummary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("状态：")
                .append(summary.isCurrentlyConnected() ? "在线" : "离线");

        if (summary.getLastSeenAt() > 0) {
            builder.append("\n最近活动：")
                    .append(DeviceHistoryStore.formatTimestamp(summary.getLastSeenAt()));
        }

        if (!TextUtils.isEmpty(summary.getLastKnownIp())) {
            builder.append("\n最近模块端点：")
                    .append(summary.getLastKnownIp());
            if (summary.getLastKnownPort() > 0) {
                builder.append(":").append(summary.getLastKnownPort());
            }
        }

        if (!TextUtils.isEmpty(summary.getLastLocalIp())) {
            builder.append("\n最近手机监听：")
                    .append(summary.getLastLocalIp());
            if (summary.getLastLocalPort() > 0) {
                builder.append(":").append(summary.getLastLocalPort());
            }
        }

        builder.append("\n通信记录：")
                .append(summary.getCommunicationCount())
                .append(" 条")
                .append("\n连接记录：")
                .append(summary.getConnectionCount())
                .append(" 条");
        return builder.toString();
    }

    private String buildLogText(List<DeviceHistoryStore.DeviceLogEntry> logs) {
        if (logs.isEmpty()) {
            return "暂无记录";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < logs.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append(logs.get(index).formatForDisplay());
        }
        return builder.toString();
    }
}
