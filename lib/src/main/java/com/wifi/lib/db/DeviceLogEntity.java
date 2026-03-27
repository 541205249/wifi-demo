package com.wifi.lib.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "DEVICE_LOG_ENTITY",
        indices = {
                @Index(value = {"DEVICE_ID"}),
                @Index(value = {"TIMESTAMP"})
        }
)
public class DeviceLogEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "DEVICE_ID")
    private String deviceId = "";

    @NonNull
    @ColumnInfo(name = "CATEGORY")
    private String category = "";

    @NonNull
    @ColumnInfo(name = "ACTION")
    private String action = "";

    @ColumnInfo(name = "MESSAGE")
    private String message;

    @ColumnInfo(name = "REMOTE_IP")
    private String remoteIp;

    @ColumnInfo(name = "REMOTE_PORT")
    private Integer remotePort;

    @ColumnInfo(name = "LOCAL_IP")
    private String localIp;

    @ColumnInfo(name = "LOCAL_PORT")
    private Integer localPort;

    @ColumnInfo(name = "TIMESTAMP")
    private long timestamp;

    public DeviceLogEntity() {
    }

    @Ignore
    public DeviceLogEntity(
            Long id,
            String deviceId,
            String category,
            String action,
            String message,
            String remoteIp,
            Integer remotePort,
            String localIp,
            Integer localPort,
            long timestamp
    ) {
        this.id = id;
        this.deviceId = deviceId == null ? "" : deviceId;
        this.category = category == null ? "" : category;
        this.action = action == null ? "" : action;
        this.message = message;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.localIp = localIp;
        this.localPort = localPort;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId == null ? "" : deviceId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category == null ? "" : category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action == null ? "" : action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public String getLocalIp() {
        return localIp;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
