package com.wifi.optometry;

import android.app.Application;

import com.wifi.lib.log.JLog;
import com.wifi.lib.log.JLogConfig;
import com.wifi.lib.utils.AppContext;

public class OptometryApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.setContext(this);
        JLog.init(new JLogConfig.Builder(this)
                .setLogTag("Optometry")
                .setSaveLogEnable(true)
                .setMonitorCrashLog(true)
                .build());
        JLog.i("OptometryApplication", "JLog initialized");
    }
}
