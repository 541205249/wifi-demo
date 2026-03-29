package com.example.wifidemo.sample.log.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wifidemo.sample.log.data.DLogSettingsRepository;
import com.example.wifidemo.sample.log.model.DLogSettingsForm;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseViewModel;

public class LogSettingsViewModel extends BaseViewModel {
    private static final String TAG = "LogSettingsViewModel";

    private final DLogSettingsRepository repository;
    private final MutableLiveData<DLogSettingsForm> formLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> appliedSummaryLiveData = new MutableLiveData<>();

    public LogSettingsViewModel(@NonNull Application application) {
        super(application);
        repository = DLogSettingsRepository.getInstance(application);
        loadCurrentSettings();
    }

    public LiveData<DLogSettingsForm> getFormLiveData() {
        return formLiveData;
    }

    public LiveData<String> getAppliedSummaryLiveData() {
        return appliedSummaryLiveData;
    }

    public void loadCurrentSettings() {
        DLogSettingsForm form = repository.loadEditableForm();
        publishAppliedForm(form);
        DLog.i(TAG, "加载当前 DLog 配置表单");
    }

    public void restoreDefaultsAndApply() {
        DLogSettingsForm defaultForm = repository.loadDefaultForm();
        try {
            DLogSettingsForm appliedForm = repository.applyAndPersist(defaultForm);
            publishAppliedForm(appliedForm);
            dispatchMessage("已恢复默认配置并立即应用");
            DLog.i(TAG, "已恢复默认 DLog 配置并立即应用");
        } catch (IllegalArgumentException exception) {
            formLiveData.setValue(defaultForm);
            dispatchMessage(exception.getMessage() == null ? "恢复默认配置失败" : exception.getMessage());
            DLog.w(TAG, "恢复默认 DLog 配置失败", exception);
        }
    }

    public void applySettings(@NonNull DLogSettingsForm form) {
        try {
            DLogSettingsForm appliedForm = repository.applyAndPersist(form);
            publishAppliedForm(appliedForm);
            dispatchMessage("DLog 配置已保存并应用");
            DLog.i(TAG, "DLog 配置应用成功");
        } catch (IllegalArgumentException exception) {
            dispatchMessage(exception.getMessage() == null ? "DLog 配置校验失败" : exception.getMessage());
            DLog.w(TAG, "DLog 配置校验失败", exception);
        }
    }

    public void onExportResult(boolean success, @NonNull String message) {
        dispatchMessage(message);
        if (success) {
            DLog.i(TAG, "日志导出成功: " + message);
            return;
        }
        DLog.w(TAG, "日志导出失败: " + message);
    }

    private void publishAppliedForm(@NonNull DLogSettingsForm form) {
        formLiveData.setValue(form);
        appliedSummaryLiveData.setValue(repository.buildAppliedSummary(form));
    }
}
