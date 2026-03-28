package com.example.wifidemo.sample.ui;

import android.content.Intent;
import android.os.Bundle;

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
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle(getString(R.string.demo_home_title));
        getPageTitleUI().initTvRight(getString(R.string.demo_common_intro), v -> showIntroDialog());

        binding.btnOpenMvvmDemo.setOnClickListener(v ->
                startActivity(new Intent(this, DemoMvvmActivity.class))
        );
        binding.btnOpenBrvahDemo.setOnClickListener(v ->
                startActivity(new Intent(this, BrvahDemoActivity.class))
        );
        binding.btnOpenNetworkDemo.setOnClickListener(v ->
                startActivity(new Intent(this, NetworkDemoActivity.class))
        );
        binding.btnOpenCommandSettings.setOnClickListener(v ->
                startActivity(new Intent(this, CommandSettingsActivity.class))
        );
        binding.btnOpenCommunicationDemo.setOnClickListener(v ->
                startActivity(new Intent(this, CommunicationDemoActivity.class))
        );
        binding.btnOpenLogSettings.setOnClickListener(v ->
                startActivity(new Intent(this, LogSettingsActivity.class))
        );
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
