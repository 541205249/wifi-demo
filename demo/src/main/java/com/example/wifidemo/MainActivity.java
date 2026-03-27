package com.example.wifidemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.wifidemo.device.DeviceHistoryStore;
import com.example.wifidemo.device.DeviceManager;
import com.wifi.lib.log.DLog;
import com.wifi.lib.utils.Toasty;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "DemoMainActivity";

    private TextView tvServerInfo;
    private TextView tvIP;
    private TextView tvPort;
    private Button btnStartStop;
    private EditText etMessage;
    private Button btnSend;
    private Button btnSendToClient;
    private Spinner spinnerClients;
    private TextView tvClientCount;
    private Spinner spinnerHistoryDevices;
    private Button btnViewDeviceHistory;
    private TextView tvLog;
    private ScrollView svLog;

    private TcpServerService tcpServerService;
    private DeviceHistoryStore deviceHistoryStore;
    private Handler mainHandler;
    private boolean isBound = false;
    private boolean isServiceStarted = false;
    private String localIpAddress;
    private ArrayAdapter<String> clientAdapter;
    private ArrayAdapter<String> historyDeviceAdapter;
    private final List<String> connectedDeviceIds = new ArrayList<>();
    private final List<String> knownDeviceIds = new ArrayList<>();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TcpServerService.TcpServerBinder binder = (TcpServerService.TcpServerBinder) service;
            tcpServerService = binder.getService();
            isBound = true;
            appendLog("服务已连接");
            trace("Demo 服务绑定完成");

            if (localIpAddress != null && tcpServerService != null) {
                tcpServerService.setLocalIpAddress(localIpAddress);
            }

            setupServiceListener();
            updateUIFromService();
            updateHistoryDeviceList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            appendLog("服务已断开");
            trace("Demo 服务连接断开");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();

        mainHandler = new Handler(Looper.getMainLooper());
        deviceHistoryStore = DeviceHistoryStore.getInstance(this);

        checkNotificationPermission();
        requestBatteryOptimizationWhitelist();
        localIpAddress = getLocalIpAddress();
        if (localIpAddress != null) {
            btnStartStop.setEnabled(true);
            tvIP.setText("IP 地址：" + localIpAddress);
            appendLog("本机 IP: " + localIpAddress);
            trace("Demo 首页初始化完成，本机 IP=" + localIpAddress);
        } else {
            btnStartStop.setEnabled(false);
            tvIP.setText("IP 地址：获取中...");
            appendLog("未能获取到 IP 地址");
            trace("Demo 首页初始化完成，但未获取到本机 IP");
        }

        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, 0);
        updateHistoryDeviceList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appendLog("App 进入后台");
        if (tcpServerService != null) {
            appendLog("服务未解绑，TCP 连接保持");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appendLog("App 回到前台");
        updateHistoryDeviceList();
        if (isBound && tcpServerService != null) {
            updateUIFromService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        appendLog("Activity 销毁");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("通知权限已授予");
            } else {
                appendLog("通知权限被拒绝，可能影响服务通知显示");
                Toasty.showLong("通知权限被拒绝，可能影响服务通知显示");
            }
        }
    }

    private void initViews() {
        tvServerInfo = findViewById(R.id.tvServerInfo);
        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        btnStartStop = findViewById(R.id.btnStartStop);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnSendToClient = findViewById(R.id.btnSendToClient);
        spinnerClients = findViewById(R.id.spinnerClients);
        tvClientCount = findViewById(R.id.tvClientCount);
        spinnerHistoryDevices = findViewById(R.id.spinnerHistoryDevices);
        btnViewDeviceHistory = findViewById(R.id.btnViewDeviceHistory);
        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);

        tvPort.setText("端口：" + ServerConstance.SERVER_PORT);
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);
        btnSendToClient.setEnabled(false);

        clientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClients.setAdapter(clientAdapter);

        historyDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        historyDeviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistoryDevices.setAdapter(historyDeviceAdapter);
        spinnerHistoryDevices.setEnabled(false);
        btnViewDeviceHistory.setEnabled(false);
    }

    private void setupListeners() {
        btnStartStop.setOnClickListener(v -> toggleServer());
        btnSend.setOnClickListener(v -> broadcastMessage());
        btnSendToClient.setOnClickListener(v -> sendMessageToSelectedClient());
        btnViewDeviceHistory.setOnClickListener(v -> openSelectedDeviceHistory());
    }

    private void setupServiceListener() {
        if (tcpServerService == null) {
            return;
        }

        tcpServerService.setOnMessageListener(new TcpServerService.OnMessageListener() {
            @Override
            public void onMessageReceived(String clientId, String message) {
                mainHandler.post(() -> {
                    trace("Demo 收到模块消息，clientId=" + clientId + ", length=" + (message == null ? 0 : message.length()));
                    appendLog("收到模块 [" + formatDeviceLabel(clientId) + "] 消息：" + message);
                    updateHistoryDeviceList();
                });
            }

            @Override
            public void onClientConnected(String clientId) {
                mainHandler.post(() -> {
                    String label = formatDeviceLabel(clientId);
                    trace("Demo 模块接入，clientId=" + clientId + ", label=" + label);
                    appendLog("客户端已连接：" + label);
                    Toasty.showShort("WiFi 模块已连接：" + label);
                    updateClientList();
                    updateHistoryDeviceList();
                });
            }

            @Override
            public void onClientIdentityResolved(String clientId, String macAddress) {
                mainHandler.post(() -> {
                    trace("Demo 模块身份已解析，clientId=" + clientId + ", mac=" + macAddress);
                    appendLog("模块身份已识别：" + macAddress + " [" + clientId + "]");
                    updateClientList();
                    updateHistoryDeviceList();
                });
            }

            @Override
            public void onClientDisconnected(String clientId) {
                mainHandler.post(() -> {
                    String label = formatDeviceLabel(clientId);
                    trace("Demo 模块断开，clientId=" + clientId + ", label=" + label);
                    appendLog("客户端已断开：" + label);
                    Toasty.showShort("WiFi 模块已断开：" + label);
                    updateClientList();
                    updateHistoryDeviceList();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    trace("Demo 通信错误：" + error);
                    appendLog("错误：" + error);
                    Toasty.showShort(error);
                });
            }

            @Override
            public void onServerStarted(String ipAddress) {
                mainHandler.post(() -> {
                    if (ipAddress != null) {
                        tvIP.setText("IP 地址：" + ipAddress);
                        appendLog("服务器已启动，IP: " + ipAddress);
                        trace("Demo 监听已启动，ip=" + ipAddress);
                    } else {
                        tvIP.setText("IP 地址：" + (localIpAddress != null ? localIpAddress : "未获取到"));
                        appendLog("服务器已启动");
                        trace("Demo 监听已启动，但服务未返回 IP");
                    }
                    updateUIFromService();
                });
            }
        });
    }

    private void toggleServer() {
        boolean serverRunning = isBound && tcpServerService != null && tcpServerService.isServerRunning();
        if (serverRunning) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        if (!isBound || tcpServerService == null) {
            appendLog("服务未连接，无法启动");
            trace("Demo 启动监听失败，服务尚未绑定");
            return;
        }

        appendLog("正在启动服务器...");
        trace("Demo 请求启动监听服务");
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        isServiceStarted = true;
    }

    private void stopServer() {
        if (!isBound || tcpServerService == null) {
            return;
        }

        appendLog("正在停止服务器...");
        trace("Demo 请求停止监听服务");
        tcpServerService.stopServer();
        isServiceStarted = false;
        updateUIFromService();
        updateHistoryDeviceList();
    }

    private void updateUIFromService() {
        boolean serverRunning = isBound && tcpServerService != null && tcpServerService.isServerRunning();
        isServiceStarted = serverRunning;

        if (serverRunning) {
            btnStartStop.setText("停止服务器");
            tvServerInfo.setText("服务器状态：运行中 ✓");
            tvServerInfo.setTextColor(getColor(android.R.color.holo_green_dark));
            etMessage.setEnabled(true);
            btnSend.setEnabled(true);
            btnSendToClient.setEnabled(tcpServerService.getConnectedClientCount() > 0);

            String ip = tcpServerService.getLocalIpAddress();
            if (ip != null) {
                tvIP.setText("IP 地址：" + ip);
            } else if (localIpAddress != null) {
                tvIP.setText("IP 地址：" + localIpAddress);
            }

            updateClientList();
        } else {
            btnStartStop.setText("启动服务器");
            tvServerInfo.setText("服务器状态：已停止");
            tvServerInfo.setTextColor(getColor(android.R.color.darker_gray));
            etMessage.setEnabled(false);
            btnSend.setEnabled(false);
            btnSendToClient.setEnabled(false);
            updateClientList();
        }
    }

    private void updateClientList() {
        if (!isBound || tcpServerService == null) {
            connectedDeviceIds.clear();
            clientAdapter.clear();
            clientAdapter.notifyDataSetChanged();
            tvClientCount.setText("已连接客户端：0 个");
            btnSendToClient.setEnabled(false);
            return;
        }

        DeviceManager.DeviceConnection[] connectedDevices = tcpServerService.getConnectedDevices();
        tvClientCount.setText("已连接客户端：" + connectedDevices.length + " 个");

        connectedDeviceIds.clear();
        clientAdapter.clear();
        for (DeviceManager.DeviceConnection device : connectedDevices) {
            connectedDeviceIds.add(device.getDeviceId());
            clientAdapter.add("模块：" + device.getSelectionLabel());
        }
        clientAdapter.notifyDataSetChanged();
        btnSendToClient.setEnabled(connectedDevices.length > 0);
        trace("Demo 在线模块列表已刷新，count=" + connectedDevices.length);
    }

    private void updateHistoryDeviceList() {
        if (deviceHistoryStore == null) {
            return;
        }

        List<DeviceHistoryStore.DeviceSummary> devices = deviceHistoryStore.getKnownDevices();
        knownDeviceIds.clear();
        historyDeviceAdapter.clear();
        for (DeviceHistoryStore.DeviceSummary device : devices) {
            knownDeviceIds.add(device.getDeviceId());
            historyDeviceAdapter.add(device.getSelectionLabel());
        }

        historyDeviceAdapter.notifyDataSetChanged();
        boolean hasHistoryDevices = !knownDeviceIds.isEmpty();
        spinnerHistoryDevices.setEnabled(hasHistoryDevices);
        btnViewDeviceHistory.setEnabled(hasHistoryDevices);
        trace("Demo 历史模块列表已刷新，count=" + knownDeviceIds.size());
    }

    private void broadcastMessage() {
        if (!isBound || tcpServerService == null) {
            return;
        }

        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toasty.showShort("请输入消息内容");
            return;
        }

        trace("Demo 广播消息，length=" + message.length());
        tcpServerService.broadcastMessage(message);
        appendLog("广播到所有模块：" + message);
        updateHistoryDeviceList();
    }

    private void sendMessageToSelectedClient() {
        if (!isBound || tcpServerService == null) {
            return;
        }

        int selectedPosition = spinnerClients.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= connectedDeviceIds.size()) {
            Toasty.showShort("请选择要发送的模块");
            return;
        }

        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toasty.showShort("请输入消息内容");
            return;
        }

        String clientId = connectedDeviceIds.get(selectedPosition);
        trace("Demo 定向发送消息，clientId=" + clientId + ", length=" + message.length());
        tcpServerService.sendMessageToClient(clientId, message);
        appendLog("发送到模块 [" + formatDeviceLabel(clientId) + "]：" + message);
        updateHistoryDeviceList();
    }

    private void openSelectedDeviceHistory() {
        int selectedPosition = spinnerHistoryDevices.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= knownDeviceIds.size()) {
            Toasty.showShort("请选择要查看记录的设备");
            return;
        }

        Intent intent = new Intent(this, DeviceHistoryActivity.class);
        intent.putExtra(DeviceHistoryActivity.EXTRA_DEVICE_ID, knownDeviceIds.get(selectedPosition));
        trace("Demo 打开模块历史页，deviceId=" + knownDeviceIds.get(selectedPosition));
        startActivity(intent);
    }

    private void appendLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvLog.append("[" + timestamp + "] " + message + "\n");
        svLog.post(() -> svLog.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void requestBatteryOptimizationWhitelist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();

            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(android.net.Uri.parse("package:" + packageName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    appendLog("已申请电池优化白名单");
                } catch (Exception e) {
                    appendLog("申请电池优化白名单失败：" + e.getMessage());
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toasty.showLong("请手动将本应用加入电池优化白名单");
                    } catch (Exception innerError) {
                        appendLog("打开设置页面失败：" + innerError.getMessage());
                    }
                }
            }
        }
    }

    private String getLocalIpAddress() {
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
        } catch (Exception e) {
            appendLog("获取本机 IP 失败：" + e.getMessage());
        }
        return null;
    }

    private String formatDeviceLabel(String deviceId) {
        if (tcpServerService != null) {
            String liveLabel = tcpServerService.getDeviceDisplayLabel(deviceId);
            if (!TextUtils.isEmpty(liveLabel) && !deviceId.equals(liveLabel)) {
                return liveLabel;
            }
        }

        if (deviceHistoryStore != null) {
            DeviceHistoryStore.DeviceSummary summary = deviceHistoryStore.getDeviceSummary(deviceId);
            if (summary != null) {
                return summary.getInlineLabel();
            }
        }
        return deviceId;
    }

    private void trace(String message) {
        DLog.i(TAG, message);
    }
}

