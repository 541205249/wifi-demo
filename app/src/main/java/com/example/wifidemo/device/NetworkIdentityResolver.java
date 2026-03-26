package com.example.wifidemo.device;

import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * 从本地 ARP 表中解析对端设备的 MAC 地址。
 */
public final class NetworkIdentityResolver {
    private static final String ARP_TABLE_PATH = "/proc/net/arp";
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 150L;

    private NetworkIdentityResolver() {
    }

    public static String resolveMacAddress(String ipAddress) {
        if (TextUtils.isEmpty(ipAddress)) {
            return null;
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String macAddress = readMacFromArp(ipAddress);
            if (!TextUtils.isEmpty(macAddress)) {
                return macAddress;
            }

            if (attempt < MAX_ATTEMPTS - 1) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private static String readMacFromArp(String ipAddress) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARP_TABLE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.trim().split("\\s+");
                if (columns.length < 4) {
                    continue;
                }
                if (ipAddress.equals(columns[0])) {
                    return DeviceHistoryStore.normalizeMacAddress(columns[3]);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
