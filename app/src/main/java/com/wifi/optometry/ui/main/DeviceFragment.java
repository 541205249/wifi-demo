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

import com.google.android.material.card.MaterialCardView;
import com.wifi.optometry.R;
import com.wifi.optometry.communication.device.DeviceHistoryStore;
import com.wifi.optometry.domain.model.ConnectedDeviceInfo;
import com.wifi.optometry.domain.model.DeviceUiState;
import com.wifi.optometry.domain.model.KnownDeviceSummary;
import com.wifi.optometry.ui.shared.SimpleTextWatcher;
import com.wifi.optometry.util.ClinicFormatters;

import java.util.List;

public class DeviceFragment extends BaseClinicFragment {
    private TextView tvServerState;
    private TextView tvServerIp;
    private TextView tvServerPort;
    private EditText etPendingMessage;
    private TextView tvLogOutput;
    private LinearLayout layoutConnectedDevices;
    private LinearLayout layoutKnownDevices;
    private DeviceUiState currentState;
    private boolean updatingMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindSharedViewModel();

        tvServerState = view.findViewById(R.id.tvServerState);
        tvServerIp = view.findViewById(R.id.tvServerIp);
        tvServerPort = view.findViewById(R.id.tvServerPort);
        etPendingMessage = view.findViewById(R.id.etPendingMessage);
        tvLogOutput = view.findViewById(R.id.tvLogOutput);
        layoutConnectedDevices = view.findViewById(R.id.layoutConnectedDevices);
        layoutKnownDevices = view.findViewById(R.id.layoutKnownDevices);

