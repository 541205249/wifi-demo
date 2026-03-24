package com.example.wifidemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
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
    private TextView tvLog;
    private ScrollView svLog;

    private TcpServerService tcpServerService;
    private Handler mainHandler;
    private boolean isBound = false;
    private boolean isServiceStarted = false;
    private String localIpAddress;

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
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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
            public void onMessageReceived(String message) {
                mainHandler.post(() -> appendLog("收到模块消息：" + message));
            }

            @Override
            public void onClientConnected() {
                mainHandler.post(() -> {
                    appendLog("客户端已连接");
                    Toast.makeText(MainActivity.this, "WiFi模块已连接", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onClientDisconnected() {
                mainHandler.post(() -> {
                    appendLog("客户端已断开");
                    Toast.makeText(MainActivity.this, "WiFi模块已断开", Toast.LENGTH_SHORT).show();
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
        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);

        tvPort.setText("端口：" + ServerConstance.SERVER_PORT);
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);
    }

    private void setupListeners() {
        btnStartStop.setOnClickListener(v -> toggleServer());
        btnSend.setOnClickListener(v -> sendMessage());
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

            String ip = tcpServerService.getLocalIpAddress();
            if (ip != null) {
                tvIP.setText("IP 地址：" + ip);
            } else if (localIpAddress != null) {
                tvIP.setText("IP 地址：" + localIpAddress);
            }
        } else {
            btnStartStop.setText("启动服务器");
            tvServerInfo.setText("服务器状态：已停止");
            tvServerInfo.setTextColor(getColor(android.R.color.darker_gray));
            etMessage.setEnabled(false);
            btnSend.setEnabled(false);
        }
    }

    private void sendMessage() {
        if (!isBound || tcpServerService == null) {
            return;
        }

        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        tcpServerService.sendMessageToClient(message);
        appendLog("发送到模块：" + message);
        etMessage.setText("");
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

    @Override
    protected void onPause() {
        super.onPause();
        appendLog("App 进入后台");
    }

    @Override
    protected void onResume() {
        super.onResume();
        appendLog("App 回到前台");
        if (isBound) {
            updateUIFromService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        isBound = false;
        appendLog("Activity 销毁");
    }
}
