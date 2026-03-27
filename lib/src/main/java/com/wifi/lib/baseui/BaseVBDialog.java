package com.wifi.lib.baseui;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.viewbinding.ViewBinding;

import com.wifi.lib.R;
import com.wifi.lib.baseui.internal.ViewBindingReflector;

public abstract class BaseVBDialog<VB extends ViewBinding> extends AppCompatDialog {
    protected VB binding;

    protected BaseVBDialog(@NonNull Context context) {
        super(context, R.style.WifiLibBaseDialog);
        init();
    }

    protected BaseVBDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        renderData();
    }

    protected abstract void initWidgets();

    protected void bindListeners() {
    }

    protected void renderData() {
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
    }
}
