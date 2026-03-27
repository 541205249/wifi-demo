package com.example.wifidemo.sample.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wifidemo.sample.data.DemoRepository;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;
import com.wifi.lib.mvvm.BaseViewModel;

import java.util.List;

public class DemoViewModel extends BaseViewModel {
    private static final String TAG = "DemoViewModel";
    private final DemoRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<List<String>> recordsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> summaryLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> permissionStateLiveData = new MutableLiveData<>("通知权限：未请求");

    public DemoViewModel(@NonNull Application application) {
        super(application);
        repository = DemoRepository.getInstance();
        DLog.i(TAG, "DemoViewModel 初始化完成");
        syncState("基础框架已准备完成");
    }

    public LiveData<List<String>> getRecordsLiveData() {
        return recordsLiveData;
    }

    public LiveData<String> getSummaryLiveData() {
        return summaryLiveData;
    }

    public LiveData<String> getPermissionStateLiveData() {
        return permissionStateLiveData;
    }

    public void refreshRecords() {
        JLog.i("DemoViewModel", "refreshRecords called");
        DLog.i(TAG, "请求刷新演示记录");
        showLoading();
        handler.postDelayed(() -> {
            syncState("已从 Repository 刷新数据");
            hideLoading();
            dispatchMessage("示例数据刷新完成");
        }, 650);
    }

    public void addMockRecord() {
        JLog.i("DemoViewModel", "addMockRecord called");
        DLog.i(TAG, "请求新增模拟记录");
        repository.appendRecord("模拟发送一条模块通信记录 #" + (repository.size() + 1));
        syncState("已新增演示记录");
        dispatchMessage("已新增一条示例记录");
    }

    public void appendNote(String note) {
        if (TextUtils.isEmpty(note)) {
            JLog.w("DemoViewModel", "appendNote ignored because note is empty");
            DLog.w(TAG, "忽略空备注写入");
            dispatchMessage("请输入备注内容");
            return;
        }
        JLog.i("DemoViewModel", "appendNote: " + note.trim());
        DLog.i(TAG, "写入自定义备注，note=" + note.trim());
        repository.appendRecord("自定义备注：" + note.trim());
        syncState("已写入自定义备注");
    }

    public void updatePermissionState(boolean granted) {
        JLog.i("DemoViewModel", granted ? "notification permission granted" : "notification permission denied");
        DLog.i(TAG, "更新通知权限状态，granted=" + granted);
        permissionStateLiveData.setValue(granted ? "通知权限：已授权" : "通知权限：已拒绝");
        repository.appendRecord(granted ? "权限授权成功：POST_NOTIFICATIONS" : "权限申请被拒绝：POST_NOTIFICATIONS");
        syncState(granted ? "权限已授权" : "权限被拒绝");
    }

    public void appendSystemRecord(String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        JLog.i("DemoViewModel", message);
        DLog.i(TAG, "写入系统动作记录，message=" + message);
        repository.appendRecord(message);
        syncState("日志导出动作已记录");
    }

    private void syncState(String prefix) {
        List<String> records = repository.snapshot();
        recordsLiveData.setValue(records);
        summaryLiveData.setValue(prefix + "，当前共 " + records.size() + " 条演示记录");
        DLog.d(TAG, "同步 Demo UI 状态完成，records=" + records.size() + ", summaryPrefix=" + prefix);
    }
}

