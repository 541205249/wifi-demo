package com.example.wifidemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView tvServerInfo;
    private TextView tvIP;
    private TextView tvPort;
    private Button btnStartStop;
    private EditText etMessage;
    private Button btnSend;
    private Button btnSendToClient;
    private Spinner spinnerClients;
    private TextView tvClientCount;
    private TextView tvLog;
    private ScrollView svLog;

    private TcpServerService tcpServerService;
    private Handler mainHandler;
    private boolean isBound = false;
    private boolean isServiceStarted = false;
    private String localIpAddress;
    private ArrayAdapter<String> clientAdapter;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TcpServerService.TcpServerBinder binder = (TcpServerService.TcpServerBinder) service;
            tcpServerService = binder.getService();
            isBound = true;
            appendLog("服务已连接");

            if (localIpAddress != null && tcpServerService != null) {
                tcpServerService.setLocalIpAddress(localIpAddress);
            }

            setupServiceListener();
            updateUIFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            appendLog("服务已断开");
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

        checkNotificationPermission();
        requestBatteryOptimizationWhitelist(); // 申请电池优化白名单
        localIpAddress = getLocalIpAddress();
        if (localIpAddress != null) {
            btnStartStop.setEnabled(true);
            tvIP.setText("IP 地址：" + localIpAddress);
            appendLog("本机 IP: " + localIpAddress);
        } else {
            btnStartStop.setEnabled(false);
            tvIP.setText("IP 地址：获取中...");
            appendLog("未能获取到 IP 地址");
        }

        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, 0);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("通知权限已授予");
            } else {
                appendLog("通知权限被拒绝，可能影响服务通知显示");
                Toast.makeText(this, "通知权限被拒绝，可能影响服务通知显示", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupServiceListener() {
        if (tcpServerService == null) return;

        tcpServerService.setOnMessageListener(new TcpServerService.OnMessageListener() {
            @Override
            public void onMessageReceived(String clientId, String message) {
                mainHandler.post(() -> appendLog("收到模块 [" + clientId + "] 消息：" + message));
            }
        
            @Override
            public void onClientConnected(String clientId) {
                mainHandler.post(() -> {
                    appendLog("客户端已连接：" + clientId);
                    Toast.makeText(MainActivity.this, "WiFi 模块已连接：" + clientId, Toast.LENGTH_SHORT).show();
                    updateClientList();
                });
            }
        
            @Override
            public void onClientDisconnected(String clientId) {
                mainHandler.post(() -> {
                    appendLog("客户端已断开：" + clientId);
                    Toast.makeText(MainActivity.this, "WiFi 模块已断开：" + clientId, Toast.LENGTH_SHORT).show();
                    updateClientList();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    appendLog("错误：" + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onServerStarted(String ipAddress) {
                mainHandler.post(() -> {
                    if (ipAddress != null) {
                        tvIP.setText("IP 地址：" + ipAddress);
                        appendLog("服务器已启动，IP: " + ipAddress);
                    } else {
                        tvIP.setText("IP 地址：" + (localIpAddress != null ? localIpAddress : "未获取到"));
                        appendLog("服务器已启动");
                    }
                    updateUIFromService();
                });
            }
        });
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
        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);

        tvPort.setText("端口：" + ServerConstance.SERVER_PORT);
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);
        btnSendToClient.setEnabled(false);
        
        // 初始化 Spinner 适配器
        clientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        clientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClients.setAdapter(clientAdapter);
    }

    private void setupListeners() {
        btnStartStop.setOnClickListener(v -> toggleServer());
        btnSend.setOnClickListener(v -> broadcastMessage());
        btnSendToClient.setOnClickListener(v -> sendMessageToSelectedClient());
    }

    private void toggleServer() {
        if (isServiceStarted) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        if (!isBound || tcpServerService == null) {
            appendLog("服务未连接，无法启动");
            return;
        }

        appendLog("正在启动服务器...");
        Intent serviceIntent = new Intent(this, TcpServerService.class);
        startForegroundService(serviceIntent);
        isServiceStarted = true;
    }

    private void stopServer() {
        if (!isBound || tcpServerService == null) {
            return;
        }

        appendLog("正在停止服务器...");
        tcpServerService.stopServer();
        isServiceStarted = false;

        updateUIFromService();
    }

    private void updateUIFromService() {
        if (isBound && tcpServerService != null && tcpServerService.isServerRunning()) {
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
        }
    }
    
    /**
     * 更新客户端列表
     */
    private void updateClientList() {
        if (!isBound || tcpServerService == null) return;
        
        String[] clientIds = tcpServerService.getConnectedClientIds();
        int clientCount = tcpServerService.getConnectedClientCount();
        
        tvClientCount.setText("已连接客户端：" + clientCount + " 个");
        
        clientAdapter.clear();
        for (String clientId : clientIds) {
            clientAdapter.add("模块：" + clientId);
        }
        clientAdapter.notifyDataSetChanged();
        
        btnSendToClient.setEnabled(clientCount > 0);
    }

    private void broadcastMessage() {
        if (!isBound || tcpServerService == null) {
            return;
        }

        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        tcpServerService.broadcastMessage(message);
        appendLog("广播到所有模块：" + message);
//        etMessage.setText("");
    }
    
    private void sendMessageToSelectedClient() {
        if (!isBound || tcpServerService == null) {
            return;
        }
        
        int selectedPosition = spinnerClients.getSelectedItemPosition();
        if (selectedPosition < 0) {
            Toast.makeText(this, "请选择要发送的模块", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedItem = (String) spinnerClients.getSelectedItem();
        String clientId = selectedItem.replace("模块：", "");
        
        tcpServerService.sendMessageToClient(clientId, message);
        appendLog("发送到模块 [" + clientId + "]：" + message);
//        etMessage.setText("");
    }

    private void appendLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvLog.append("[" + timestamp + "] " + message + "\n");

        svLog.post(() -> {
            svLog.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 申请电池优化白名单，防止系统杀后台
     */
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
                    // 如果直接申请失败，引导用户手动设置
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        Toast.makeText(this, "请手动将本应用加入电池优化白名单", Toast.LENGTH_LONG).show();
                    } catch (Exception e2) {
                        appendLog("打开设置页面失败：" + e2.getMessage());
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        appendLog("App 进入后台");
        // 不在后台解绑服务，保持 TCP 连接
        
        // 确保 WiFi 锁和电源锁在后台仍然有效
        if (tcpServerService != null) {
            appendLog("服务未解绑，TCP 连接保持");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appendLog("App 回到前台");
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
}
