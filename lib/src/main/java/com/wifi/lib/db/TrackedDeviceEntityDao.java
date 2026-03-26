package com.wifi.lib.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;

public class TrackedDeviceEntityDao extends AbstractDao<TrackedDeviceEntity, Long> {
    public static final String TABLENAME = "TRACKED_DEVICE_ENTITY";

    public static class Properties {
        public static final Property Id = new Property(0, Long.class, "id", true, "_id");
        public static final Property DeviceId = new Property(1, String.class, "deviceId", false, "DEVICE_ID");
        public static final Property MacAddress = new Property(2, String.class, "macAddress", false, "MAC_ADDRESS");
        public static final Property LastKnownIp = new Property(3, String.class, "lastKnownIp", false, "LAST_KNOWN_IP");
        public static final Property LastKnownPort = new Property(4, Integer.class, "lastKnownPort", false, "LAST_KNOWN_PORT");
        public static final Property LastLocalIp = new Property(5, String.class, "lastLocalIp", false, "LAST_LOCAL_IP");
        public static final Property LastLocalPort = new Property(6, Integer.class, "lastLocalPort", false, "LAST_LOCAL_PORT");
        public static final Property LastSeenAt = new Property(7, long.class, "lastSeenAt", false, "LAST_SEEN_AT");
        public static final Property CurrentlyConnected = new Property(8, boolean.class, "currentlyConnected", false, "CURRENTLY_CONNECTED");
        public static final Property CommunicationCount = new Property(9, long.class, "communicationCount", false, "COMMUNICATION_COUNT");
        public static final Property ConnectionCount = new Property(10, long.class, "connectionCount", false, "CONNECTION_COUNT");
    }

    public TrackedDeviceEntityDao(DaoConfig config) {
        super(config);
    }

    public TrackedDeviceEntityDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists ? "IF NOT EXISTS " : "";
        db.execSQL("CREATE TABLE " + constraint + "\"TRACKED_DEVICE_ENTITY\" (" +
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT," +
                "\"DEVICE_ID\" TEXT NOT NULL UNIQUE," +
                "\"MAC_ADDRESS\" TEXT," +
                "\"LAST_KNOWN_IP\" TEXT," +
                "\"LAST_KNOWN_PORT\" INTEGER," +
                "\"LAST_LOCAL_IP\" TEXT," +
                "\"LAST_LOCAL_PORT\" INTEGER," +
                "\"LAST_SEEN_AT\" INTEGER NOT NULL," +
                "\"CURRENTLY_CONNECTED\" INTEGER NOT NULL," +
                "\"COMMUNICATION_COUNT\" INTEGER NOT NULL," +
                "\"CONNECTION_COUNT\" INTEGER NOT NULL);");
    }

    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"TRACKED_DEVICE_ENTITY\"";
        db.execSQL(sql);
    }

    @Override
    protected void bindValues(DatabaseStatement stmt, TrackedDeviceEntity entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getDeviceId());
        if (entity.getMacAddress() != null) {
            stmt.bindString(3, entity.getMacAddress());
        }
        if (entity.getLastKnownIp() != null) {
            stmt.bindString(4, entity.getLastKnownIp());
        }
        if (entity.getLastKnownPort() != null) {
            stmt.bindLong(5, entity.getLastKnownPort());
        }
        if (entity.getLastLocalIp() != null) {
            stmt.bindString(6, entity.getLastLocalIp());
        }
        if (entity.getLastLocalPort() != null) {
            stmt.bindLong(7, entity.getLastLocalPort());
        }
        stmt.bindLong(8, entity.getLastSeenAt());
        stmt.bindLong(9, entity.getCurrentlyConnected() ? 1L : 0L);
        stmt.bindLong(10, entity.getCommunicationCount());
        stmt.bindLong(11, entity.getConnectionCount());
    }

    @Override
    protected void bindValues(SQLiteStatement stmt, TrackedDeviceEntity entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getDeviceId());
        if (entity.getMacAddress() != null) {
            stmt.bindString(3, entity.getMacAddress());
        }
        if (entity.getLastKnownIp() != null) {
            stmt.bindString(4, entity.getLastKnownIp());
        }
        if (entity.getLastKnownPort() != null) {
            stmt.bindLong(5, entity.getLastKnownPort());
        }
        if (entity.getLastLocalIp() != null) {
            stmt.bindString(6, entity.getLastLocalIp());
        }
        if (entity.getLastLocalPort() != null) {
            stmt.bindLong(7, entity.getLastLocalPort());
        }
        stmt.bindLong(8, entity.getLastSeenAt());
        stmt.bindLong(9, entity.getCurrentlyConnected() ? 1L : 0L);
        stmt.bindLong(10, entity.getCommunicationCount());
        stmt.bindLong(11, entity.getConnectionCount());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset) ? null : cursor.getLong(offset);
    }

    @Override
    public TrackedDeviceEntity readEntity(Cursor cursor, int offset) {
        return new TrackedDeviceEntity(
                cursor.isNull(offset) ? null : cursor.getLong(offset),
                cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1),
                cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2),
                cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3),
                cursor.isNull(offset + 4) ? null : cursor.getInt(offset + 4),
                cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5),
                cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6),
                cursor.getLong(offset + 7),
                cursor.getShort(offset + 8) != 0,
                cursor.getLong(offset + 9),
                cursor.getLong(offset + 10)
        );
    }

    @Override
    public void readEntity(Cursor cursor, TrackedDeviceEntity entity, int offset) {
        entity.setId(cursor.isNull(offset) ? null : cursor.getLong(offset));
        entity.setDeviceId(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setMacAddress(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setLastKnownIp(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setLastKnownPort(cursor.isNull(offset + 4) ? null : cursor.getInt(offset + 4));
        entity.setLastLocalIp(cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5));
        entity.setLastLocalPort(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setLastSeenAt(cursor.getLong(offset + 7));
        entity.setCurrentlyConnected(cursor.getShort(offset + 8) != 0);
        entity.setCommunicationCount(cursor.getLong(offset + 9));
        entity.setConnectionCount(cursor.getLong(offset + 10));
    }

    @Override
    protected Long updateKeyAfterInsert(TrackedDeviceEntity entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    @Override
    public Long getKey(TrackedDeviceEntity entity) {
        return entity != null ? entity.getId() : null;
    }

    @Override
    public boolean hasKey(TrackedDeviceEntity entity) {
        return entity != null && entity.getId() != null;
    }

    @Override
    protected boolean isEntityUpdateable() {
        return true;
    }
}
