package com.wifi.lib.mvvm;

import android.app.Application;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.viewbinding.ViewBinding;

import com.wifi.lib.baseui.BaseVBFragment;

public abstract class BaseMvvmFragment<VB extends ViewBinding, VM extends BaseViewModel> extends BaseVBFragment<VB> {
    protected VM viewModel;

    @NonNull
    protected abstract Class<VM> getViewModelClass();

    protected boolean useActivityViewModel() {
        return false;
    }

    @Override
    protected void onBindingCreated(@Nullable Bundle savedInstanceState) {
        Application application = requireActivity().getApplication();
        ViewModelStoreOwner owner = useActivityViewModel() ? requireActivity() : this;
        viewModel = new ViewModelProvider(
                owner,
                ViewModelProvider.AndroidViewModelFactory.getInstance(application)
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
            viewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), loading -> {
                if (Boolean.TRUE.equals(loading)) {
                    getPageLoadingUI().show();
                } else {
                    getPageLoadingUI().hide();
                }
            });
        }
        if (enableDefaultMessageObserver()) {
            viewModel.getMessageEvent().observe(getViewLifecycleOwner(), new EventObserver<>(
                    message -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            ));
        }
    }
}
