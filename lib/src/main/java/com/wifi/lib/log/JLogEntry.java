package com.wifi.lib.log;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class JLogEntry {
    private final long timestamp;
    private final int priority;
    @NonNull
    private final String tag;
    @NonNull
    private final String message;
    @Nullable
    private final Throwable throwable;
    @NonNull
    private final String threadName;

    public JLogEntry(
            long timestamp,
            int priority,
            @NonNull String tag,
            @NonNull String message,
            @Nullable Throwable throwable,
            @NonNull String threadName
    ) {
        this.timestamp = timestamp;
        this.priority = priority;
        this.tag = tag;
        this.message = message;
        this.throwable = throwable;
        this.threadName = threadName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getPriority() {
        return priority;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    @NonNull
    public String getThreadName() {
        return threadName;
    }

    @NonNull
    public String getDisplayMessage() {
        if (throwable == null) {
            return message;
        }
        String trace = Log.getStackTraceString(throwable);
        if (message.isEmpty()) {
            return trace;
        }
        return message + "\n" + trace;
    }

    @NonNull
    public String getPriorityLabel() {
        switch (priority) {
            case Log.DEBUG:
                return "D";
            case Log.INFO:
                return "I";
            case Log.WARN:
                return "W";
            case Log.ERROR:
                return "E";
            default:
                return "V";
        }
    }
}
