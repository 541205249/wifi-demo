package com.wifi.lib.db;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;

/**
 * Room 数据库升级注册表。
 *
 * 升级约定：
 * 1. 先把 {@link WifiDeviceDatabase#VERSION} 加 1。
 * 2. 在这里新增一个 Migration(x, x + 1)。
 * 3. 把新增的 Migration 放进 ALL 数组，顺序按版本递增。
 * 4. 提交同步生成的 schema json，便于后续比对表结构变化。
 *
 * 例如：
 * private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
 *     @Override
 *     public void migrate(@NonNull SupportSQLiteDatabase database) {
 *         database.execSQL("ALTER TABLE TRACKED_DEVICE_ENTITY ADD COLUMN XXX TEXT");
 *     }
 * };
 */
public final class WifiDeviceDbMigrations {
    private static final Migration[] ALL = new Migration[0];

    private WifiDeviceDbMigrations() {
    }

    @NonNull
    public static Migration[] getAll() {
        return ALL;
    }
}
