package com.wifi.lib.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.github.yuweiguocn.library.greendao.MigrationHelper;
import com.wifi.lib.log.JLog;

import org.greenrobot.greendao.database.Database;

/**
 * greenDAO 升级入口。
 * 只关心表重建与迁移，不介入具体业务。
 */
public class WifiDeviceDbSQLiteOpenHelper extends DaoMaster.OpenHelper {
    private static final String TAG = "WifiDeviceDb";

    public WifiDeviceDbSQLiteOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        JLog.i(TAG, "db onUpgrade: " + oldVersion + "--->" + newVersion);
        super.onUpgrade(db, oldVersion, newVersion);
        MigrationHelper.migrate(db, new MigrationHelper.ReCreateAllTableListener() {
                    @Override
                    public void onCreateAllTables(Database db, boolean ifNotExists) {
                        DaoMaster.createAllTables(db, ifNotExists);
                    }

                    @Override
                    public void onDropAllTables(Database db, boolean ifExists) {
                        DaoMaster.dropAllTables(db, ifExists);
                    }
                },
                TrackedDeviceEntityDao.class,
                DeviceLogEntityDao.class);
    }
}
