package com.example.wifidemo.sample.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.ActivityDemoHomeBinding;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.mvvm.BaseMvvmActivity;

public class DemoHomeActivity extends BaseMvvmActivity<ActivityDemoHomeBinding, DemoViewModel> {
    @NonNull
    @Override
    protected Class<DemoViewModel> getViewModelClass() {
        return DemoViewModel.class;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle("BaseUI + MVVM");
        getPageTitleUI().initTvRight("说明", v -> showIntroDialog());

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
        dialog.setTitleTxt("示例说明");
        dialog.setContentTxt("这个页面演示了 lib 中的 BaseUI、MVVM、Repository、权限代理、确认弹框和 BottomSheet 的基础用法。");
        dialog.setOkTxt("知道了");
        dialog.hideCancelTv();
        dialog.show();
    }
}
