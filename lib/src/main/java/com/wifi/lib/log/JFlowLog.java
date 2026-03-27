package com.wifi.lib.log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Deprecated
public final class JFlowLog {
    public static final String TAG = DLog.TAG;

    private JFlowLog() {
    }

    public static void d(@NonNull String source, @Nullable String message) {
        DLog.d(source, message);
    }

    public static void i(@NonNull String source, @Nullable String message) {
        DLog.i(source, message);
    }

    public static void w(@NonNull String source, @Nullable String message) {
        DLog.w(source, message);
    }

    public static void w(@NonNull String source, @Nullable String message, @Nullable Throwable throwable) {
        DLog.w(source, message, throwable);
    }

    public static void e(@NonNull String source, @Nullable String message) {
        DLog.e(source, message);
    }

    public static void e(@NonNull String source, @Nullable String message, @Nullable Throwable throwable) {
        DLog.e(source, message, throwable);
    }
}
