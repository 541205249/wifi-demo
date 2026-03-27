package com.example.wifidemo.sample.network.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.ActivityNetworkDemoBinding;
import com.example.wifidemo.sample.network.model.NetworkDemoUiState;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.mvvm.BaseMvvmActivity;

public class NetworkDemoActivity extends BaseMvvmActivity<ActivityNetworkDemoBinding, NetworkDemoViewModel> {
    @NonNull
    @Override
    protected Class<NetworkDemoViewModel> getViewModelClass() {
        return NetworkDemoViewModel.class;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle(getString(R.string.demo_network_title));
        getPageTitleUI().initTvRight(getString(R.string.demo_common_intro), v -> showIntroDialog());

        binding.btnSendGet.setOnClickListener(v -> viewModel.runGetExample());
        binding.btnSendPostJson.setOnClickListener(v -> viewModel.runPostJsonExample());
        binding.btnSendPostForm.setOnClickListener(v -> viewModel.runPostFormExample());
        binding.btnUploadFile.setOnClickListener(v -> viewModel.runUploadExample());
    }

    @Override
    protected void observeUi() {
        super.observeUi();
        viewModel.getUiStateLiveData().observe(this, this::renderState);
    }

    private void renderState(@NonNull NetworkDemoUiState state) {
        binding.tvBaseUrl.setText(getString(R.string.demo_network_base_url_prefix, state.getBaseUrl()));
        binding.tvStatus.setText(state.getStatusText());
        binding.tvScenarioTitle.setText(state.getScenarioTitle());
        binding.tvRequestPreview.setText(state.getRequestPreview());
        binding.tvResponsePreview.setText(state.getResponsePreview());
        binding.tvStatus.setTextColor(ContextCompat.getColor(
                this,
                state.isLastSuccess() ? R.color.brand_success : R.color.brand_error
        ));
    }

    private void showIntroDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(this);
        dialog.setTitleTxt(getString(R.string.demo_network_intro_title));
        dialog.setContentTxt(getString(R.string.demo_network_intro_content));
        dialog.setOkTxt(getString(R.string.demo_common_known));
        dialog.hideCancelTv();
        dialog.show();
    }
}
