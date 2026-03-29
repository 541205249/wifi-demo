package com.example.wifidemo.sample.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.ActivityDemoMvvmBinding;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.mvvm.BaseMvvmActivity;

public class DemoMvvmActivity extends BaseMvvmActivity<ActivityDemoMvvmBinding, DemoViewModel> {
    @NonNull
    @Override
    protected Class<DemoViewModel> getViewModelClass() {
        return DemoViewModel.class;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        initPageChrome();
        bindActivityActions();
        attachFeatureFragmentIfNeeded(savedInstanceState);
    }

    @Override
    protected void observeUi() {
        super.observeUi();
        viewModel.getSummaryLiveData().observe(this, this::renderSummary);
        viewModel.getPermissionStateLiveData().observe(this, this::renderPermissionState);
    }

    private void initPageChrome() {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle(getString(R.string.demo_mvvm_title));
        getPageTitleUI().initTvRight(getString(R.string.demo_common_intro), v -> showIntroDialog());
    }

    private void bindActivityActions() {
        binding.btnRefreshFromActivity.setOnClickListener(v -> viewModel.refreshRecords());
        binding.btnAddRecordFromActivity.setOnClickListener(v -> viewModel.addMockRecord());
    }

    private void attachFeatureFragmentIfNeeded(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.fragmentContainer.getId(), new DemoFeatureFragment())
                .commit();
    }

    private void renderSummary(@NonNull String summary) {
        binding.tvSummary.setText(summary);
    }

    private void renderPermissionState(@NonNull String state) {
        binding.tvPermissionState.setText(state);
    }

    private void showIntroDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(this);
        dialog.setTitleTxt(getString(R.string.demo_mvvm_intro_title));
        dialog.setContentTxt(getString(R.string.demo_mvvm_intro_content));
        dialog.setOkTxt(getString(R.string.demo_common_known));
        dialog.hideCancelTv();
        dialog.show();
    }
}
