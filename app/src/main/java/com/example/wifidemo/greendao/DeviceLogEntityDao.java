package com.example.wifidemo.greendao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.internal.DaoConfig;

public class DeviceLogEntityDao extends AbstractDao<DeviceLogEntity, Long> {
    public static final String TABLENAME = "DEVICE_LOG_ENTITY";

    public static class Properties {
        public static final Property Id = new Property(0, Long.class, "id", true, "_id");
        public static final Property DeviceId = new Property(1, String.class, "deviceId", false, "DEVICE_ID");
        public static final Property Category = new Property(2, String.class, "category", false, "CATEGORY");
        public static final Property Action = new Property(3, String.class, "action", false, "ACTION");
        public static final Property Message = new Property(4, String.class, "message", false, "MESSAGE");
        public static final Property RemoteIp = new Property(5, String.class, "remoteIp", false, "REMOTE_IP");
        public static final Property RemotePort = new Property(6, Integer.class, "remotePort", false, "REMOTE_PORT");
        public static final Property LocalIp = new Property(7, String.class, "localIp", false, "LOCAL_IP");
        public static final Property LocalPort = new Property(8, Integer.class, "localPort", false, "LOCAL_PORT");
        public static final Property Timestamp = new Property(9, long.class, "timestamp", false, "TIMESTAMP");
    }

    public DeviceLogEntityDao(DaoConfig config) {
        super(config);
    }

    public DeviceLogEntityDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists ? "IF NOT EXISTS " : "";
        db.execSQL("CREATE TABLE " + constraint + "\"DEVICE_LOG_ENTITY\" (" +
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT," +
                "\"DEVICE_ID\" TEXT NOT NULL," +
                "\"CATEGORY\" TEXT NOT NULL," +
                "\"ACTION\" TEXT NOT NULL," +
                "\"MESSAGE\" TEXT," +
                "\"REMOTE_IP\" TEXT," +
                "\"REMOTE_PORT\" INTEGER," +
                "\"LOCAL_IP\" TEXT," +
                "\"LOCAL_PORT\" INTEGER," +
                "\"TIMESTAMP\" INTEGER NOT NULL);");
        db.execSQL("CREATE INDEX " + constraint + "\"IDX_DEVICE_LOG_ENTITY_DEVICE_ID\" ON \"DEVICE_LOG_ENTITY\" (\"DEVICE_ID\");");
        db.execSQL("CREATE INDEX " + constraint + "\"IDX_DEVICE_LOG_ENTITY_TIMESTAMP\" ON \"DEVICE_LOG_ENTITY\" (\"TIMESTAMP\");");
    }

    public static void dropTable(Database db, boolean ifExists) {
        db.execSQL("DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"DEVICE_LOG_ENTITY\"");
        db.execSQL("DROP INDEX " + (ifExists ? "IF EXISTS " : "") + "\"IDX_DEVICE_LOG_ENTITY_DEVICE_ID\"");
        db.execSQL("DROP INDEX " + (ifExists ? "IF EXISTS " : "") + "\"IDX_DEVICE_LOG_ENTITY_TIMESTAMP\"");
    }

    @Override
    protected void bindValues(DatabaseStatement stmt, DeviceLogEntity entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getDeviceId());
        stmt.bindString(3, entity.getCategory());
        stmt.bindString(4, entity.getAction());
        if (entity.getMessage() != null) {
            stmt.bindString(5, entity.getMessage());
        }
        if (entity.getRemoteIp() != null) {
            stmt.bindString(6, entity.getRemoteIp());
        }
        if (entity.getRemotePort() != null) {
            stmt.bindLong(7, entity.getRemotePort());
        }
        if (entity.getLocalIp() != null) {
            stmt.bindString(8, entity.getLocalIp());
        }
        if (entity.getLocalPort() != null) {
            stmt.bindLong(9, entity.getLocalPort());
        }
        stmt.bindLong(10, entity.getTimestamp());
    }

    @Override
    protected void bindValues(SQLiteStatement stmt, DeviceLogEntity entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindString(2, entity.getDeviceId());
        stmt.bindString(3, entity.getCategory());
        stmt.bindString(4, entity.getAction());
        if (entity.getMessage() != null) {
            stmt.bindString(5, entity.getMessage());
        }
        if (entity.getRemoteIp() != null) {
            stmt.bindString(6, entity.getRemoteIp());
        }
        if (entity.getRemotePort() != null) {
            stmt.bindLong(7, entity.getRemotePort());
        }
        if (entity.getLocalIp() != null) {
            stmt.bindString(8, entity.getLocalIp());
        }
        if (entity.getLocalPort() != null) {
            stmt.bindLong(9, entity.getLocalPort());
        }
        stmt.bindLong(10, entity.getTimestamp());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset) ? null : cursor.getLong(offset);
    }

    @Override
    public DeviceLogEntity readEntity(Cursor cursor, int offset) {
        return new DeviceLogEntity(
                cursor.isNull(offset) ? null : cursor.getLong(offset),
                cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1),
                cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2),
                cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3),
                cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4),
                cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5),
                cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6),
                cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7),
                cursor.isNull(offset + 8) ? null : cursor.getInt(offset + 8),
                cursor.getLong(offset + 9)
        );
    }

    @Override
    public void readEntity(Cursor cursor, DeviceLogEntity entity, int offset) {
        entity.setId(cursor.isNull(offset) ? null : cursor.getLong(offset));
        entity.setDeviceId(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setCategory(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setAction(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setMessage(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setRemoteIp(cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5));
        entity.setRemotePort(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setLocalIp(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
        entity.setLocalPort(cursor.isNull(offset + 8) ? null : cursor.getInt(offset + 8));
        entity.setTimestamp(cursor.getLong(offset + 9));
    }

    @Override
    protected Long updateKeyAfterInsert(DeviceLogEntity entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    @Override
    public Long getKey(DeviceLogEntity entity) {
        return entity != null ? entity.getId() : null;
    }

    @Override
    public boolean hasKey(DeviceLogEntity entity) {
        return entity != null && entity.getId() != null;
    }

    @Override
    protected boolean isEntityUpdateable() {
        return true;
    }
}
