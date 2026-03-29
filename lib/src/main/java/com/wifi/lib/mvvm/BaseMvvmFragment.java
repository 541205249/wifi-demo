package com.wifi.lib.mvvm;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewbinding.ViewBinding;

import com.wifi.lib.baseui.BaseVBFragment;
import com.wifi.lib.utils.Toasty;

public abstract class BaseMvvmFragment<VB extends ViewBinding, VM extends BaseViewModel> extends BaseVBFragment<VB> {
    protected VM viewModel;

    @NonNull
    protected abstract Class<VM> getViewModelClass();

    protected boolean useActivityViewModel() {
        return false;
    }

    @Override
    protected void onBindingCreated(@Nullable Bundle savedInstanceState) {
        viewModel = createViewModel();
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

    @NonNull
    private VM createViewModel() {
        Application application = requireActivity().getApplication();
        return new ViewModelProvider(
                resolveViewModelStoreOwner(),
                createViewModelFactory(application)
        ).get(getViewModelClass());
    }

    @NonNull
    private ViewModelProvider.Factory createViewModelFactory(@NonNull Application application) {
        return ViewModelProvider.AndroidViewModelFactory.getInstance(application);
    }

    @NonNull
    private ViewModelStoreOwner resolveViewModelStoreOwner() {
        return useActivityViewModel() ? requireActivity() : this;
    }

    private void observeBaseState() {
        observeLoadingState();
        observeMessageState();
    }

    private void observeLoadingState() {
        if (!enableDefaultLoadingObserver()) {
            return;
        }
        viewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                getPageLoadingUI().show();
            } else {
                getPageLoadingUI().hide();
            }
        });
    }

    private void observeMessageState() {
        if (!enableDefaultMessageObserver()) {
            return;
        }
        viewModel.getMessageEvent().observe(getViewLifecycleOwner(), new EventObserver<>(
                Toasty::showShort
        ));
    }
}
