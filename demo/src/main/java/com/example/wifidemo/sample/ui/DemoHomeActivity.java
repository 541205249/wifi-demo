package com.example.wifidemo.sample.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.databinding.ActivityDemoHomeBinding;
import com.example.wifidemo.sample.brvah.ui.BrvahDemoActivity;
import com.example.wifidemo.sample.command.CommandSettingsActivity;
import com.example.wifidemo.sample.log.ui.LogSettingsActivity;
import com.example.wifidemo.sample.network.ui.NetworkDemoActivity;
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
        binding.btnOpenBrvahDemo.setOnClickListener(v ->
                startActivity(new Intent(this, BrvahDemoActivity.class))
        );
        binding.btnOpenNetworkDemo.setOnClickListener(v ->
                startActivity(new Intent(this, NetworkDemoActivity.class))
        );
        binding.btnOpenCommandSettings.setOnClickListener(v ->
                startActivity(new Intent(this, CommandSettingsActivity.class))
        );
        binding.btnOpenLogSettings.setOnClickListener(v ->
                startActivity(new Intent(this, LogSettingsActivity.class))
        );

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
        dialog.setContentTxt("这个页面演示了 lib 中的 BaseUI、MVVM、Repository、权限代理、确认弹框、网络框架、命令框架和 BottomSheet 的基础用法，同时提供了 BRVAH 场景集入口，方便你直接参考具体写法。");
        dialog.setOkTxt("知道了");
        dialog.hideCancelTv();
        dialog.show();
    }
}
