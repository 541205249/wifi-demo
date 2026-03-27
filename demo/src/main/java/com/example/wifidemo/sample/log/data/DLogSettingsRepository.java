package com.example.wifidemo.sample.log.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.wifidemo.sample.log.model.DLogSettingsForm;
import com.wifi.lib.flowdebug.FlowDebugOverlay;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLogConfig;
import com.wifi.lib.mvvm.BaseRepository;

public class DLogSettingsRepository extends BaseRepository {
    private static final String TAG = "DLogSettingsRepo";
    private static final String PREF_NAME = "dlog_settings";
    private static final String KEY_SAVE_ENABLE = "save_enable";
    private static final String KEY_MONITOR_CRASH = "monitor_crash";
    private static final String KEY_OVERLAY_VISIBLE = "overlay_visible";
    private static final String KEY_LOG_TAG = "log_tag";
    private static final String KEY_LOG_DIRECTORY = "log_directory";
    private static final String KEY_MAX_STORAGE_DAYS = "max_storage_days";
    private static final String KEY_UNZIP_CODE = "unzip_code";
    private static final String KEY_SINGLE_FILE_SIZE = "single_file_size";
    private static final String KEY_DAILY_FILES_LIMIT = "daily_files_limit";
    private static final String KEY_MEMORY_BUFFER_LIMIT = "memory_buffer_limit";
    private static final String DEFAULT_LOG_TAG = "WifiDemo";

    private static volatile DLogSettingsRepository instance;

    private final Context appContext;
    private final SharedPreferences preferences;

