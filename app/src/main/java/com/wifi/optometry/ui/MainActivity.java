package com.wifi.optometry.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseMvvmActivity;
import com.wifi.optometry.communication.ServerConstance;
import com.wifi.optometry.communication.TcpServerService;
import com.wifi.optometry.communication.device.DeviceManager;
import com.wifi.optometry.databinding.ActivityMainBinding;
import com.wifi.optometry.domain.model.ConnectedDeviceInfo;
import com.wifi.optometry.ui.main.WorkbenchFragment;
import com.wifi.optometry.ui.state.ClinicViewModel;
import com.wifi.optometry.ui.state.DeviceServiceGateway;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseMvvmActivity<ActivityMainBinding, ClinicViewModel> implements DeviceServiceGateway {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1101;
    private static final String TAG = "MainActivity";

    private ClinicViewModel clinicViewModel;
    private TcpServerService tcpServerService;
    private boolean isServiceBound;
    private String localIpAddress;
    private final TcpServerService.OnMessageListener serviceMessageListener = new TcpServerService.OnMessageListener() {
        @Override
        public void onMessageReceived(String clientId, String message) {
            appendConsoleAndRefresh("收到 " + clientId + " 的消息: " + message);
        }

        @Override
        public void onClientConnected(String clientId) {
            appendConsoleAndRefresh("设备已接入: " + clientId);
        }

        @Override
        public void onClientIdentityResolved(String clientId, String macAddress) {
            appendConsoleAndRefresh("设备身份已解析: " + macAddress + " [" + clientId + "]");
        }

        @Override
        public void onClientDisconnected(String clientId) {
            appendConsoleAndRefresh("设备已断开: " + clientId);
        }

        @Override
        public void onError(String error) {
            appendConsoleAndRefresh("通信错误: " + error);
        }

        @Override
        public void onServerStarted(String ipAddress) {
            if (!TextUtils.isEmpty(ipAddress)) {
                localIpAddress = ipAddress;
            }
            appendConsoleAndRefresh("监听服务运行中，地址: "
                    + (TextUtils.isEmpty(getLocalIpAddress()) ? "未获取" : getLocalIpAddress())
                    + ":" + ServerConstance.SERVER_PORT);
        }

        @Override
        public void onProtocolEvent(String clientId, @NonNull ProtocolInboundEvent event) {
            appendConsoleAndRefresh("协议事件[" + clientId + "] " + event.getPayloadType().name()
                    + ": " + event.getRawMessage());
        }

        @Override
        public void onProtocolError(String clientId, String rawMessage, String errorMessage) {
            appendConsoleAndRefresh("协议解析失败[" + clientId + "]: " + errorMessage + " | " + rawMessage);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TcpServerService.TcpServerBinder binder = (TcpServerService.TcpServerBinder) service;
            tcpServerService = binder.getService();
            isServiceBound = true;
            if (!TextUtils.isEmpty(localIpAddress)) {
                tcpServerService.setLocalIpAddress(localIpAddress);
            }
            bindServiceCallbacks();
            clinicViewModel.setDeviceServiceGateway(MainActivity.this);
            clinicViewModel.appendDeviceConsole("WiFi 通信服务已连接");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            tcpServerService = null;
            clinicViewModel.refreshDeviceState();
            clinicViewModel.appendDeviceConsole("WiFi 通信服务已断开");
        }
    };

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        applyWindowInsets();
        checkNotificationPermission();
        requestBatteryOptimizationWhitelist();
        localIpAddress = resolveLocalIpAddress();

        clinicViewModel.setDeviceServiceGateway(this);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(binding.fragmentContainer.getId(), new WorkbenchFragment())
                    .commit();
        }
        startAndBindService();
    }

    private void applyWindowInsets() {
        final int paddingLeft = binding.getRoot().getPaddingLeft();
        final int paddingTop = binding.getRoot().getPaddingTop();
        final int paddingRight = binding.getRoot().getPaddingRight();
        final int paddingBottom = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            binding.getRoot().setPadding(
                    paddingLeft,
                    paddingTop + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top,
                    paddingRight,
                    paddingBottom + insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    @NonNull
    @Override
    protected Class<ClinicViewModel> getViewModelClass() {
        return ClinicViewModel.class;
    }

    @Override
    protected void onViewModelCreated(@NonNull ClinicViewModel viewModel) {
        clinicViewModel = viewModel;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tcpServerService != null) {
            tcpServerService.unregisterOnMessageListener(serviceMessageListener);
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            clinicViewModel.appendDeviceConsole("通知权限已开启");
        }
    }

    private void startAndBindService() {
        trace("请求启动并绑定 TCP 前台服务");
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void bindServiceCallbacks() {
        if (tcpServerService == null) {
            return;
        }
        tcpServerService.unregisterOnMessageListener(serviceMessageListener);
        tcpServerService.registerOnMessageListener(serviceMessageListener);
    }

    private void appendConsoleAndRefresh(String consoleMessage) {
        clinicViewModel.appendDeviceConsole(consoleMessage);
        clinicViewModel.refreshDeviceState();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void requestBatteryOptimizationWhitelist() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        String packageName = getPackageName();
        if (powerManager == null || powerManager.isIgnoringBatteryOptimizations(packageName)) {
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private String resolveLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public boolean isServerRunning() {
        return tcpServerService != null && tcpServerService.isServerRunning();
    }

    @Override
    public String getLocalIpAddress() {
        if (tcpServerService != null && !TextUtils.isEmpty(tcpServerService.getLocalIpAddress())) {
            return tcpServerService.getLocalIpAddress();
        }
        if (TextUtils.isEmpty(localIpAddress)) {
            localIpAddress = resolveLocalIpAddress();
        }
        return localIpAddress;
    }

    @Override
    public int getServerPort() {
        return ServerConstance.SERVER_PORT;
    }

    @Override
    public List<ConnectedDeviceInfo> getConnectedDevices() {
        List<ConnectedDeviceInfo> result = new ArrayList<>();
        if (tcpServerService == null) {
            return result;
        }
        DeviceManager.DeviceConnection[] connections = tcpServerService.getConnectedDevices();
        for (DeviceManager.DeviceConnection connection : connections) {
            result.add(new ConnectedDeviceInfo(
                    connection.getDeviceId(),
                    connection.getSelectionLabel(),
                    connection.getMacAddress(),
                    connection.getRemoteIp(),
                    connection.getRemotePort(),
                    connection.getConnectedAt()
            ));
        }
        return result;
    }

    @Override
    public void startServer() {
        trace("界面层请求启动监听服务");
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        if (!isServiceBound) {
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    @Override
    public void stopServer() {
        if (tcpServerService != null) {
            trace("界面层请求停止监听服务");
            tcpServerService.stopServer();
        }
    }

    @Override
    public void broadcastMessage(String message) {
        if (tcpServerService != null) {
            trace("界面层请求广播消息，长度=" + (message == null ? 0 : message.length()));
            tcpServerService.broadcastMessage(message);
        }
    }

    @Override
    public void sendMessageToClient(String clientId, String message) {
        if (tcpServerService != null) {
            trace("界面层请求定向发送，clientId=" + clientId + ", 长度=" + (message == null ? 0 : message.length()));
            tcpServerService.sendMessageToClient(clientId, message);
        }
    }

    @Override
    public void sendCommandToClient(String clientId, String commandCode, Map<String, String> arguments) {
        if (tcpServerService != null && !TextUtils.isEmpty(clientId) && !TextUtils.isEmpty(commandCode)) {
            trace("界面层按编码发送命令，clientId=" + clientId + ", code=" + commandCode);
            tcpServerService.sendCommandByCodeToClient(clientId, commandCode, arguments);
        }
    }

    private void trace(String message) {
        DLog.i(TAG, message);
    }
}
