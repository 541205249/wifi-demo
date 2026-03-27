package com.example.wifidemo;

import android.app.Application;

import com.example.wifidemo.sample.log.data.DLogSettingsRepository;
import com.wifi.lib.flowdebug.FlowDebugOverlay;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;
import com.wifi.lib.utils.AppContext;

public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContext.setContext(this);
        DLogSettingsRepository repository = DLogSettingsRepository.getInstance(this);
        DLog.init(repository.loadStoredOrDefaultConfig());
        FlowDebugOverlay.install(this);
        FlowDebugOverlay.setVisible(repository.loadStoredOverlayVisible());
        JLog.i("DemoApplication", "JLog initialized");
    }
}
