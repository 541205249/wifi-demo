package com.wifi.lib.baseui;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.wifi.lib.R;
import com.wifi.lib.baseui.internal.ViewBindingReflector;

public abstract class BaseVBBottomSheetDialog<VB extends ViewBinding> extends BottomSheetDialog {
    protected VB binding;
    protected boolean fullHeight = true;

    protected BaseVBBottomSheetDialog(@NonNull Context context) {
        super(context, R.style.WifiLibBaseDialog);
        init();
    }

    protected BaseVBBottomSheetDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    protected abstract void initWidgets();

    protected void bindListeners() {
    }

    public void setFullHeight(boolean fullHeight) {
        this.fullHeight = fullHeight;
    }

    @Override
    public void show() {
        if (fullHeight) {
            BottomSheetBehavior<FrameLayout> behavior = getBehavior();
            behavior.setPeekHeight(getWindowHeight());
        }
        super.show();
    }

    public void setShowBottom() {
        setShowPosition(Gravity.BOTTOM);
    }

    public void setShowBottomWithAnim() {
        setShowBottom();
        Window window = getWindow();
        if (window != null) {
            window.setWindowAnimations(R.style.WifiLibBottomDialogAnimation);
        }
    }

    public void setShowPosition(int gravity) {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        window.setGravity(gravity);
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(layoutParams);
    }

    private void init() {
        binding = ViewBindingReflector.inflate(this, getLayoutInflater());
        setContentView(binding.getRoot());
        initWidgets();
        bindListeners();
        setShowBottomWithAnim();
    }

    private int getWindowHeight() {
        Resources resources = getContext().getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        return displayMetrics.heightPixels;
    }
}
