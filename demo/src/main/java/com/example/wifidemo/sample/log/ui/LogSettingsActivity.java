package com.example.wifidemo.sample.log.ui;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.databinding.ActivityLogSettingsBinding;
import com.example.wifidemo.sample.log.model.DLogSettingsForm;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLogExporter;
import com.wifi.lib.mvvm.BaseMvvmActivity;

public class LogSettingsActivity extends BaseMvvmActivity<ActivityLogSettingsBinding, LogSettingsViewModel> {
    @NonNull
    @Override
    protected Class<LogSettingsViewModel> getViewModelClass() {
        return LogSettingsViewModel.class;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initTitle("日志设置");
        initExportHooks();

        binding.btnApplySettings.setOnClickListener(v -> viewModel.applySettings(collectForm()));
        binding.btnResetDefaultSettings.setOnClickListener(v -> viewModel.restoreDefaultsAndApply());
    }

    @Override
    protected void observeUi() {
        super.observeUi();
        viewModel.getFormLiveData().observe(this, this::renderForm);
        viewModel.getAppliedSummaryLiveData().observe(this, summary -> binding.tvAppliedSummary.setText(summary));
    }

    private void initExportHooks() {
        ComponentActivity activity = this;
        DLog.hookToExport(activity, binding.btnExportLocalDLog, new DLog.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                viewModel.onExportResult(true, "DLog 本地导出成功：" + message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                viewModel.onExportResult(false, "DLog 本地导出失败：" + errorMessage);
            }
        });
        DLog.hookToShare(activity, binding.btnSharePlatformDLog, new DLog.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                viewModel.onExportResult(true, "DLog 平台分享已触发：" + message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                viewModel.onExportResult(false, "DLog 平台分享失败：" + errorMessage);
            }
        });
        JLogExporter.get().hookToExport(activity, binding.btnExportLocalJLog, new JLogExporter.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                viewModel.onExportResult(true, "JLog 全量本地导出成功：" + message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                viewModel.onExportResult(false, "JLog 全量本地导出失败：" + errorMessage);
            }
        });
        JLogExporter.get().hookToShare(activity, binding.btnSharePlatformJLog, new JLogExporter.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                viewModel.onExportResult(true, "JLog 全量平台分享已触发：" + message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                viewModel.onExportResult(false, "JLog 全量平台分享失败：" + errorMessage);
            }
        });
    }

    private void renderForm(@NonNull DLogSettingsForm form) {
        binding.switchSaveLog.setChecked(form.isSaveLogEnable());
        binding.switchCrashMonitor.setChecked(form.isMonitorCrashLog());
        binding.switchOverlayVisible.setChecked(form.isOverlayVisible());
        setTextIfDifferent(binding.etLogTag, form.getLogTag());
        setTextIfDifferent(binding.etLogDirectory, form.getLogDirectoryPath());
        setTextIfDifferent(binding.etMaxStorageDays, form.getMaxLogStorageDays());
        setTextIfDifferent(binding.etUnzipCode, form.getUnzipCode());
        setTextIfDifferent(binding.etSingleFileSizeLimit, form.getSingleFileSizeLimit());
        setTextIfDifferent(binding.etDailyFilesLimit, form.getDailyFilesLimit());
        setTextIfDifferent(binding.etMemoryBufferLimit, form.getMemoryBufferLimit());
    }

    @NonNull
    private DLogSettingsForm collectForm() {
        return new DLogSettingsForm(
                binding.switchSaveLog.isChecked(),
                binding.switchCrashMonitor.isChecked(),
                binding.switchOverlayVisible.isChecked(),
                textOf(binding.etLogTag),
                textOf(binding.etLogDirectory),
                textOf(binding.etMaxStorageDays),
                textOf(binding.etUnzipCode),
                textOf(binding.etSingleFileSizeLimit),
                textOf(binding.etDailyFilesLimit),
                textOf(binding.etMemoryBufferLimit)
        );
    }

    @NonNull
    private String textOf(@NonNull com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void setTextIfDifferent(
            @NonNull com.google.android.material.textfield.TextInputEditText editText,
            @NonNull String targetValue
    ) {
        String currentValue = editText.getText() == null ? "" : editText.getText().toString();
        if (!TextUtils.equals(currentValue, targetValue)) {
            editText.setText(targetValue);
            if (editText.getText() != null) {
                editText.setSelection(editText.getText().length());
            }
        }
    }
}
