package com.wifi.lib.flowdebug;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public final class FlowDebugOverlay {
    private static final FlowDebugOverlay INSTANCE = new FlowDebugOverlay();

    private boolean installed;
    private boolean overlayVisible = true;
    private boolean collapsed;
    private float overlayX = Float.NaN;
    private float overlayY = Float.NaN;
    @NonNull
    private WeakReference<Activity> currentActivityRef = new WeakReference<>(null);
    @Nullable
    private FlowDebugOverlayView overlayView;

    private final Application.ActivityLifecycleCallbacks lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            currentActivityRef = new WeakReference<>(activity);
            if (overlayVisible) {
                attachTo(activity);
            } else {
                detachOverlay();
            }
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            Activity currentActivity = currentActivityRef.get();
            if (currentActivity == activity) {
                currentActivityRef = new WeakReference<>(null);
            }
            if (overlayView != null) {
                ViewParent parent = overlayView.getParent();
                if (parent == activity.getWindow().getDecorView()) {
                    detachOverlay();
                }
            }
        }
    };

    private FlowDebugOverlay() {
    }

    public static void install(@NonNull Application application) {
        INSTANCE.installInternal(application);
    }

    public static void setVisible(boolean visible) {
        INSTANCE.setVisibleInternal(visible);
    }

    public static boolean isVisible() {
        return INSTANCE.isVisibleInternal();
    }

    private synchronized void installInternal(@NonNull Application application) {
        if (installed) {
            return;
        }
        FlowLogCenter.getInstance().start();
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        installed = true;
    }

    private synchronized void setVisibleInternal(boolean visible) {
        overlayVisible = visible;
        if (!visible) {
            detachOverlay();
            return;
        }
        Activity currentActivity = currentActivityRef.get();
        if (currentActivity != null && !currentActivity.isFinishing()) {
            attachTo(currentActivity);
        }
    }

    private synchronized boolean isVisibleInternal() {
        return overlayVisible;
    }

    private synchronized void attachTo(@NonNull Activity activity) {
        if (!overlayVisible || activity.isFinishing()) {
            return;
        }

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (overlayView != null && overlayView.getParent() == decorView) {
            return;
        }

        detachOverlay();

        FlowDebugOverlayView view = new FlowDebugOverlayView(activity);
        view.bind(collapsed, overlayX, overlayY, new FlowDebugOverlayView.Callback() {
            @Override
            public void onCollapsedChanged(boolean isCollapsed) {
                collapsed = isCollapsed;
            }

            @Override
            public void onPositionChanged(float x, float y) {
                overlayX = x;
                overlayY = y;
            }
        });
        decorView.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        overlayView = view;
    }

    private synchronized void detachOverlay() {
        if (overlayView == null) {
            return;
        }
        overlayX = overlayView.getX();
        overlayY = overlayView.getY();
        overlayView.release();
        ViewParent parent = overlayView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(overlayView);
        }
        overlayView = null;
    }
}
