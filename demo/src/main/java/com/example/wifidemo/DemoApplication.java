package com.example.wifidemo;

import android.app.Application;

import com.wifi.lib.log.JLog;
import com.wifi.lib.log.JLogConfig;
import com.wifi.lib.utils.AppContext;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.setContext(this);
        JLog.init(new JLogConfig.Builder(this)
                .setLogTag("WifiDemo")
                .setSaveLogEnable(true)
                .setMonitorCrashLog(true)
                .build());
        JLog.i("DemoApplication", "JLog initialized");
    }
}
