package com.wifi.optometry.ui.device;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wifi.lib.baseui.BaseVBActivity;
import com.wifi.lib.utils.Toasty;
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
        applyWindowInsets();
        deviceHistoryStore = DeviceHistoryStore.getInstance(this);
        deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        if (TextUtils.isEmpty(deviceId)) {
            Toasty.showShort("未找到设备标识");
            finish();
            return;
        }

        initPageChrome();
        bindFilter();
        renderHistory();
    }

    private void applyWindowInsets() {
        final int topAppBarPaddingTop = binding.topAppBar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            binding.topAppBar.setPadding(
                    binding.topAppBar.getPaddingLeft(),
                    topAppBarPaddingTop + topInset,
                    binding.topAppBar.getPaddingRight(),
                    binding.topAppBar.getPaddingBottom()
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
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

    private void initPageChrome() {
        setSupportActionBar(binding.topAppBar);
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
        binding.topAppBar.setTitle("设备记录");
    }

    private void bindFilter() {
        binding.rgFilter.setOnCheckedChangeListener((group, checkedId) -> renderHistory());
    }

    private void renderHistory() {
        DeviceHistoryStore.DeviceSummary summary = deviceHistoryStore.getDeviceSummary(deviceId);
        if (summary == null) {
            renderEmptyHistory();
            return;
        }

        renderSummary(summary);
        renderLogs(deviceHistoryStore.getLogs(deviceId, resolveFilter()));
    }

    private void renderEmptyHistory() {
        binding.tvDeviceTitle.setText(deviceId);
        binding.tvDeviceSummary.setText("暂无该设备记录");
        binding.tvLogs.setText("暂无记录");
    }

    private void renderSummary(DeviceHistoryStore.DeviceSummary summary) {
        binding.tvDeviceTitle.setText(summary.getPrimaryLabel());
        binding.tvDeviceSummary.setText(buildSummaryText(summary));
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

    private void renderLogs(List<DeviceHistoryStore.DeviceLogEntry> logs) {
        binding.tvLogs.setText(buildLogText(logs));
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
