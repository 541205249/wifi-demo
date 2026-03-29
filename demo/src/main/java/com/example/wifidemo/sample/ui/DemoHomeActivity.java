package com.example.wifidemo.sample.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.ActivityDemoHomeBinding;
import com.example.wifidemo.sample.brvah.ui.BrvahDemoActivity;
import com.example.wifidemo.sample.communication.ui.CommunicationDemoActivity;
import com.example.wifidemo.sample.command.CommandSettingsActivity;
import com.example.wifidemo.sample.log.ui.LogSettingsActivity;
import com.example.wifidemo.sample.network.ui.NetworkDemoActivity;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.baseui.BaseVBActivity;

public class DemoHomeActivity extends BaseVBActivity<ActivityDemoHomeBinding> {
    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        initPageChrome();
        bindDemoEntries();
    }

    private void initPageChrome() {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle(getString(R.string.demo_home_title));
        getPageTitleUI().initTvRight(getString(R.string.demo_common_intro), v -> showIntroDialog());
    }

    private void bindDemoEntries() {
        binding.btnOpenMvvmDemo.setOnClickListener(v ->
                openDemoPage(DemoMvvmActivity.class)
        );
        binding.btnOpenBrvahDemo.setOnClickListener(v ->
                openDemoPage(BrvahDemoActivity.class)
        );
        binding.btnOpenNetworkDemo.setOnClickListener(v ->
                openDemoPage(NetworkDemoActivity.class)
        );
        binding.btnOpenCommandSettings.setOnClickListener(v ->
                openDemoPage(CommandSettingsActivity.class)
        );
        binding.btnOpenCommunicationDemo.setOnClickListener(v ->
                openDemoPage(CommunicationDemoActivity.class)
        );
        binding.btnOpenLogSettings.setOnClickListener(v ->
                openDemoPage(LogSettingsActivity.class)
        );
    }

    private void openDemoPage(@NonNull Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void showIntroDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(this);
        dialog.setTitleTxt(getString(R.string.demo_home_intro_title));
        dialog.setContentTxt(getString(R.string.demo_home_intro_content));
        dialog.setOkTxt(getString(R.string.demo_common_known));
        dialog.hideCancelTv();
        dialog.show();
    }
}
