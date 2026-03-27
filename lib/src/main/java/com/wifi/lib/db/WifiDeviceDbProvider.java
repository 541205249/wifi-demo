package com.wifi.lib.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

/**
 * Room Database 单例提供者。
 */
public final class WifiDeviceDbProvider {
    private static final String DB_NAME = "wifi-device-history-room.db";

    private static volatile WifiDeviceDatabase instance;

    private WifiDeviceDbProvider() {
    }

    @NonNull
    public static WifiDeviceDatabase getDatabase(@NonNull Context context) {
        WifiDeviceDatabase database = instance;
        if (database != null) {
            return database;
        }
        synchronized (WifiDeviceDbProvider.class) {
            database = instance;
            if (database == null) {
                database = Room.databaseBuilder(
                                context.getApplicationContext(),
                                WifiDeviceDatabase.class,
                                DB_NAME
                        )
                        .addMigrations(WifiDeviceDbMigrations.getAll())
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build();
                instance = database;
            }
        }
        return database;
    }
}
