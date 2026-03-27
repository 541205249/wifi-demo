package com.wifi.lib.utils;

import android.annotation.SuppressLint;
import android.content.Context;

/**
 * 全局可使用的context，一定要在Application中初始化，否则报空指针
 */
public class AppContext {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context get() {
        return context.getApplicationContext();
    }

    public static void setContext(Context context) {
        AppContext.context = context;
    }
}
