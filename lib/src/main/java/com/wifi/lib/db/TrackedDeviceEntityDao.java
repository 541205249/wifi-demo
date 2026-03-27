package com.wifi.lib.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TrackedDeviceEntityDao {
    @Query("SELECT * FROM TRACKED_DEVICE_ENTITY")
    List<TrackedDeviceEntity> loadAll();

    @Query("SELECT * FROM TRACKED_DEVICE_ENTITY ORDER BY LAST_SEEN_AT DESC")
    List<TrackedDeviceEntity> loadAllOrderByLastSeenDesc();

    @Query("SELECT * FROM TRACKED_DEVICE_ENTITY WHERE DEVICE_ID = :deviceId LIMIT 1")
    TrackedDeviceEntity findByDeviceId(String deviceId);

    @Insert
    long insert(TrackedDeviceEntity entity);

    @Update
    int update(TrackedDeviceEntity entity);
}
