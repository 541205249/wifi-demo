package com.example.wifidemo.sample.communication.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.ActivityCommunicationDemoBinding;
import com.example.wifidemo.sample.communication.model.CommunicationDemoUiState;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.mvvm.BaseMvvmActivity;

public class CommunicationDemoActivity extends BaseMvvmActivity<ActivityCommunicationDemoBinding, CommunicationDemoViewModel> {
    @NonNull
    @Override
    protected Class<CommunicationDemoViewModel> getViewModelClass() {
        return CommunicationDemoViewModel.class;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle(getString(R.string.demo_communication_title));
        getPageTitleUI().initTvRight(getString(R.string.demo_common_intro), v -> showIntroDialog());

        binding.btnScenarioAction.setOnClickListener(v -> viewModel.showActionControl());
        binding.btnScenarioFields.setOnClickListener(v -> viewModel.showFieldSetting());
        binding.btnScenarioQuery.setOnClickListener(v -> viewModel.showQueryResponse());
        binding.btnScenarioStatus.setOnClickListener(v -> viewModel.showStatusReport());
        binding.btnScenarioResult.setOnClickListener(v -> viewModel.showResultReport());
        binding.btnScenarioTransfer.setOnClickListener(v -> viewModel.runTransferExample());
        binding.btnScenarioStream.setOnClickListener(v -> viewModel.runStreamExample());
        binding.btnScenarioAck.setOnClickListener(v -> viewModel.showAckModel());
        binding.btnScenarioDispatch.setOnClickListener(v -> viewModel.runDispatcherExample());
    }

    @Override
    protected void observeUi() {
        super.observeUi();
        viewModel.getUiStateLiveData().observe(this, this::renderState);
    }

    private void renderState(@NonNull CommunicationDemoUiState state) {
        binding.tvStatus.setText(state.getStatusText());
        binding.tvScenarioTitle.setText(state.getScenarioTitle());
        binding.tvMeaning.setText(state.getMeaningText());
        binding.tvExample.setText(state.getExampleText());
        binding.tvFlow.setText(state.getFlowText());
        binding.tvConsole.setText(state.getConsoleText());
        binding.tvStatus.setTextColor(ContextCompat.getColor(
                this,
                state.isLastSuccess() ? R.color.brand_success : R.color.brand_error
        ));
    }

    private void showIntroDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(this);
        dialog.setTitleTxt(getString(R.string.demo_communication_intro_title));
        dialog.setContentTxt(getString(R.string.demo_communication_intro_content));
        dialog.setOkTxt(getString(R.string.demo_common_known));
        dialog.hideCancelTv();
        dialog.show();
    }
}