        view.findViewById(R.id.btnToggleServer).setOnClickListener(v -> {
            if (currentState != null && currentState.isServerRunning()) {
                clinicViewModel.stopServer();
            } else {
                clinicViewModel.startServer();
            }
        });
        view.findViewById(R.id.btnRefreshDeviceState).setOnClickListener(v -> clinicViewModel.refreshDeviceState());
        view.findViewById(R.id.btnBroadcastMessage).setOnClickListener(v -> {
            String message = readPendingMessage();
            if (TextUtils.isEmpty(message)) {
                showToast("请输入要发送的内容");
                return;
            }
            clinicViewModel.broadcastMessage(message);
        });
        view.findViewById(R.id.btnSendToSelected).setOnClickListener(v -> {
            String message = readPendingMessage();
            if (TextUtils.isEmpty(message)) {
                showToast("请输入要发送的内容");
                return;
            }
            if (currentState == null || TextUtils.isEmpty(currentState.getSelectedClientId())) {
                showToast("请先选择一个在线模块");
                return;
            }
            clinicViewModel.sendMessageToSelectedClient(message);
        });
        etPendingMessage.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!updatingMessage) {
                    clinicViewModel.updatePendingMessage(s == null ? "" : s.toString());
                }
            }
        });

        clinicViewModel.getDeviceUiState().observe(getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(DeviceUiState state) {
        currentState = state;
        if (state == null) {
            return;
        }
        tvServerState.setText(state.isServerRunning() ? "服务状态：运行中" : "服务状态：已停止");
        tvServerIp.setText("手机监听地址：" + (TextUtils.isEmpty(state.getLocalIp()) ? "未获取" : state.getLocalIp()));
        tvServerPort.setText("端口：" + state.getServerPort());

        if (!etPendingMessage.hasFocus()) {
            String pending = state.getPendingMessage() == null ? "" : state.getPendingMessage();
            if (!TextUtils.equals(pending, etPendingMessage.getText())) {
                updatingMessage = true;
                etPendingMessage.setText(pending);
                etPendingMessage.setSelection(pending.length());
                updatingMessage = false;
            }
        }

        TextView btnToggle = requireView().findViewById(R.id.btnToggleServer);
        btnToggle.setText(state.isServerRunning() ? "停止监听" : "启动监听");

        renderConnectedDevices(state.getConnectedDevices(), state.getSelectedClientId());
        renderKnownDevices(state.getKnownDevices());
        renderLogs(state.getLogs());
    }

    private void renderConnectedDevices(List<ConnectedDeviceInfo> devices, String selectedClientId) {
        layoutConnectedDevices.removeAllViews();
        if (devices == null || devices.isEmpty()) {
            layoutConnectedDevices.addView(createText(
                    requireContext(),
                    "当前没有在线模块，服务启动后等待模块主动连接。",
                    14,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false
            ));
            return;
        }

        for (ConnectedDeviceInfo device : devices) {
            MaterialCardView card = createCard();
            if (TextUtils.equals(device.getClientId(), selectedClientId)) {
                card.setStrokeWidth(dp(2));
                card.setStrokeColor(requireContext().getColor(R.color.brand_primary));
            }
            LinearLayout content = createCardContent(card);
            content.addView(createText(requireContext(), device.getDisplayLabel(), 17,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(),
                    "MAC: " + (TextUtils.isEmpty(device.getMacAddress()) ? "识别中" : device.getMacAddress())
                            + "\nIP: " + device.getIpAddress() + ":" + device.getPort()
                            + "\n连接时间: " + ClinicFormatters.formatTimestamp(device.getConnectedAt()),
                    14,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false));

            LinearLayout actions = new LinearLayout(requireContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(12), 0, 0);

            com.google.android.material.button.MaterialButton btnSelect = createActionButton("选中发送");
            btnSelect.setOnClickListener(v -> clinicViewModel.selectConnectedDevice(device.getClientId()));
            actions.addView(btnSelect);

            com.google.android.material.button.MaterialButton btnSend = createActionButton("发送当前消息");
            LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            sendParams.leftMargin = dp(8);
            btnSend.setLayoutParams(sendParams);
            btnSend.setOnClickListener(v -> {
                String message = readPendingMessage();
                if (TextUtils.isEmpty(message)) {
                    showToast("请输入要发送的内容");
                    return;
                }
                clinicViewModel.sendMessageToClient(device.getClientId(), message);
            });
            actions.addView(btnSend);

            com.google.android.material.button.MaterialButton btnHistory = createActionButton("查看记录");
            LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            historyParams.leftMargin = dp(8);
            btnHistory.setLayoutParams(historyParams);
            btnHistory.setOnClickListener(v -> mainActivity().openDeviceHistory(
                    TextUtils.isEmpty(device.getMacAddress()) ? device.getClientId() : device.getMacAddress()));
            actions.addView(btnHistory);

            content.addView(actions);
            layoutConnectedDevices.addView(card);
        }
    }

    private void renderKnownDevices(List<KnownDeviceSummary> devices) {
        layoutKnownDevices.removeAllViews();
        if (devices == null || devices.isEmpty()) {
            layoutKnownDevices.addView(createText(
                    requireContext(),
                    "还没有设备台账。模块连上并返回 MAC 后会自动建档。",
                    14,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false
            ));
            return;
        }

        for (KnownDeviceSummary device : devices) {
            MaterialCardView card = createCard();
            LinearLayout content = createCardContent(card);
            content.addView(createText(requireContext(), device.getDisplayLabel(), 17,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(),
                    "最近 IP: " + (TextUtils.isEmpty(device.getLastKnownIp()) ? "未记录" : device.getLastKnownIp())
                            + "\n最近活动: " + DeviceHistoryStore.formatTimestamp(device.getLastSeenAt())
                            + "\n通信记录: " + device.getCommunicationCount()
                            + " 条  |  连接记录: " + device.getConnectionCount() + " 条",
                    14,
                    requireContext().getColor(R.color.brand_text_secondary),
                    false));

            com.google.android.material.button.MaterialButton btnHistory = createActionButton("打开记录页");
            btnHistory.setOnClickListener(v -> mainActivity().openDeviceHistory(device.getDeviceId()));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(12);
            btnHistory.setLayoutParams(params);
            content.addView(btnHistory);

            layoutKnownDevices.addView(card);
        }
    }

    private void renderLogs(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            tvLogOutput.setText("暂无运行日志");
            return;
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(logs.size(), 30);
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(logs.get(index));
        }
        tvLogOutput.setText(builder.toString());
    }

    private String readPendingMessage() {
        return etPendingMessage.getText() == null ? "" : etPendingMessage.getText().toString().trim();
    }
}
