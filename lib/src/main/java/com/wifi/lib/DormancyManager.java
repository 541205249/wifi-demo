package com.wifi.lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class DormancyManager {

    private PowerManager.WakeLock wl;


    @SuppressLint("InvalidWakeLockTag")
    public void wakeAndUnlock(Context context) {
        //获取电源管理器对象
        //锁屏、唤醒相关
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        //获取PowerManager.WakeLock对象，后面的参数|表示同时传入两个值，最后的是调试用的Tag
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bright");
            //唤醒CPU
            wl.acquire(30 * 60 * 1000L /*30 minutes*/);
            Log.d("AppRun", "CPU保活30分钟..");
        } else {
            Log.d("AppRun", "唤醒失败...");
        }

    }

    public void unLock() {
        if (wl != null) {
            wl.release();
        }
    }

}
