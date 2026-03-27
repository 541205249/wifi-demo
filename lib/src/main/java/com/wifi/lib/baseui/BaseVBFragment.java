package com.wifi.lib.baseui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.wifi.lib.baseui.delegate.PageLoadingUIDelegate;
import com.wifi.lib.baseui.delegate.PermissionDelegate;
import com.wifi.lib.baseui.internal.ViewBindingReflector;

public abstract class BaseVBFragment<VB extends ViewBinding> extends Fragment {
    protected VB binding;

    private PageLoadingUIDelegate pageLoadingUIDelegate;
    private PermissionDelegate permissionDelegate;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = ViewBindingReflector.inflate(this, inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
    protected PageLoadingUIDelegate getPageLoadingUI() {
        if (pageLoadingUIDelegate == null) {
            pageLoadingUIDelegate = new PageLoadingUIDelegate(binding.getRoot().getRootView());
        }
        return pageLoadingUIDelegate;
    }

    @NonNull
    protected PermissionDelegate getPermissionDelegate() {
        if (permissionDelegate == null) {
            permissionDelegate = new PermissionDelegate(requireContext());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pageLoadingUIDelegate = null;
        binding = null;
    }
}