    private DLogSettingsRepository(@NonNull Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static DLogSettingsRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (DLogSettingsRepository.class) {
                if (instance == null) {
                    instance = new DLogSettingsRepository(context);
                }
            }
        }
        return instance;
    }

    @NonNull
    public JLogConfig loadStoredOrDefaultConfig() {
        return buildConfig(readForm(loadDefaultForm()));
    }

    public boolean loadStoredOverlayVisible() {
        return preferences.getBoolean(KEY_OVERLAY_VISIBLE, loadDefaultForm().isOverlayVisible());
    }

    @NonNull
    public DLogSettingsForm loadEditableForm() {
        JLogConfig currentConfig = DLog.getConfig();
        if (currentConfig != null) {
            return DLogSettingsForm.fromConfig(currentConfig, FlowDebugOverlay.isVisible());
        }
        return readForm(loadDefaultForm());
    }

    @NonNull
    public DLogSettingsForm loadDefaultForm() {
        return DLogSettingsForm.fromConfig(
                DLog.newConfigBuilder(appContext)
                        .setLogTag(DEFAULT_LOG_TAG)
                        .setSaveLogEnable(true)
                        .setMonitorCrashLog(true)
                        .build(),
                true
        );
    }

    @NonNull
    public DLogSettingsForm applyAndPersist(@NonNull DLogSettingsForm form) {
        JLogConfig config = buildConfig(form);
        persist(config, form.isOverlayVisible());
        DLog.init(config);
        FlowDebugOverlay.setVisible(form.isOverlayVisible());
        DLog.i(TAG, "DLog 配置已应用，tag=" + config.getLogTag() + ", overlayVisible=" + form.isOverlayVisible());
        return DLogSettingsForm.fromConfig(config, form.isOverlayVisible());
    }

    @NonNull
    public String buildAppliedSummary(@NonNull DLogSettingsForm form) {
        return "日志标签: " + form.getLogTag()
                + "\n本地持久化: " + (form.isSaveLogEnable() ? "开启" : "关闭")
                + "\n崩溃监控: " + (form.isMonitorCrashLog() ? "开启" : "关闭")
                + "\n浮窗显示: " + (form.isOverlayVisible() ? "开启" : "关闭")
                + "\n日志目录: " + form.getLogDirectoryPath()
                + "\n日志保留天数: " + form.getMaxLogStorageDays()
                + "\n导出压缩码: " + form.getUnzipCode()
                + "\n单文件大小上限(MB): " + form.getSingleFileSizeLimit()
                + "\n每日文件数上限: " + form.getDailyFilesLimit()
                + "\n内存缓冲上限(MB): " + form.getMemoryBufferLimit();
    }

    @NonNull
    private DLogSettingsForm readForm(@NonNull DLogSettingsForm defaults) {
        return new DLogSettingsForm(
                preferences.getBoolean(KEY_SAVE_ENABLE, defaults.isSaveLogEnable()),
                preferences.getBoolean(KEY_MONITOR_CRASH, defaults.isMonitorCrashLog()),
                preferences.getBoolean(KEY_OVERLAY_VISIBLE, defaults.isOverlayVisible()),
                preferences.getString(KEY_LOG_TAG, defaults.getLogTag()),
                preferences.getString(KEY_LOG_DIRECTORY, defaults.getLogDirectoryPath()),
                preferences.getString(KEY_MAX_STORAGE_DAYS, defaults.getMaxLogStorageDays()),
                preferences.getString(KEY_UNZIP_CODE, defaults.getUnzipCode()),
                preferences.getString(KEY_SINGLE_FILE_SIZE, defaults.getSingleFileSizeLimit()),
                preferences.getString(KEY_DAILY_FILES_LIMIT, defaults.getDailyFilesLimit()),
                preferences.getString(KEY_MEMORY_BUFFER_LIMIT, defaults.getMemoryBufferLimit())
        );
    }

    @NonNull
    private JLogConfig buildConfig(@NonNull DLogSettingsForm form) {
        String logTag = requireText(form.getLogTag(), "日志标签不能为空");
        String logDirectoryPath = requireText(form.getLogDirectoryPath(), "日志目录不能为空");
        String unzipCode = requireText(form.getUnzipCode(), "导出压缩码不能为空");
        int maxLogStorageDays = parsePositiveInt(form.getMaxLogStorageDays(), "日志保留天数需要大于 0");
        int dailyFilesLimit = parsePositiveInt(form.getDailyFilesLimit(), "每日文件数上限需要大于 0");
        float singleFileSizeLimit = parsePositiveFloat(form.getSingleFileSizeLimit(), "单文件大小上限需要大于 0");
        float memoryBufferLimit = parsePositiveFloat(form.getMemoryBufferLimit(), "内存缓冲上限需要大于 0");

        return DLog.newConfigBuilder(appContext)
                .setSaveLogEnable(form.isSaveLogEnable())
                .setMonitorCrashLog(form.isMonitorCrashLog())
                .setLogTag(logTag)
                .setLogDirectoryPath(logDirectoryPath)
                .setMaxLogStorageDays(maxLogStorageDays)
                .setUnzipCode(unzipCode)
                .setSingleFileSizeLimit(singleFileSizeLimit)
                .setDailyFilesLimit(dailyFilesLimit)
                .setMemoryBufferLimit(memoryBufferLimit)
                .build();
    }

    private void persist(@NonNull JLogConfig config, boolean overlayVisible) {
        preferences.edit()
                .putBoolean(KEY_SAVE_ENABLE, config.isSaveLogEnable())
                .putBoolean(KEY_MONITOR_CRASH, config.isMonitorCrashLog())
                .putBoolean(KEY_OVERLAY_VISIBLE, overlayVisible)
                .putString(KEY_LOG_TAG, config.getLogTag())
                .putString(KEY_LOG_DIRECTORY, config.getLogDirectoryPath())
                .putString(KEY_MAX_STORAGE_DAYS, String.valueOf(config.getMaxLogStorageDays()))
                .putString(KEY_UNZIP_CODE, config.getUnzipCode())
                .putString(KEY_SINGLE_FILE_SIZE, trimNumber(config.getSingleFileSizeLimit()))
                .putString(KEY_DAILY_FILES_LIMIT, String.valueOf(config.getDailyFilesLimit()))
                .putString(KEY_MEMORY_BUFFER_LIMIT, trimNumber(config.getMemoryBufferLimit()))
                .apply();
    }

    @NonNull
    private String requireText(@NonNull String value, @NonNull String errorMessage) {
        String trimmed = value.trim();
        if (TextUtils.isEmpty(trimmed)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return trimmed;
    }

    private int parsePositiveInt(@NonNull String value, @NonNull String errorMessage) {
        try {
            int result = Integer.parseInt(value.trim());
            if (result <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return result;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private float parsePositiveFloat(@NonNull String value, @NonNull String errorMessage) {
        try {
            float result = Float.parseFloat(value.trim());
            if (result <= 0f) {
                throw new IllegalArgumentException(errorMessage);
            }
            return result;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @NonNull
    private String trimNumber(float value) {
        String stringValue = String.valueOf(value);
        if (stringValue.endsWith(".0")) {
            return stringValue.substring(0, stringValue.length() - 2);
        }
        return stringValue;
    }
}
