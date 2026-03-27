package com.wifi.lib.log;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

public class JLogConfig {
    private final Context context;
    private final boolean saveLogEnable;
    private final boolean monitorCrashLog;
    private final String logTag;
    private final String logDirectoryPath;
    private final int maxLogStorageDays;
    private final String unzipCode;
    private final float singleFileSizeLimit;
    private final int dailyFilesLimit;
    private final float memoryBufferLimit;

    JLogConfig(
            Context context,
            boolean saveLogEnable,
            boolean monitorCrashLog,
            String logTag,
            String logDirectoryPath,
            int maxLogStorageDays,
            String unzipCode,
            float singleFileSizeLimit,
            int dailyFilesLimit,
            float memoryBufferLimit
    ) {
        this.context = context;
        this.saveLogEnable = saveLogEnable;
        this.monitorCrashLog = monitorCrashLog;
        this.logTag = logTag;
        this.logDirectoryPath = logDirectoryPath;
        this.maxLogStorageDays = maxLogStorageDays;
        this.unzipCode = unzipCode;
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.dailyFilesLimit = dailyFilesLimit;
        this.memoryBufferLimit = memoryBufferLimit;
    }

    public Context getContext() {
        return context;
    }

    public boolean isSaveLogEnable() {
        return saveLogEnable;
    }

    public boolean isMonitorCrashLog() {
        return monitorCrashLog;
    }

    public String getLogTag() {
        return logTag;
    }

    @NonNull
    public String getLogDirectoryPath() {
        return logDirectoryPath;
    }

    public int getMaxLogStorageDays() {
        return maxLogStorageDays;
    }

    public String getUnzipCode() {
        return unzipCode;
    }

    public float getSingleFileSizeLimit() {
        return singleFileSizeLimit;
    }

    public int getDailyFilesLimit() {
        return dailyFilesLimit;
    }

    public float getMemoryBufferLimit() {
        return memoryBufferLimit;
    }

    public static class Builder {
        private final Context context;
        private boolean saveLogEnable;
        private boolean monitorCrashLog;
        private String logTag = "JLog";
        private String logDirectoryPath;
        private int maxLogStorageDays = 2;
        private String unzipCode = "jlog0731";
        private float singleFileSizeLimit = 5f;
        private int dailyFilesLimit = 5;
        private float memoryBufferLimit = 6f;

        public Builder(@NonNull Context context) {
            this.context = context.getApplicationContext();
            File baseDir = this.context.getExternalFilesDir(null);
            if (baseDir == null) {
                baseDir = this.context.getFilesDir();
            }
            File logDir = new File(baseDir, "jlog_logs");
            logDirectoryPath = logDir.getAbsolutePath();
        }

        public Builder setSaveLogEnable(boolean saveLogEnable) {
            this.saveLogEnable = saveLogEnable;
            return this;
        }

        public Builder setMonitorCrashLog(boolean monitorCrashLog) {
            this.monitorCrashLog = monitorCrashLog;
            return this;
        }

        public Builder setLogTag(@NonNull String logTag) {
            this.logTag = logTag;
            return this;
        }

        public Builder setLogDirectoryPath(@NonNull String logDirectoryPath) {
            this.logDirectoryPath = logDirectoryPath;
            return this;
        }

        public Builder setMaxLogStorageDays(int maxLogStorageDays) {
            this.maxLogStorageDays = maxLogStorageDays;
            return this;
        }

        public Builder setUnzipCode(@NonNull String unzipCode) {
            this.unzipCode = unzipCode;
            return this;
        }

        public Builder setSingleFileSizeLimit(float singleFileSizeLimit) {
            this.singleFileSizeLimit = singleFileSizeLimit;
            return this;
        }

        public Builder setDailyFilesLimit(int dailyFilesLimit) {
            this.dailyFilesLimit = dailyFilesLimit;
            return this;
        }

        public Builder setMemoryBufferLimit(float memoryBufferLimit) {
            this.memoryBufferLimit = memoryBufferLimit;
            return this;
        }

        public JLogConfig build() {
            return new JLogConfig(
                    context,
                    saveLogEnable,
                    monitorCrashLog,
                    logTag,
                    logDirectoryPath,
                    maxLogStorageDays,
                    unzipCode,
                    singleFileSizeLimit,
                    dailyFilesLimit,
                    memoryBufferLimit
            );
        }
    }
}
