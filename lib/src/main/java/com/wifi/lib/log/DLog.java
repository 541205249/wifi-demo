package com.wifi.lib.log;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DLog extends JLog {
    public static final String TAG = "DLOG";

    public interface Callback extends JLogExporter.Callback {
    }

    private DLog() {
    }

    public static void init(@NonNull JLogConfig config) {
        JLog.init(config);
    }

    @Nullable
    public static JLogConfig getConfig() {
        return JLog.get().getLogConfig();
    }

    @NonNull
    public static JLogConfig.Builder newConfigBuilder(@NonNull Context context) {
        return new JLogConfig.Builder(context);
    }

    public static void saveLogsToFile() {
        JLog.saveLogsToFile();
    }

    public static void d(@NonNull String source, @Nullable String message) {
        JLog.d(TAG, buildMessage(source, message));
    }

    public static void i(@NonNull String source, @Nullable String message) {
        JLog.i(TAG, buildMessage(source, message));
    }

    public static void w(@NonNull String source, @Nullable String message) {
        JLog.w(TAG, buildMessage(source, message));
    }

    public static void w(@NonNull String source, @Nullable String message, @Nullable Throwable throwable) {
        JLog.w(TAG, buildMessage(source, message), throwable);
    }

    public static void e(@NonNull String source, @Nullable String message) {
        JLog.e(TAG, buildMessage(source, message));
    }

    public static void e(@NonNull String source, @Nullable String message, @Nullable Throwable throwable) {
        JLog.e(TAG, buildMessage(source, message), throwable);
    }

    public static void hookToExport(@NonNull ComponentActivity activity, @NonNull View targetView, @Nullable Callback callback) {
        DLogExporter.get().hookToExport(activity, targetView, callback);
    }

    public static void hookToExport(@NonNull ComponentActivity activity, @NonNull View targetView) {
        hookToExport(activity, targetView, null);
    }

    public static void hookToShare(@NonNull ComponentActivity activity, @NonNull View targetView, @Nullable Callback callback) {
        DLogExporter.get().hookToShare(activity, targetView, callback);
    }

    public static void hookToShare(@NonNull ComponentActivity activity, @NonNull View targetView) {
        hookToShare(activity, targetView, null);
    }

    public static void exportToLocalDirectory(@NonNull ComponentActivity activity, @Nullable Callback callback) {
        DLogExporter.get().exportToLocalDirectory(activity, callback);
    }

    public static void exportToLocalDirectory(@NonNull ComponentActivity activity) {
        exportToLocalDirectory(activity, null);
    }

    public static void shareToSocialApp(@NonNull ComponentActivity activity, @Nullable Callback callback) {
        DLogExporter.get().shareToSocialApp(activity, callback);
    }

    public static void shareToSocialApp(@NonNull ComponentActivity activity) {
        shareToSocialApp(activity, null);
    }

    public static void shareToPlatform(@NonNull ComponentActivity activity, @Nullable Callback callback) {
        DLogExporter.get().shareToPlatform(activity, callback);
    }

    public static void shareToPlatform(@NonNull ComponentActivity activity) {
        shareToPlatform(activity, null);
    }

    @NonNull
    private static String buildMessage(@NonNull String source, @Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            return "[" + source + "]";
        }
        return "[" + source + "] " + message;
    }
}
