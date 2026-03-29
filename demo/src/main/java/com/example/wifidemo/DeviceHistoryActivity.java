package com.example.wifidemo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.wifidemo.device.DeviceHistoryStore;
import com.wifi.lib.utils.Toasty;

import java.util.List;

public class DeviceHistoryActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE_ID = "extra_device_id";

    private TextView tvDeviceTitle;
    private TextView tvDeviceSummary;
    private RadioGroup rgFilter;
    private TextView tvLogs;

    private DeviceHistoryStore deviceHistoryStore;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_history);

        deviceHistoryStore = DeviceHistoryStore.getInstance(this);
        deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        if (TextUtils.isEmpty(deviceId)) {
            Toasty.showShort("未找到设备标识");
            finish();
            return;
        }

        initViews();
        initPageChrome();
        bindFilter();
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

    private void initViews() {
        tvDeviceTitle = findViewById(R.id.tvDeviceTitle);
        tvDeviceSummary = findViewById(R.id.tvDeviceSummary);
        rgFilter = findViewById(R.id.rgFilter);
        tvLogs = findViewById(R.id.tvLogs);
    }

    private void initPageChrome() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设备记录");
        }
    }

    private void bindFilter() {
        rgFilter.setOnCheckedChangeListener((group, checkedId) -> renderHistory());
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
        tvDeviceTitle.setText(deviceId);
        tvDeviceSummary.setText("暂无该设备记录");
        tvLogs.setText("暂无记录");
    }

    private void renderSummary(DeviceHistoryStore.DeviceSummary summary) {
        tvDeviceTitle.setText(summary.getPrimaryLabel());
        tvDeviceSummary.setText(buildSummaryText(summary));
    }

    private DeviceHistoryStore.LogFilter resolveFilter() {
        int checkedId = rgFilter.getCheckedRadioButtonId();
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
        tvLogs.setText(buildLogText(logs));
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
