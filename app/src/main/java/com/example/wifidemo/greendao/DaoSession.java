package com.example.wifidemo.greendao;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import java.util.Map;

public class DaoSession extends AbstractDaoSession {
    private final DaoConfig trackedDeviceEntityDaoConfig;
    private final DaoConfig deviceLogEntityDaoConfig;

    private final TrackedDeviceEntityDao trackedDeviceEntityDao;
    private final DeviceLogEntityDao deviceLogEntityDao;

    public DaoSession(
            Database db,
            IdentityScopeType type,
            Map<Class<? extends AbstractDao<?, ?>>, DaoConfig> daoConfigMap
    ) {
        super(db);

        trackedDeviceEntityDaoConfig = daoConfigMap.get(TrackedDeviceEntityDao.class).clone();
        trackedDeviceEntityDaoConfig.initIdentityScope(type);

        deviceLogEntityDaoConfig = daoConfigMap.get(DeviceLogEntityDao.class).clone();
        deviceLogEntityDaoConfig.initIdentityScope(type);

        trackedDeviceEntityDao = new TrackedDeviceEntityDao(trackedDeviceEntityDaoConfig, this);
        deviceLogEntityDao = new DeviceLogEntityDao(deviceLogEntityDaoConfig, this);

        registerDao(TrackedDeviceEntity.class, trackedDeviceEntityDao);
        registerDao(DeviceLogEntity.class, deviceLogEntityDao);
    }

    public void clear() {
        trackedDeviceEntityDaoConfig.clearIdentityScope();
        deviceLogEntityDaoConfig.clearIdentityScope();
    }

    public TrackedDeviceEntityDao getTrackedDeviceEntityDao() {
        return trackedDeviceEntityDao;
    }

    public DeviceLogEntityDao getDeviceLogEntityDao() {
        return deviceLogEntityDao;
    }
}
