package com.example.wifidemo.device;

import android.text.TextUtils;

import com.wifi.lib.log.DLog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 从本地 ARP 表中解析对端设备的 MAC 地址。
 */
public final class NetworkIdentityResolver {
    private static final String TAG = "NetworkResolver";
    private static final String ARP_TABLE_PATH = "/proc/net/arp";
    private static final int MIN_ARP_COLUMNS = 4;
    private static final int ARP_MAC_INDEX = 3;
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 150L;

    private NetworkIdentityResolver() {
    }

    public static String resolveMacAddress(String ipAddress) {
        if (TextUtils.isEmpty(ipAddress)) {
            return null;
        }

        DLog.i(TAG, "开始从 ARP 表解析 MAC，ip=" + ipAddress);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String macAddress = readMacFromArp(ipAddress);
            if (!TextUtils.isEmpty(macAddress)) {
                DLog.i(TAG, "ARP 表解析成功，ip=" + ipAddress + ", mac=" + macAddress + ", attempt=" + (attempt + 1));
                return macAddress;
            }
            if (!waitBeforeNextAttempt(ipAddress, attempt)) {
                return null;
            }
        }
        DLog.w(TAG, "ARP 表解析未命中，ip=" + ipAddress);
        return null;
    }

    private static String readMacFromArp(String ipAddress) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARP_TABLE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String macAddress = findMacAddressInLine(ipAddress, line);
                if (!TextUtils.isEmpty(macAddress)) {
                    return macAddress;
                }
            }
        } catch (IOException e) {
            DLog.e(TAG, "读取 ARP 表失败，ip=" + ipAddress, e);
        }
        return null;
    }

    private static boolean waitBeforeNextAttempt(String ipAddress, int attempt) {
        if (attempt >= MAX_ATTEMPTS - 1) {
            return true;
        }
        try {
            Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            DLog.w(TAG, "ARP 表解析被中断，ip=" + ipAddress, e);
            return false;
        }
    }

    private static String findMacAddressInLine(String ipAddress, String line) {
        String[] columns = line.trim().split("\\s+");
        if (columns.length < MIN_ARP_COLUMNS || !ipAddress.equals(columns[0])) {
            return null;
        }
        String macAddress = DeviceHistoryStore.normalizeMacAddress(columns[ARP_MAC_INDEX]);
        DLog.d(TAG, "从 ARP 行读取到 MAC=" + macAddress + ", ip=" + ipAddress);
        return macAddress;
    }
}

