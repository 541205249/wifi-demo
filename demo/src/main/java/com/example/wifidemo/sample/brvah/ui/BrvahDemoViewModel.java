package com.example.wifidemo.sample.brvah.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wifidemo.clinic.model.ExamProgram;
import com.example.wifidemo.clinic.model.KnownDeviceSummary;
import com.example.wifidemo.clinic.model.ReportRecord;
import com.example.wifidemo.clinic.model.VisionChart;
import com.example.wifidemo.sample.brvah.data.BrvahDemoRepository;
import com.example.wifidemo.sample.brvah.model.BrvahReportUiState;
import com.example.wifidemo.sample.brvah.model.BrvahWorkflowItem;
import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;
import com.wifi.lib.mvvm.BaseViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrvahDemoViewModel extends BaseViewModel {
    private static final String TAG = "BrvahDemoViewModel";
    private final BrvahDemoRepository repository = new BrvahDemoRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final MutableLiveData<List<KnownDeviceSummary>> devicesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<VisionChart>> chartsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<BrvahWorkflowItem>> workflowLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ExamProgram>> programsLiveData = new MutableLiveData<>();
    private final MutableLiveData<BrvahReportUiState> reportUiStateLiveData = new MutableLiveData<>();

    private final List<ReportRecord> allReports;
    private final List<ReportRecord> loadedReports = new ArrayList<>();

    private int reportPageIndex;
    private boolean reportErrorInjected;
    private boolean reportLoading;

    public BrvahDemoViewModel(@NonNull Application application) {
        super(application);
        allReports = repository.createAllReports();
        DLog.i(TAG, "BRVAH 示例 ViewModel 初始化完成，reports=" + allReports.size());
        resetDevices();
        resetCharts();
        resetWorkflow();
        resetPrograms();
        resetReportPaging();
    }

    public LiveData<List<KnownDeviceSummary>> getDevicesLiveData() {
        return devicesLiveData;
    }

    public LiveData<List<VisionChart>> getChartsLiveData() {
        return chartsLiveData;
    }

    public LiveData<List<BrvahWorkflowItem>> getWorkflowLiveData() {
        return workflowLiveData;
    }

    public LiveData<List<ExamProgram>> getProgramsLiveData() {
        return programsLiveData;
    }

    public LiveData<BrvahReportUiState> getReportUiStateLiveData() {
        return reportUiStateLiveData;
    }

    public String formatSeenTime(long timestamp) {
        return repository.formatSeenTime(timestamp);
    }

    public String formatReportTime(long timestamp) {
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    public void resetDevices() {
        JLog.i("BrvahDemoViewModel", "resetDevices");
        DLog.i(TAG, "重置模块示例列表");
        devicesLiveData.setValue(repository.createKnownDevices());
    }

    public void clearDevices() {
        JLog.i("BrvahDemoViewModel", "clearDevices");
        DLog.i(TAG, "清空模块示例列表");
        devicesLiveData.setValue(new ArrayList<>());
        dispatchMessage("已切到空列表，方便查看 EmptyView 效果");
    }

    public void resetCharts() {
        JLog.i("BrvahDemoViewModel", "resetCharts");
        DLog.i(TAG, "重置视标示例列表");
        chartsLiveData.setValue(repository.createVisionCharts());
    }

    public void rotateCharts() {
        List<VisionChart> current = new ArrayList<>(valueOrEmpty(chartsLiveData.getValue()));
        if (current.size() > 1) {
            Collections.rotate(current, 1);
            chartsLiveData.setValue(current);
            DLog.i(TAG, "轮换视标宫格顺序");
            dispatchMessage("已轮换宫格顺序");
        }
    }

    public void resetWorkflow() {
        JLog.i("BrvahDemoViewModel", "resetWorkflow");
        DLog.i(TAG, "重置工作流示例列表");
        workflowLiveData.setValue(repository.createWorkflowItems());
    }

    public void appendWorkflowAlert() {
        List<BrvahWorkflowItem> current = new ArrayList<>(valueOrEmpty(workflowLiveData.getValue()));
        current.add(new BrvahWorkflowItem(
                BrvahWorkflowItem.TYPE_ACTION,
                "提醒：" + timeFormat.format(new Date()),
                "被测者反馈近用发酸",
                "这一条是运行期动态插入的 Action 卡片，用来演示多布局列表增量刷新。",
                "新插入"
        ));
        workflowLiveData.setValue(current);
        DLog.i(TAG, "追加工作流提醒卡片");
        dispatchMessage("已追加一条多布局提醒");
    }

    public void resetPrograms() {
        JLog.i("BrvahDemoViewModel", "resetPrograms");
        DLog.i(TAG, "重置流程拖拽示例列表");
        programsLiveData.setValue(repository.createExamPrograms());
    }

    public void insertFollowUpProgram() {
        List<ExamProgram> current = new ArrayList<>(valueOrEmpty(programsLiveData.getValue()));
        ExamProgram followUp = new ExamProgram(
                "program-night-" + System.currentTimeMillis(),
                "夜间驾驶复查",
                "针对眩光主诉的补充流程",
                "适合作为拖拽列表中的新增项示例。"
        );
        current.add(0, followUp);
        programsLiveData.setValue(current);
        DLog.i(TAG, "插入新的流程卡片，programId=" + followUp.getId());
        dispatchMessage("已插入一个新的流程卡片");
    }

    public void persistPrograms(@NonNull List<ExamProgram> programs) {
        programsLiveData.setValue(new ArrayList<>(programs));
        DLog.d(TAG, "持久化拖拽后的流程列表，count=" + programs.size());
    }

    public void resetReportPaging() {
        JLog.i("BrvahDemoViewModel", "resetReportPaging");
        DLog.i(TAG, "重置报告分页状态");
        handler.removeCallbacksAndMessages(null);
        loadedReports.clear();
        reportPageIndex = 0;
        reportErrorInjected = false;
        reportLoading = false;

        List<ReportRecord> firstPage = repository.getReportPage(allReports, reportPageIndex);
        loadedReports.addAll(firstPage);
        reportPageIndex++;
        updateReportState(BrvahReportUiState.STATUS_IDLE, null);
    }

    public void loadNextReportPage() {
        if (reportLoading) {
            DLog.w(TAG, "忽略重复分页请求，当前仍在加载");
            return;
        }
        boolean hasMore = loadedReports.size() < allReports.size();
        if (!hasMore) {
            DLog.i(TAG, "报告分页已到底");
            updateReportState(BrvahReportUiState.STATUS_COMPLETE, null);
            return;
        }
        reportLoading = true;
        DLog.i(TAG, "开始加载下一页报告，pageIndex=" + reportPageIndex);
        updateReportState(BrvahReportUiState.STATUS_LOADING, null);
        handler.postDelayed(() -> {
            if (reportPageIndex == 1 && !reportErrorInjected) {
                reportErrorInjected = true;
                reportLoading = false;
                DLog.w(TAG, "按计划注入一次分页错误，pageIndex=" + reportPageIndex);
                updateReportState(BrvahReportUiState.STATUS_ERROR, "第 2 页模拟网络抖动，点击底部重试即可继续");
                dispatchMessage("已模拟一次分页失败，方便查看 BRVAH 的错误重试态");
                return;
            }
            List<ReportRecord> nextPage = repository.getReportPage(allReports, reportPageIndex);
            loadedReports.addAll(nextPage);
            reportPageIndex++;
            reportLoading = false;
            DLog.i(TAG, "报告分页加载完成，loaded=" + loadedReports.size() + ", nextPageIndex=" + reportPageIndex);
            if (loadedReports.size() >= allReports.size()) {
                updateReportState(BrvahReportUiState.STATUS_COMPLETE, null);
            } else {
                updateReportState(BrvahReportUiState.STATUS_IDLE, null);
            }
        }, 900);
    }

    public void retryLoadMoreReports() {
        JLog.i("BrvahDemoViewModel", "retryLoadMoreReports");
        DLog.i(TAG, "重试加载下一页报告");
        loadNextReportPage();
    }

    public void notifyAction(@NonNull String message) {
        JLog.i("BrvahDemoViewModel", message);
        DLog.i(TAG, "转发 BRVAH 示例动作消息，message=" + message);
        dispatchMessage(message);
    }

    private void updateReportState(int status, String errorMessage) {
        boolean hasMore = loadedReports.size() < allReports.size();
        String summary = "已加载 " + loadedReports.size() + " 条报告，分页大小 " + repository.getReportPageSize() + "。";
        String footerTip;
        if (status == BrvahReportUiState.STATUS_LOADING) {
            footerTip = "正在追加下一页数据，可继续下滑观察尾部状态切换。";
        } else if (status == BrvahReportUiState.STATUS_ERROR) {
            footerTip = errorMessage;
        } else if (!hasMore) {
            footerTip = "全部示例报告已加载完成，底部会进入完成态。";
        } else {
            footerTip = "滚动到底部会自动加载下一页；第 2 页会先故意失败一次。";
        }
        reportUiStateLiveData.setValue(new BrvahReportUiState(
                loadedReports,
                summary,
                footerTip,
                status,
                hasMore,
                errorMessage
        ));
        DLog.d(TAG, "刷新报告分页 UI 状态，status=" + status + ", hasMore=" + hasMore + ", loaded=" + loadedReports.size());
    }

    private <T> List<T> valueOrEmpty(List<T> current) {
        return current == null ? new ArrayList<>() : current;
    }
}

