package com.wifi.lib.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * 数据库初始化入口。
 * 职责仅限于打开数据库并准备 DaoSession。
 */
public class WifiDeviceDbInitiator {
    private static final String DB_NAME = "wifi-device-history.db";

    public synchronized void setup(Context context) {
        if (WifiDeviceDbSessionProvider.getInstance().getDaoSession() != null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        WifiDeviceDbSQLiteOpenHelper helper = new WifiDeviceDbSQLiteOpenHelper(appContext, DB_NAME);
        SQLiteDatabase sqLiteDatabase = helper.getWritableDatabase();
        DaoSession session = new DaoMaster(sqLiteDatabase).newSession();
        WifiDeviceDbSessionProvider.getInstance().setDaoSession(session);
    }
}
