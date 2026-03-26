package com.wifi.lib.db;

/**
 * greenDAO Session 提供者。
 * 仅负责持有 Session，不承载业务逻辑。
 */
public class WifiDeviceDbSessionProvider {
    private static volatile WifiDeviceDbSessionProvider instance;

    private DaoSession daoSession;

    private WifiDeviceDbSessionProvider() {
    }

    public static WifiDeviceDbSessionProvider getInstance() {
        if (instance == null) {
            synchronized (WifiDeviceDbSessionProvider.class) {
                if (instance == null) {
                    instance = new WifiDeviceDbSessionProvider();
                }
            }
        }
        return instance;
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public void setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
    }
}
