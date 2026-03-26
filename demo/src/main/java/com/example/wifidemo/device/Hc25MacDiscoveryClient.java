package com.example.wifidemo.device;

import android.text.TextUtils;

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
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                String response = sendSearch(remoteAddress);
                String macAddress = parseMacAddress(response);
                if (!TextUtils.isEmpty(macAddress)) {
                    return macAddress;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    String parseMacAddress(String response) {
        if (TextUtils.isEmpty(response)) {
            return null;
        }

        Matcher matcher = MAC_PATTERN.matcher(response);
        if (matcher.find()) {
            return DeviceHistoryStore.normalizeMacAddress(matcher.group(1));
        }

        String compactResponse = response.replaceAll("\\s+", "");
        matcher = MAC_PATTERN.matcher(compactResponse);
        if (matcher.find()) {
            return DeviceHistoryStore.normalizeMacAddress(matcher.group(1));
        }
        return null;
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
                    return null;
                }
            }
        }
    }
}
