package com.wifi.lib.log;

import android.view.View;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

import com.wifi.lib.log.zip.JZipDelegate;

public class JLogExporter {
    public interface Callback {
        void onSuccess(@NonNull String message);

        void onError(@NonNull String errorMessage);
    }

    private static final long CONTINUOUS_CLICK_INTERVAL_MS = 500L;
    private static final int REQUIRED_CLICK_COUNT = 5;

    private static volatile JLogExporter instance;

    public static JLogExporter get() {
        if (instance == null) {
            synchronized (JLogExporter.class) {
                if (instance == null) {
                    instance = new JLogExporter();
                }
            }
        }
        return instance;
    }

    public void hookToExport(@NonNull ComponentActivity activity, @NonNull View targetView, Callback callback) {
        hook(activity, targetView, true, callback);
    }

    public void hookToShare(@NonNull ComponentActivity activity, @NonNull View targetView, Callback callback) {
        hook(activity, targetView, false, callback);
    }

    public void exportToLocalDirectory(@NonNull ComponentActivity activity, Callback callback) {
        JZipDelegate zipDelegate = new JZipDelegate(activity);
        zipDelegate.exportToLocalDirectory(new DelegateCallback(callback));
    }

    public void shareToSocialApp(@NonNull ComponentActivity activity, Callback callback) {
        JZipDelegate zipDelegate = new JZipDelegate(activity);
        zipDelegate.shareToSocialApp(new DelegateCallback(callback));
    }

    private void hook(@NonNull ComponentActivity activity, @NonNull View targetView, boolean exportToLocal, Callback callback) {
        FiveClickTrigger trigger = new FiveClickTrigger();
        targetView.setOnClickListener(v -> {
            int remainingClicks = trigger.onClick();
            if (remainingClicks > 0) {
                Toast.makeText(activity, "再连续点击 " + remainingClicks + " 次即可触发日志导出", Toast.LENGTH_SHORT).show();
                return;
            }

            if (exportToLocal) {
                exportToLocalDirectory(activity, callback);
            } else {
                shareToSocialApp(activity, callback);
            }
        });
    }

    private static class DelegateCallback implements JZipDelegate.Callback {
        private final Callback callback;

        DelegateCallback(Callback callback) {
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
