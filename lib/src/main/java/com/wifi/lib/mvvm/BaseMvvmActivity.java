package com.wifi.lib.mvvm;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.wifi.lib.baseui.BaseVBActivity;
import com.wifi.lib.utils.Toasty;

public abstract class BaseMvvmActivity<VB extends ViewBinding, VM extends BaseViewModel> extends BaseVBActivity<VB> {
    protected VM viewModel;

    @NonNull
    protected abstract Class<VM> getViewModelClass();

    @Override
    protected void onBindingCreated(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())
        ).get(getViewModelClass());
        observeBaseState();
        onViewModelCreated(viewModel);
        super.onBindingCreated(savedInstanceState);
    }

    protected void onViewModelCreated(@NonNull VM viewModel) {
    }

    protected boolean enableDefaultLoadingObserver() {
        return true;
    }

    protected boolean enableDefaultMessageObserver() {
        return true;
    }

    private void observeBaseState() {
        if (enableDefaultLoadingObserver()) {
            viewModel.getLoadingLiveData().observe(this, loading -> {
                if (Boolean.TRUE.equals(loading)) {
                    getPageLoadingUI().show();
                } else {
                    getPageLoadingUI().hide();
                }
            });
        }
        if (enableDefaultMessageObserver()) {
            viewModel.getMessageEvent().observe(this, new EventObserver<>(
                    Toasty::showShort
            ));
        }
    }
}
