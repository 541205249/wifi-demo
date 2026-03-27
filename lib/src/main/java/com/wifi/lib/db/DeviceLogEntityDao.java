package com.wifi.lib.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DeviceLogEntityDao {
    @Insert
    long insert(DeviceLogEntity entity);

    @Delete
    int delete(DeviceLogEntity entity);

    @Query("SELECT * FROM DEVICE_LOG_ENTITY WHERE DEVICE_ID = :deviceId ORDER BY TIMESTAMP DESC, _id DESC")
    List<DeviceLogEntity> loadByDeviceIdOrderByTimestampDesc(String deviceId);

    @Query("SELECT * FROM DEVICE_LOG_ENTITY WHERE DEVICE_ID = :deviceId AND CATEGORY = :category ORDER BY TIMESTAMP DESC, _id DESC")
    List<DeviceLogEntity> loadByDeviceIdAndCategoryOrderByTimestampDesc(String deviceId, String category);

    @Query("SELECT COUNT(*) FROM DEVICE_LOG_ENTITY WHERE DEVICE_ID = :deviceId")
    int countByDeviceId(String deviceId);

    @Query("DELETE FROM DEVICE_LOG_ENTITY WHERE _id IN (" +
            "SELECT _id FROM DEVICE_LOG_ENTITY " +
            "WHERE DEVICE_ID = :deviceId " +
            "ORDER BY TIMESTAMP ASC, _id ASC " +
            "LIMIT :count" +
            ")")
    int deleteOldestLogs(String deviceId, int count);
}
