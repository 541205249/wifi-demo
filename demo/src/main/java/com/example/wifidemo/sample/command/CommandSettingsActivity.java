package com.example.wifidemo.sample.command;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.TcpServerService;
import com.example.wifidemo.databinding.ActivityCommandSettingsBinding;
import com.example.wifidemo.device.DeviceManager;
import com.wifi.lib.command.CommandViewHelper;
import com.wifi.lib.command.OutboundCommand;
import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.profile.OptometryCommandCatalogs;
import com.wifi.lib.command.profile.OptometryCommandCodes;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseMvvmActivity;
import com.wifi.lib.utils.Toasty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandSettingsActivity extends BaseMvvmActivity<ActivityCommandSettingsBinding, CommandSettingsViewModel> {
    private static final String TAG = "CommandSettingsAct";

    private final List<String> connectedDeviceIds = new ArrayList<>();

    @Nullable
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    @Nullable
    private TcpServerService tcpServerService;
    private boolean isServiceBound;
    private ArrayAdapter<String> clientAdapter;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TcpServerService.TcpServerBinder binder = (TcpServerService.TcpServerBinder) service;
            tcpServerService = binder.getService();
            isServiceBound = true;
            trace("命令设置页已绑定 TCP 服务");
            setupServiceListener();
            updateServiceState();
            updateClientList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            trace("命令设置页 TCP 服务连接断开");
            tcpServerService = null;
            isServiceBound = false;
            updateServiceState();
            updateClientList();
        }
    };

    @NonNull
    @Override
    protected Class<CommandSettingsViewModel> getViewModelClass() {
        return CommandSettingsViewModel.class;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle("命令设置");
        initDocumentLauncher();
        initClientSpinner();
        bindCommandButtons();
        binding.btnLoadBuiltInSample.setOnClickListener(v -> viewModel.loadBuiltInSample());
        binding.btnPickCommandTable.setOnClickListener(v -> openDocumentPicker());
        binding.btnReloadCommandTable.setOnClickListener(v -> viewModel.reloadLastLoadedTable());
        startAndBindService();
        updateServiceState();
    }

    @Override
    protected void observeUi() {
        super.observeUi();
        viewModel.getLoadedFileLiveData().observe(this, value -> binding.tvLoadedFile.setText(value));
        viewModel.getTableSummaryLiveData().observe(this, value -> binding.tvTableSummary.setText(value));
        viewModel.getValidationLiveData().observe(this, value -> binding.tvValidationSummary.setText(value));
        viewModel.getConsoleLiveData().observe(this, value -> binding.tvCommandConsole.setText(value));
    }

    @Override
    protected void loadData() {
        if (viewModel.getLastLoadedUri() != null) {
            viewModel.reloadLastLoadedTable();
            return;
        }
        viewModel.loadBuiltInSample();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tcpServerService != null) {
            tcpServerService.setOnMessageListener(null);
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private void initDocumentLauncher() {
        if (openDocumentLauncher != null) {
            return;
        }
        openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleDocumentPicked);
    }

    private void initClientSpinner() {
        clientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCommandClients.setAdapter(clientAdapter);
    }

    private void bindCommandButtons() {
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnQueryModuleInfo,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_QUERY_MODULE_INFO),
                v -> sendToTarget(OptometryCommandCodes.CODE_QUERY_MODULE_INFO, null)
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnSwitchAutoMode,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_SWITCH_AUTO_MODE),
                v -> sendToTarget(
                        OptometryCommandCodes.CODE_SWITCH_AUTO_MODE,
                        viewModel.createModeArgument("AUTO")
                )
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnSwitchManualMode,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_SWITCH_MANUAL_MODE),
                v -> sendToTarget(
                        OptometryCommandCodes.CODE_SWITCH_MANUAL_MODE,
                        viewModel.createModeArgument("MANUAL")
                )
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnStartOptometry,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_START_OPTOMETRY),
                v -> sendToTarget(OptometryCommandCodes.CODE_START_OPTOMETRY, null)
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnStopOptometry,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_STOP_OPTOMETRY),
                v -> sendToTarget(OptometryCommandCodes.CODE_STOP_OPTOMETRY, null)
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnMockModuleInfo,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_REPORT_MODULE_INFO),
                v -> viewModel.simulateIncomingMessage("模拟模块", "INFO+HC25,FW=1.0.0")
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnMockDeviceStatus,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_REPORT_DEVICE_STATUS),
                v -> viewModel.simulateIncomingMessage("模拟模块", "STATUS+READY")
        );
        CommandViewHelper.bindClickWithCodeHint(
                binding.btnMockOptometryResult,
                OptometryCommandCatalogs.requireReservation(OptometryCommandCodes.CODE_REPORT_OPTOMETRY_RESULT),
                v -> viewModel.simulateIncomingMessage("模拟模块", "RESULT+SPH=-1.25,CYL=-0.50,AXIS=180")
        );
    }

    private void openDocumentPicker() {
        if (openDocumentLauncher == null) {
            Toasty.showShort("文档选择器尚未准备好");
            return;
        }
        openDocumentLauncher.launch(new String[]{"text/*", "application/vnd.ms-excel", "application/csv"});
    }

    private void handleDocumentPicked(@Nullable Uri uri) {
        if (uri == null) {
            Toasty.showShort("未选择编码表文档");
            return;
        }

        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        trace("已选择编码表文档 uri=" + uri);
        viewModel.loadCommandTable(uri);
    }

    private void startAndBindService() {
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void setupServiceListener() {
        if (tcpServerService == null) {
            return;
        }
        tcpServerService.setOnMessageListener(new TcpServerService.OnMessageListener() {
            @Override
            public void onMessageReceived(String clientId, String message) {
                runOnUiThread(() -> {
                    trace("收到模块原始消息，clientId=" + clientId + ", raw=" + message);
                    viewModel.onIncomingMessage(clientId, message);
                    updateClientList();
                });
            }

            @Override
            public void onClientConnected(String clientId) {
                runOnUiThread(() -> {
                    trace("模块接入，clientId=" + clientId);
                    updateServiceState();
                    updateClientList();
                });
            }

            @Override
            public void onClientIdentityResolved(String clientId, String macAddress) {
                runOnUiThread(() -> {
                    trace("模块身份已识别，clientId=" + clientId + ", mac=" + macAddress);
                    updateClientList();
                });
            }

            @Override
            public void onClientDisconnected(String clientId) {
                runOnUiThread(() -> {
                    trace("模块断开，clientId=" + clientId);
                    updateServiceState();
                    updateClientList();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    trace("TCP 服务返回错误: " + error);
                    Toasty.showShort(error);
                    updateServiceState();
                });
            }

            @Override
            public void onServerStarted(String ipAddress) {
                runOnUiThread(() -> {
                    trace("TCP 服务启动，ip=" + ipAddress);
                    updateServiceState();
                });
            }

            @Override
            public void onProtocolEvent(String clientId, @NonNull ProtocolInboundEvent event) {
                runOnUiThread(() -> viewModel.onProtocolEvent(clientId, event));
            }
        });
    }

    private void updateServiceState() {
        if (!isServiceBound || tcpServerService == null) {
            binding.tvServiceState.setText("TCP 服务状态: 未绑定");
            return;
        }

        String ipAddress = tcpServerService.getLocalIpAddress();
        int connectedCount = tcpServerService.getConnectedClientCount();
        binding.tvServiceState.setText(
                "TCP 服务状态: " + (tcpServerService.isServerRunning() ? "运行中" : "未运行")
                        + "\n监听地址: " + (TextUtils.isEmpty(ipAddress) ? "未获取" : ipAddress)
                        + "\n在线模块: " + connectedCount + " 个"
        );
    }

    private void updateClientList() {
        connectedDeviceIds.clear();
        clientAdapter.clear();

        if (!isServiceBound || tcpServerService == null) {
            clientAdapter.notifyDataSetChanged();
            return;
        }

        DeviceManager.DeviceConnection[] connectedDevices = tcpServerService.getConnectedDevices();
        for (DeviceManager.DeviceConnection device : connectedDevices) {
            connectedDeviceIds.add(device.getDeviceId());
            clientAdapter.add(device.getSelectionLabel());
        }
        clientAdapter.notifyDataSetChanged();
        updateServiceState();
    }

    private void sendToTarget(@NonNull String code, @Nullable Map<String, String> arguments) {
        if (!isServiceBound || tcpServerService == null || !tcpServerService.isServerRunning()) {
            Toasty.showShort("TCP 服务未连接或未启动");
            return;
        }

        OutboundCommand outboundCommand;
        if (binding.switchBroadcastAll.isChecked()) {
            outboundCommand = tcpServerService.broadcastCommandByCode(code, arguments);
            if (outboundCommand == null) {
                Toasty.showShort("按编码广播发送失败");
                return;
            }
            viewModel.onCommandSent("全部在线模块", outboundCommand);
            return;
        }

        int selectedPosition = binding.spinnerCommandClients.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= connectedDeviceIds.size()) {
            Toasty.showShort("请选择一个在线模块，或切换为广播发送");
            return;
        }

        String clientId = connectedDeviceIds.get(selectedPosition);
        String targetLabel = tcpServerService.getDeviceDisplayLabel(clientId);
        outboundCommand = tcpServerService.sendCommandByCodeToClient(clientId, code, arguments);
        if (outboundCommand == null) {
            Toasty.showShort("按编码定向发送失败");
            return;
        }
        viewModel.onCommandSent(targetLabel, outboundCommand);
    }

    private void trace(@NonNull String message) {
        DLog.i(TAG, message);
    }
}
