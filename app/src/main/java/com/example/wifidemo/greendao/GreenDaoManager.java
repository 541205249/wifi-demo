package com.example.wifidemo.greendao;

import android.content.Context;

import org.greenrobot.greendao.database.Database;

/**
 * 数据库对象按应用级单例持有，符合 greenDAO 官方 FAQ 的建议。
 */
public class GreenDaoManager {
    private static final String DATABASE_NAME = "wifi-device-history.db";

    private static volatile GreenDaoManager instance;

    private final DaoSession daoSession;

    private GreenDaoManager(Context context) {
        Context appContext = context.getApplicationContext();
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(appContext, DATABASE_NAME);
        Database database = helper.getWritableDb();
        daoSession = new DaoMaster(database).newSession();
    }

    public static GreenDaoManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GreenDaoManager.class) {
                if (instance == null) {
                    instance = new GreenDaoManager(context);
                }
            }
        }
        return instance;
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public TrackedDeviceEntityDao getTrackedDeviceDao() {
        return daoSession.getTrackedDeviceEntityDao();
    }

    public DeviceLogEntityDao getDeviceLogDao() {
        return daoSession.getDeviceLogEntityDao();
    }
}
