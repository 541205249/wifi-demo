package com.wifi.lib.baseui.delegate;

import android.graphics.Color;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class StatusBarDelegate {
    private final Window window;

    public StatusBarDelegate(Window window) {
        this.window = window;
    }

    public void setLightMode() {
        setBgColor(Color.TRANSPARENT);
        setLightStatusBar(true);
    }

    public void setDarkMode() {
        setBgColor(Color.TRANSPARENT);
        setLightStatusBar(false);
    }

    public void setBgTransparent() {
        setBgColor(Color.TRANSPARENT);
    }

    public void setBgColorRes(@ColorRes int colorRes) {
        setBgColor(ContextCompat.getColor(window.getContext(), colorRes));
    }

    public void setBgColor(@ColorInt int color) {
        window.setStatusBarColor(color);
    }

    public void setLightStatusBar(boolean lightStatusBar) {
        new WindowInsetsControllerCompat(window, window.getDecorView())
                .setAppearanceLightStatusBars(lightStatusBar);
    }
}
