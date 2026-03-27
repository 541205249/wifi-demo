package com.wifi.lib.log;

import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.wifi.lib.log.zip.DLogZipDelegate;

import java.util.Map;
import java.util.WeakHashMap;

public class DLogExporter {
    private static volatile DLogExporter instance;
    private final Map<ComponentActivity, DLogZipDelegate> exportDelegateCache = new WeakHashMap<>();

    public static DLogExporter get() {
        if (instance == null) {
            synchronized (DLogExporter.class) {
                if (instance == null) {
                    instance = new DLogExporter();
                }
            }
        }
        return instance;
    }

    public void hookToExport(@NonNull ComponentActivity activity, @NonNull View targetView, DLog.Callback callback) {
        hook(activity, targetView, true, callback);
    }

    public void hookToShare(@NonNull ComponentActivity activity, @NonNull View targetView, DLog.Callback callback) {
        hook(activity, targetView, false, callback);
    }

    public void exportToLocalDirectory(@NonNull ComponentActivity activity, DLog.Callback callback) {
        DLogZipDelegate zipDelegate = getOrCreateExportDelegate(activity, callback);
        if (zipDelegate == null) {
            return;
        }
        zipDelegate.exportToLocalDirectory(new DelegateCallback(callback));
    }

    public void shareToSocialApp(@NonNull ComponentActivity activity, DLog.Callback callback) {
        DLogZipDelegate zipDelegate = DLogZipDelegate.withoutDirectoryPicker(activity);
        zipDelegate.shareToSocialApp(new DelegateCallback(callback));
    }

    public void shareToPlatform(@NonNull ComponentActivity activity, DLog.Callback callback) {
        shareToSocialApp(activity, callback);
    }

    private void hook(@NonNull ComponentActivity activity, @NonNull View targetView, boolean exportToLocal, DLog.Callback callback) {
        if (exportToLocal) {
            getOrCreateExportDelegate(activity, callback);
        }
        FiveClickTrigger trigger = new FiveClickTrigger();
        targetView.setOnClickListener(v -> {
            int remainingClicks = trigger.onClick();
            if (remainingClicks > 0) {
                return;
            }

            if (exportToLocal) {
                exportToLocalDirectory(activity, callback);
            } else {
                shareToSocialApp(activity, callback);
            }
        });
    }

    private DLogZipDelegate getOrCreateExportDelegate(@NonNull ComponentActivity activity, DLog.Callback callback) {
        DLogZipDelegate cachedDelegate = exportDelegateCache.get(activity);
        if (cachedDelegate != null) {
            return cachedDelegate;
        }

        if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            String message = "DLog 导出组件需要在页面初始化阶段完成注册，请先在 onCreate 或 onViewCreated 中调用 hookToExport";
            JLog.w("DLogExporter", message);
            if (callback != null) {
                callback.onError(message);
            }
            return null;
        }

        DLogZipDelegate delegate = DLogZipDelegate.withDirectoryPicker(activity);
        exportDelegateCache.put(activity, delegate);
        return delegate;
    }

    private static class DelegateCallback implements DLogZipDelegate.Callback {
        private final DLog.Callback callback;

        DelegateCallback(DLog.Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onComplete(@NonNull String resultMessage) {
            if (callback != null) {
                callback.onSuccess(resultMessage);
            }
        }

        @Override
        public void onError(@NonNull String errorMessage) {
            if (callback != null) {
                callback.onError(errorMessage);
            }
        }
    }

    private static class FiveClickTrigger {
        private static final long CONTINUOUS_CLICK_INTERVAL_MS = 500L;
        private static final int REQUIRED_CLICK_COUNT = 5;

        private int clickCount;
        private long lastClickTime;

        int onClick() {
            long now = System.currentTimeMillis();
            if (now - lastClickTime <= CONTINUOUS_CLICK_INTERVAL_MS) {
                clickCount++;
            } else {
                clickCount = 1;
            }
            lastClickTime = now;

            if (clickCount >= REQUIRED_CLICK_COUNT) {
                clickCount = 0;
                return 0;
            }
            return REQUIRED_CLICK_COUNT - clickCount;
        }
    }
}
