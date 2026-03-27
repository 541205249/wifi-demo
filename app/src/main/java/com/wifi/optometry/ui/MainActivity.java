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
import androidx.core.view.GravityCompat;

import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseMvvmActivity;
import com.wifi.optometry.R;
import com.wifi.optometry.communication.ServerConstance;
import com.wifi.optometry.communication.TcpServerService;
import com.wifi.optometry.communication.device.DeviceManager;
import com.wifi.optometry.databinding.ActivityMainBinding;
import com.wifi.optometry.domain.model.ConnectedDeviceInfo;
import com.wifi.optometry.ui.main.DeviceFragment;
import com.wifi.optometry.ui.main.PatientFragment;
import com.wifi.optometry.ui.main.ProgramFragment;
import com.wifi.optometry.ui.main.ReportFragment;
import com.wifi.optometry.ui.main.SettingsFragment;
import com.wifi.optometry.ui.main.WorkbenchFragment;
import com.wifi.optometry.ui.state.ClinicViewModel;
import com.wifi.optometry.ui.state.DeviceServiceGateway;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends BaseMvvmActivity<ActivityMainBinding, ClinicViewModel> implements DeviceServiceGateway {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1101;
    private static final String TAG = "MainActivity";

    private ClinicViewModel clinicViewModel;
    private TcpServerService tcpServerService;
    private boolean isServiceBound;
    private String localIpAddress;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TcpServerService.TcpServerBinder binder = (TcpServerService.TcpServerBinder) service;
            tcpServerService = binder.getService();
            isServiceBound = true;
            trace("TCP 服务绑定完成，准备同步本地地址和回调");
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
            trace("TCP 服务连接断开，触发界面状态刷新");
            clinicViewModel.refreshDeviceState();
            clinicViewModel.appendDeviceConsole("WiFi 通信服务已断开");
        }
    };

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        setSupportActionBar(binding.topAppBar);
        binding.topAppBar.setNavigationOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.navigationView.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return openDestination(item.getItemId());
        });
        checkNotificationPermission();
        requestBatteryOptimizationWhitelist();
        localIpAddress = resolveLocalIpAddress();
        trace("主界面初始化，本机 IP=" + (TextUtils.isEmpty(localIpAddress) ? "未获取" : localIpAddress));

        clinicViewModel.setDeviceServiceGateway(this);
        if (savedInstanceState == null) {
            openDestination(R.id.menu_workbench);
            binding.navigationView.setCheckedItem(R.id.menu_workbench);
        }

        startAndBindService();
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

    public void openDeviceHistory(String deviceId) {
        Intent intent = new Intent(this, com.wifi.optometry.ui.device.DeviceHistoryActivity.class);
        intent.putExtra(com.wifi.optometry.ui.device.DeviceHistoryActivity.EXTRA_DEVICE_ID, deviceId);
        startActivity(intent);
    }

    private boolean openDestination(int itemId) {
        androidx.fragment.app.Fragment fragment;
        int titleResId;
        if (itemId == R.id.menu_patient) {
            fragment = new PatientFragment();
            titleResId = R.string.nav_patient;
        } else if (itemId == R.id.menu_program) {
            fragment = new ProgramFragment();
            titleResId = R.string.nav_program;
        } else if (itemId == R.id.menu_report) {
            fragment = new ReportFragment();
            titleResId = R.string.nav_report;
        } else if (itemId == R.id.menu_device) {
            fragment = new DeviceFragment();
            titleResId = R.string.nav_device;
        } else if (itemId == R.id.menu_settings) {
            fragment = new SettingsFragment();
            titleResId = R.string.nav_settings;
        } else {
            fragment = new WorkbenchFragment();
            titleResId = R.string.nav_workbench;
        }

        binding.topAppBar.setTitle(titleResId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), fragment)
                .commit();
        return true;
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
        tcpServerService.setOnMessageListener(new TcpServerService.OnMessageListener() {
            @Override
            public void onMessageReceived(String clientId, String message) {
                trace("服务回调收到模块消息，clientId=" + clientId + ", 长度=" + (message == null ? 0 : message.length()));
                clinicViewModel.appendDeviceConsole("收到 " + clientId + " 的消息: " + message);
                clinicViewModel.refreshDeviceState();
            }

            @Override
            public void onClientConnected(String clientId) {
                trace("服务回调模块接入，clientId=" + clientId);
                clinicViewModel.appendDeviceConsole("模块已连接: " + clientId);
                clinicViewModel.refreshDeviceState();
            }

            @Override
            public void onClientIdentityResolved(String clientId, String macAddress) {
                trace("服务回调模块身份已解析，clientId=" + clientId + ", mac=" + macAddress);
                clinicViewModel.appendDeviceConsole("已识别模块身份: " + macAddress + " [" + clientId + "]");
                clinicViewModel.refreshDeviceState();
            }

            @Override
            public void onClientDisconnected(String clientId) {
                trace("服务回调模块断开，clientId=" + clientId);
                clinicViewModel.appendDeviceConsole("模块已断开: " + clientId);
                clinicViewModel.refreshDeviceState();
            }

            @Override
            public void onError(String error) {
                trace("服务回调出现错误: " + error);
                clinicViewModel.appendDeviceConsole("通信错误: " + error);
                clinicViewModel.refreshDeviceState();
            }

            @Override
            public void onServerStarted(String ipAddress) {
                if (!TextUtils.isEmpty(ipAddress)) {
                    localIpAddress = ipAddress;
                }
                trace("服务回调监听已启动，地址=" + (TextUtils.isEmpty(ipAddress) ? getLocalIpAddress() : ipAddress)
                        + ":" + ServerConstance.SERVER_PORT);
                clinicViewModel.appendDeviceConsole("监听服务运行中，地址: "
                        + (TextUtils.isEmpty(getLocalIpAddress()) ? "未获取" : getLocalIpAddress())
                        + ":" + ServerConstance.SERVER_PORT);
                clinicViewModel.refreshDeviceState();
            }
        });
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
            trace("查询在线模块时服务尚未就绪");
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
        trace("聚合在线模块列表完成，数量=" + result.size());
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

    private void trace(String message) {
        DLog.i(TAG, message);
    }
}

