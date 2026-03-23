package com.example.wifidemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    private TextView tvServerInfo;
    private TextView tvIP;
    private TextView tvPort;
    private Button btnStartStop;
    private EditText etMessage;
    private Button btnSend;
    private TextView tvLog;
    private ScrollView svLog;

    private TcpServer tcpServer;
    private Handler mainHandler;
    private boolean isServerRunning = false;
    private static final int SERVER_PORT = 9111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());
        tcpServer = new TcpServer();

        initViews();
        setupListeners();
        updateIPInfo();
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

        tvPort.setText("端口：" + SERVER_PORT);
    }

    private void setupListeners() {
        btnStartStop.setOnClickListener(v -> toggleServer());
        btnSend.setOnClickListener(v -> sendMessage());

        tcpServer.setOnMessageListener(new TcpServer.OnMessageListener() {
            @Override
            public void onMessageReceived(String message) {
                mainHandler.post(() -> appendLog("收到模块消息：" + message));
            }

            @Override
            public void onClientConnected() {
                mainHandler.post(() -> {
                    appendLog("客户端已连接");
                    Toast.makeText(MainActivity.this, "WiFi 模块已连接", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onClientDisconnected() {
                mainHandler.post(() -> {
                    appendLog("客户端已断开");
                    Toast.makeText(MainActivity.this, "WiFi 模块已断开", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    appendLog("错误：" + error);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void toggleServer() {
        if (isServerRunning) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        appendLog("正在启动服务器...");
        boolean success = tcpServer.start(SERVER_PORT);
        if (success) {
            isServerRunning = true;
            btnStartStop.setText("停止服务器");
            tvServerInfo.setText("服务器状态：运行中 ✓");
            tvServerInfo.setTextColor(getColor(android.R.color.holo_green_dark));
            etMessage.setEnabled(true);
            btnSend.setEnabled(true);
            appendLog("服务器已启动，等待客户端连接...");
            updateIPInfo();
        } else {
            appendLog("服务器启动失败");
        }
    }

    private void stopServer() {
        appendLog("正在停止服务器...");
        tcpServer.stop();
        isServerRunning = false;
        btnStartStop.setText("启动服务器");
        tvServerInfo.setText("服务器状态：已停止");
        tvServerInfo.setTextColor(getColor(android.R.color.darker_gray));
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);
        appendLog("服务器已停止");
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        tcpServer.sendMessageToClient(message);
        appendLog("发送到模块：" + message);
        etMessage.setText("");
    }

    private void updateIPInfo() {
        String ipAddress = getLocalIpAddress();
        if (ipAddress != null) {
            tvIP.setText("IP 地址：" + ipAddress);
            appendLog("本机 IP: " + ipAddress);
        } else {
            tvIP.setText("IP 地址：未获取到");
            appendLog("未能获取到 IP 地址");
        }
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

    private void appendLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvLog.append("[" + timestamp + "] " + message + "\n");

        svLog.post(() -> {
            svLog.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }
}
