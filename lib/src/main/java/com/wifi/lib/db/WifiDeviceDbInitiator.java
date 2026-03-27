package com.wifi.lib.db;

import android.content.Context;

/**
 * 数据库初始化入口。
 * 职责仅限于预热 Room Database 单例。
 */
public class WifiDeviceDbInitiator {
    public synchronized void setup(Context context) {
        WifiDeviceDbProvider.getDatabase(context.getApplicationContext());
    }
}
