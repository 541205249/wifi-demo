package com.wifi.lib.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "TRACKED_DEVICE_ENTITY",
        indices = {
                @Index(value = {"DEVICE_ID"}, unique = true)
        }
)
public class TrackedDeviceEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Long id;

    @NonNull
    @ColumnInfo(name = "DEVICE_ID")
    private String deviceId = "";

    @ColumnInfo(name = "MAC_ADDRESS")
    private String macAddress;

    @ColumnInfo(name = "LAST_KNOWN_IP")
    private String lastKnownIp;

    @ColumnInfo(name = "LAST_KNOWN_PORT")
    private Integer lastKnownPort;

    @ColumnInfo(name = "LAST_LOCAL_IP")
    private String lastLocalIp;

    @ColumnInfo(name = "LAST_LOCAL_PORT")
    private Integer lastLocalPort;

    @ColumnInfo(name = "LAST_SEEN_AT")
    private long lastSeenAt;

    @ColumnInfo(name = "CURRENTLY_CONNECTED")
    private boolean currentlyConnected;

    @ColumnInfo(name = "COMMUNICATION_COUNT")
    private long communicationCount;

    @ColumnInfo(name = "CONNECTION_COUNT")
    private long connectionCount;

    public TrackedDeviceEntity() {
    }

    @Ignore
    public TrackedDeviceEntity(
            Long id,
            String deviceId,
            String macAddress,
            String lastKnownIp,
            Integer lastKnownPort,
            String lastLocalIp,
            Integer lastLocalPort,
            long lastSeenAt,
            boolean currentlyConnected,
            long communicationCount,
            long connectionCount
    ) {
        this.id = id;
        this.deviceId = deviceId == null ? "" : deviceId;
        this.macAddress = macAddress;
        this.lastKnownIp = lastKnownIp;
        this.lastKnownPort = lastKnownPort;
        this.lastLocalIp = lastLocalIp;
        this.lastLocalPort = lastLocalPort;
        this.lastSeenAt = lastSeenAt;
        this.currentlyConnected = currentlyConnected;
        this.communicationCount = communicationCount;
        this.connectionCount = connectionCount;
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

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getLastKnownIp() {
        return lastKnownIp;
    }

    public void setLastKnownIp(String lastKnownIp) {
        this.lastKnownIp = lastKnownIp;
    }

    public Integer getLastKnownPort() {
        return lastKnownPort;
    }

    public void setLastKnownPort(Integer lastKnownPort) {
        this.lastKnownPort = lastKnownPort;
    }

    public String getLastLocalIp() {
        return lastLocalIp;
    }

    public void setLastLocalIp(String lastLocalIp) {
        this.lastLocalIp = lastLocalIp;
    }

    public Integer getLastLocalPort() {
        return lastLocalPort;
    }

    public void setLastLocalPort(Integer lastLocalPort) {
        this.lastLocalPort = lastLocalPort;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean getCurrentlyConnected() {
        return currentlyConnected;
    }

    public void setCurrentlyConnected(boolean currentlyConnected) {
        this.currentlyConnected = currentlyConnected;
    }

    public long getCommunicationCount() {
        return communicationCount;
    }

    public void setCommunicationCount(long communicationCount) {
        this.communicationCount = communicationCount;
    }

    public long getConnectionCount() {
        return connectionCount;
    }

    public void setConnectionCount(long connectionCount) {
        this.connectionCount = connectionCount;
    }
}
