package com.example.wifidemo.sample.network.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wifidemo.sample.network.data.NetworkDemoRepository;
import com.example.wifidemo.sample.network.model.NetworkDemoResult;
import com.example.wifidemo.sample.network.model.NetworkDemoUiState;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseViewModel;

public class NetworkDemoViewModel extends BaseViewModel {
    private static final String TAG = "NetworkDemoVM";

    private final NetworkDemoRepository repository;
    private final MutableLiveData<NetworkDemoUiState> uiStateLiveData = new MutableLiveData<>();

    private interface RepositoryAction {
        void run(@NonNull NetworkDemoRepository.Callback callback);
    }

    public NetworkDemoViewModel(@NonNull Application application) {
        super(application);
        repository = NetworkDemoRepository.getInstance(application);
        uiStateLiveData.setValue(NetworkDemoUiState.idle(repository.getBaseUrl()));
        DLog.i(TAG, "网络示例 ViewModel 初始化完成");
    }

    public LiveData<NetworkDemoUiState> getUiStateLiveData() {
        return uiStateLiveData;
    }

    public void runGetExample() {
        executeScenario("GET 查询示例", repository::requestGetExample);
    }

    public void runPostJsonExample() {
        executeScenario("POST JSON 示例", repository::requestPostJsonExample);
    }

    public void runPostFormExample() {
        executeScenario("表单提交示例", repository::requestPostFormExample);
    }

    public void runUploadExample() {
        executeScenario("文件上传示例", repository::requestUploadExample);
    }

    private void executeScenario(
            @NonNull String scenarioName,
            @NonNull RepositoryAction action
    ) {
        NetworkDemoUiState currentState = uiStateLiveData.getValue();
        if (currentState == null) {
            currentState = NetworkDemoUiState.idle(repository.getBaseUrl());
        }
        uiStateLiveData.setValue(new NetworkDemoUiState(
                repository.getBaseUrl(),
                "正在执行 " + scenarioName + " ...",
                currentState.getScenarioTitle(),
                currentState.getRequestPreview(),
                currentState.getResponsePreview(),
                currentState.isLastSuccess()
        ));
        showLoading();
        DLog.i(TAG, "开始执行网络场景：" + scenarioName);
        action.run(result -> handleResult(scenarioName, result));
    }

    private void handleResult(
            @NonNull String scenarioName,
            @NonNull NetworkDemoResult result
    ) {
        hideLoading();
        uiStateLiveData.setValue(new NetworkDemoUiState(
                repository.getBaseUrl(),
                result.getStatusText(),
                result.getScenarioTitle(),
                result.getRequestPreview(),
                result.getResponsePreview(),
                result.isSuccess()
        ));
        if (result.isSuccess()) {
            dispatchMessage(scenarioName + " 已完成");
            DLog.i(TAG, "网络场景执行成功：" + scenarioName);
            return;
        }
        dispatchMessage(result.getStatusText());
        DLog.w(TAG, "网络场景执行失败：" + scenarioName);
    }
}
