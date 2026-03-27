package com.wifi.lib.log;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.logcat.JLogcatCollector;

import java.util.concurrent.CopyOnWriteArrayList;

public class JLog {
    private static volatile JLog instance;
    private static String defaultTag = "JLog";

    private final JLogcatCollector logcatCollector = new JLogcatCollector();
    private final CopyOnWriteArrayList<JLogObserver> observers = new CopyOnWriteArrayList<>();
    private JLogConfig logConfig;

    public static JLog get() {
        if (instance == null) {
            synchronized (JLog.class) {
                if (instance == null) {
                    instance = new JLog();
                }
            }
        }
        return instance;
    }

    public static void init(@NonNull JLogConfig logConfig) {
        JLog jLog = get();
        if (jLog.logConfig != null) {
            jLog.logcatCollector.stop();
        }
        jLog.logConfig = logConfig;
        if (!TextUtils.isEmpty(logConfig.getLogTag())) {
            defaultTag = logConfig.getLogTag();
        }
        jLog.logcatCollector.start(logConfig);
    }

    public static void stop() {
        if (instance != null) {
            instance.logcatCollector.stop();
        }
    }

    @Nullable
    public JLogConfig getLogConfig() {
        return logConfig;
    }

    @NonNull
    public JLogcatCollector getLogcatCollector() {
        return logcatCollector;
    }

    public static void saveLogsToFile() {
        get().logcatCollector.saveLogsToFile();
    }

    public static void addObserver(@NonNull JLogObserver observer) {
        get().observers.addIfAbsent(observer);
    }

    public static void removeObserver(@NonNull JLogObserver observer) {
        get().observers.remove(observer);
    }

    public static void d(String message) {
        d(defaultTag, message);
    }

    public static void d(String tag, String message) {
        log(Log.DEBUG, tag, message, null);
    }

    public static void i(String message) {
        i(defaultTag, message);
    }

    public static void i(String tag, String message) {
        log(Log.INFO, tag, message, null);
    }

    public static void w(String message) {
        w(defaultTag, message);
    }

    public static void w(String tag, String message) {
        log(Log.WARN, tag, message, null);
    }

    public static void w(String tag, Throwable throwable) {
        log(Log.WARN, tag, "", throwable);
    }

    public static void w(String tag, String message, Throwable throwable) {
        log(Log.WARN, tag, message, throwable);
    }

    public static void e(String message) {
        e(defaultTag, message);
    }

    public static void e(String tag, String message) {
        log(Log.ERROR, tag, message, null);
    }

    public static void e(String tag, Throwable throwable) {
        log(Log.ERROR, tag, "", throwable);
    }

    public static void e(String tag, String message, Throwable throwable) {
        log(Log.ERROR, tag, message, throwable);
    }

    public static void v(String message) {
        v(defaultTag, message);
    }

    public static void v(String tag, String message) {
        log(Log.VERBOSE, tag, message, null);
    }

    private static void log(int priority, String tag, String message, Throwable throwable) {
        String safeTag = TextUtils.isEmpty(tag) ? defaultTag : tag;
        String safeMessage = message == null ? "" : message;
        JLog jLog = get();

        switch (priority) {
            case Log.DEBUG:
                Log.d(safeTag, safeMessage, throwable);
                break;
            case Log.INFO:
                Log.i(safeTag, safeMessage, throwable);
                break;
            case Log.WARN:
                Log.w(safeTag, safeMessage, throwable);
                break;
            case Log.ERROR:
                Log.e(safeTag, safeMessage, throwable);
                break;
            default:
                Log.v(safeTag, safeMessage, throwable);
                break;
        }

        if (jLog.logConfig != null && jLog.logConfig.isSaveLogEnable()) {
            jLog.logcatCollector.record(priority, safeTag, safeMessage, throwable);
        }
        jLog.dispatch(new JLogEntry(
                System.currentTimeMillis(),
                priority,
                safeTag,
                safeMessage,
                throwable,
                Thread.currentThread().getName()
        ));
    }

    private void dispatch(@NonNull JLogEntry entry) {
        for (JLogObserver observer : observers) {
            observer.onLog(entry);
        }
    }
}
