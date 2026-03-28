package com.wifi.optometry.communication.device;

import android.text.TextUtils;

import com.wifi.lib.log.DLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过 HC-25 的 UDP SEARCH 能力查询模块 MAC。
 */
public final class Hc25MacDiscoveryClient {
    private static final String TAG = "Hc25MacDiscovery";
    private static final String RESPONSE_COMPACT_REGEX = "\\s+";
    public static final int DEFAULT_SEARCH_PORT = 54321;
    public static final String DEFAULT_SEARCH_KEYWORD = "HC-25";

    private static final int MAX_ATTEMPTS = 3;
    private static final int SOCKET_TIMEOUT_MS = 1200;
    private static final int RECEIVE_BUFFER_SIZE = 512;
    private static final Pattern MAC_PATTERN = Pattern.compile(
            "MAC\\s*[:=]\\s*([0-9A-Fa-f:-]{12,17}|[0-9A-Fa-f]{12})"
    );

    public String queryMacAddress(String remoteIp) {
        if (TextUtils.isEmpty(remoteIp)) {
            return null;
        }

        try {
            InetAddress remoteAddress = InetAddress.getByName(remoteIp);
            DLog.i(TAG, "开始查询模块 MAC，remoteIp=" + remoteIp);
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                String response = sendSearch(remoteAddress);
                String macAddress = parseMacAddress(response);
                if (!TextUtils.isEmpty(macAddress)) {
                    DLog.i(TAG, "模块 MAC 查询成功，remoteIp=" + remoteIp + ", mac=" + macAddress + ", attempt=" + (attempt + 1));
                    return macAddress;
                }
                DLog.w(TAG, "模块 MAC 查询未命中，remoteIp=" + remoteIp + ", attempt=" + (attempt + 1));
            }
        } catch (IOException e) {
            DLog.e(TAG, "模块 MAC 查询异常，remoteIp=" + remoteIp, e);
        }
        return null;
    }

    String parseMacAddress(String response) {
        if (TextUtils.isEmpty(response)) {
            return null;
        }

        String macAddress = findMacAddress(response, "SEARCH 响应");
        if (!TextUtils.isEmpty(macAddress)) {
            return macAddress;
        }
        return findMacAddress(response.replaceAll(RESPONSE_COMPACT_REGEX, ""), "紧凑 SEARCH 响应");
    }

    private String sendSearch(InetAddress remoteAddress) throws IOException {
        byte[] requestData = DEFAULT_SEARCH_KEYWORD.getBytes(StandardCharsets.UTF_8);
        DatagramPacket requestPacket = new DatagramPacket(
                requestData,
                requestData.length,
                remoteAddress,
                DEFAULT_SEARCH_PORT
        );

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            socket.send(requestPacket);

            byte[] responseBuffer = new byte[RECEIVE_BUFFER_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            while (true) {
                try {
                    socket.receive(responsePacket);
                    if (remoteAddress.equals(responsePacket.getAddress())) {
                        return new String(
                                responsePacket.getData(),
                                responsePacket.getOffset(),
                                responsePacket.getLength(),
                                StandardCharsets.UTF_8
                        ).trim();
                    }
                } catch (SocketTimeoutException e) {
                    DLog.w(TAG, "等待 SEARCH 响应超时，remoteIp=" + remoteAddress.getHostAddress());
                    return null;
                }
            }
        }
    }

    private String findMacAddress(String response, String responseLabel) {
        Matcher matcher = MAC_PATTERN.matcher(response);
        if (!matcher.find()) {
            return null;
        }

        String macAddress = DeviceHistoryStore.normalizeMacAddress(matcher.group(1));
        DLog.d(TAG, "从" + responseLabel + "中解析到 MAC=" + macAddress);
        return macAddress;
    }
}

