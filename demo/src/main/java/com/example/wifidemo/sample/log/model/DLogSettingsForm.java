package com.example.wifidemo.sample.log.model;

import androidx.annotation.NonNull;

import com.wifi.lib.log.JLogConfig;

import java.math.BigDecimal;

public class DLogSettingsForm {
    private final boolean saveLogEnable;
    private final boolean monitorCrashLog;
    private final boolean overlayVisible;
    @NonNull
    private final String logTag;
    @NonNull
    private final String logDirectoryPath;
    @NonNull
    private final String maxLogStorageDays;
    @NonNull
    private final String unzipCode;
    @NonNull
    private final String singleFileSizeLimit;
    @NonNull
    private final String dailyFilesLimit;
    @NonNull
    private final String memoryBufferLimit;

    public DLogSettingsForm(
            boolean saveLogEnable,
            boolean monitorCrashLog,
            boolean overlayVisible,
            @NonNull String logTag,
            @NonNull String logDirectoryPath,
            @NonNull String maxLogStorageDays,
            @NonNull String unzipCode,
            @NonNull String singleFileSizeLimit,
            @NonNull String dailyFilesLimit,
            @NonNull String memoryBufferLimit
    ) {
        this.saveLogEnable = saveLogEnable;
        this.monitorCrashLog = monitorCrashLog;
        this.overlayVisible = overlayVisible;
        this.logTag = logTag;
        this.logDirectoryPath = logDirectoryPath;
        this.maxLogStorageDays = maxLogStorageDays;
        this.unzipCode = unzipCode;
        this.singleFileSizeLimit = singleFileSizeLimit;
        this.dailyFilesLimit = dailyFilesLimit;
        this.memoryBufferLimit = memoryBufferLimit;
    }

    @NonNull
    public static DLogSettingsForm fromConfig(@NonNull JLogConfig config) {
        return fromConfig(config, true);
    }

    @NonNull
    public static DLogSettingsForm fromConfig(@NonNull JLogConfig config, boolean overlayVisible) {
        return new DLogSettingsForm(
                config.isSaveLogEnable(),
                config.isMonitorCrashLog(),
                overlayVisible,
                config.getLogTag(),
                config.getLogDirectoryPath(),
                String.valueOf(config.getMaxLogStorageDays()),
                config.getUnzipCode(),
                formatDecimal(config.getSingleFileSizeLimit()),
                String.valueOf(config.getDailyFilesLimit()),
                formatDecimal(config.getMemoryBufferLimit())
        );
    }

    public boolean isSaveLogEnable() {
        return saveLogEnable;
    }

    public boolean isMonitorCrashLog() {
        return monitorCrashLog;
    }

    public boolean isOverlayVisible() {
        return overlayVisible;
    }

    @NonNull
    public String getLogTag() {
        return logTag;
    }

    @NonNull
    public String getLogDirectoryPath() {
        return logDirectoryPath;
    }

    @NonNull
    public String getMaxLogStorageDays() {
        return maxLogStorageDays;
    }

    @NonNull
    public String getUnzipCode() {
        return unzipCode;
    }

    @NonNull
    public String getSingleFileSizeLimit() {
        return singleFileSizeLimit;
    }

    @NonNull
    public String getDailyFilesLimit() {
        return dailyFilesLimit;
    }

    @NonNull
    public String getMemoryBufferLimit() {
        return memoryBufferLimit;
    }

    @NonNull
    private static String formatDecimal(float value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
