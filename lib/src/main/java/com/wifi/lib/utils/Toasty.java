package com.wifi.lib.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class Toasty {

    private Toasty() {
    }

    public static void showShort(CharSequence message) {
        showShort(AppContext.get(), message);
    }
    public static void showShort(@StringRes int resId) {
        showShort(AppContext.get(), resId);
    }

    public static void showLong(CharSequence message) {
        showLong(AppContext.get(), message);
    }
    public static void showLong(@StringRes int resId) {
        showLong(AppContext.get(), resId);
    }

    /**
     * 短时间显示Toast
     */
    public static void showShort(Context context, CharSequence message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 短时间显示Toast
     */
    public static void showShort(Context context, @StringRes int resId) {
        Toast.makeText(context.getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * 长时间显示Toast
     */
    public static void showLong(Context context, CharSequence message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
    public static void showLong(Context context, @StringRes int resId) {
        Toast.makeText(context.getApplicationContext(), resId, Toast.LENGTH_LONG).show();
    }
}
