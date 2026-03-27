package com.wifi.lib.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                TrackedDeviceEntity.class,
                DeviceLogEntity.class
        },
        version = WifiDeviceDatabase.VERSION,
        exportSchema = true
)
public abstract class WifiDeviceDatabase extends RoomDatabase {
    public static final int VERSION = 1;

    public abstract TrackedDeviceEntityDao getTrackedDeviceEntityDao();

    public abstract DeviceLogEntityDao getDeviceLogEntityDao();
}
