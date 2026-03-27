package com.wifi.lib.baseui;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.viewbinding.ViewBinding;

import com.wifi.lib.baseui.delegate.PageLoadingUIDelegate;
import com.wifi.lib.baseui.delegate.PageTitleUIDelegate;
import com.wifi.lib.baseui.delegate.PermissionDelegate;
import com.wifi.lib.baseui.delegate.StatusBarDelegate;
import com.wifi.lib.baseui.internal.ViewBindingReflector;

public abstract class BaseVBActivity<VB extends ViewBinding> extends AppCompatActivity {
    protected VB binding;

    private StatusBarDelegate statusBarDelegate;
    private PageTitleUIDelegate pageTitleUIDelegate;
    private PageLoadingUIDelegate pageLoadingUIDelegate;
    private PermissionDelegate permissionDelegate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        binding = ViewBindingReflector.inflate(this, getLayoutInflater());
        setContentView(binding.getRoot());
        onBindingCreated(savedInstanceState);
    }

    protected void onBindingCreated(@Nullable Bundle savedInstanceState) {
        initWidgets(savedInstanceState);
        bindListeners();
        observeUi();
        loadData();
    }

    protected void initWidgets(@Nullable Bundle savedInstanceState) {
    }

    protected void bindListeners() {
    }

    protected void observeUi() {
    }

    protected void loadData() {
    }

    @NonNull
    protected StatusBarDelegate getStatusBarUI() {
        if (statusBarDelegate == null) {
            statusBarDelegate = new StatusBarDelegate(getWindow());
        }
        return statusBarDelegate;
    }

    @NonNull
    protected PageTitleUIDelegate getPageTitleUI() {
        if (pageTitleUIDelegate == null) {
            pageTitleUIDelegate = new PageTitleUIDelegate(
                    binding.getRoot(),
                    resolveStatusBarHeight(),
                    this::onBackPressedDispatcher
            );
        }
        return pageTitleUIDelegate;
    }

    @NonNull
    protected PageLoadingUIDelegate getPageLoadingUI() {
        if (pageLoadingUIDelegate == null) {
            pageLoadingUIDelegate = new PageLoadingUIDelegate(binding.getRoot().getRootView());
        }
        return pageLoadingUIDelegate;
    }

    @NonNull
    protected PermissionDelegate getPermissionDelegate() {
        if (permissionDelegate == null) {
            permissionDelegate = new PermissionDelegate(this);
        }
        return permissionDelegate;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionDelegate != null) {
            permissionDelegate.onResult(requestCode, permissions, grantResults);
        }
    }

    private void onBackPressedDispatcher() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private int resolveStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
