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
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle(getString(R.string.demo_mvvm_title));
        getPageTitleUI().initTvRight(getString(R.string.demo_common_intro), v -> showIntroDialog());

        binding.btnRefreshFromActivity.setOnClickListener(v -> viewModel.refreshRecords());
        binding.btnAddRecordFromActivity.setOnClickListener(v -> viewModel.addMockRecord());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(binding.fragmentContainer.getId(), new DemoFeatureFragment())
                    .commit();
        }
    }

    @Override
    protected void observeUi() {
        super.observeUi();
        viewModel.getSummaryLiveData().observe(this, summary -> binding.tvSummary.setText(summary));
        viewModel.getPermissionStateLiveData().observe(this, state -> binding.tvPermissionState.setText(state));
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
