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
        DLog.hookToExport(activity, binding.btnExportLocalDLog,
                createExportResultCallback("DLog 本地导出成功：", "DLog 本地导出失败："));
        DLog.hookToShare(activity, binding.btnSharePlatformDLog,
                createExportResultCallback("DLog 平台分享已触发：", "DLog 平台分享失败："));
        JLogExporter.get().hookToExport(activity, binding.btnExportLocalJLog,
                createExportResultCallback("JLog 全量本地导出成功：", "JLog 全量本地导出失败："));
        JLogExporter.get().hookToShare(activity, binding.btnSharePlatformJLog,
                createExportResultCallback("JLog 全量平台分享已触发：", "JLog 全量平台分享失败："));
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

    @NonNull
    private DLog.Callback createExportResultCallback(
            @NonNull String successPrefix,
            @NonNull String failurePrefix
    ) {
        return new DLog.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                viewModel.onExportResult(true, successPrefix + message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                viewModel.onExportResult(false, failurePrefix + errorMessage);
            }
        };
    }
}
