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
import com.wifi.lib.log.JLog;
import com.wifi.lib.mvvm.BaseViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrvahDemoViewModel extends BaseViewModel {
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
        devicesLiveData.setValue(repository.createKnownDevices());
    }

    public void clearDevices() {
        JLog.i("BrvahDemoViewModel", "clearDevices");
        devicesLiveData.setValue(new ArrayList<>());
        dispatchMessage("已切到空列表，方便查看 EmptyView 效果");
    }

    public void resetCharts() {
        JLog.i("BrvahDemoViewModel", "resetCharts");
        chartsLiveData.setValue(repository.createVisionCharts());
    }

    public void rotateCharts() {
        List<VisionChart> current = new ArrayList<>(valueOrEmpty(chartsLiveData.getValue()));
        if (current.size() > 1) {
            Collections.rotate(current, 1);
            chartsLiveData.setValue(current);
            dispatchMessage("已轮换宫格顺序");
        }
    }

    public void resetWorkflow() {
        JLog.i("BrvahDemoViewModel", "resetWorkflow");
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
        dispatchMessage("已追加一条多布局提醒");
    }

    public void resetPrograms() {
        JLog.i("BrvahDemoViewModel", "resetPrograms");
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
        dispatchMessage("已插入一个新的流程卡片");
    }

    public void persistPrograms(@NonNull List<ExamProgram> programs) {
        programsLiveData.setValue(new ArrayList<>(programs));
    }

    public void resetReportPaging() {
        JLog.i("BrvahDemoViewModel", "resetReportPaging");
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
            return;
        }
        boolean hasMore = loadedReports.size() < allReports.size();
        if (!hasMore) {
            updateReportState(BrvahReportUiState.STATUS_COMPLETE, null);
            return;
        }
        reportLoading = true;
        updateReportState(BrvahReportUiState.STATUS_LOADING, null);
        handler.postDelayed(() -> {
            if (reportPageIndex == 1 && !reportErrorInjected) {
                reportErrorInjected = true;
                reportLoading = false;
                updateReportState(BrvahReportUiState.STATUS_ERROR, "第 2 页模拟网络抖动，点击底部重试即可继续");
                dispatchMessage("已模拟一次分页失败，方便查看 BRVAH 的错误重试态");
                return;
            }
            List<ReportRecord> nextPage = repository.getReportPage(allReports, reportPageIndex);
            loadedReports.addAll(nextPage);
            reportPageIndex++;
            reportLoading = false;
            if (loadedReports.size() >= allReports.size()) {
                updateReportState(BrvahReportUiState.STATUS_COMPLETE, null);
            } else {
                updateReportState(BrvahReportUiState.STATUS_IDLE, null);
            }
        }, 900);
    }

    public void retryLoadMoreReports() {
        JLog.i("BrvahDemoViewModel", "retryLoadMoreReports");
        loadNextReportPage();
    }

    public void notifyAction(@NonNull String message) {
        JLog.i("BrvahDemoViewModel", message);
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
    }

    private <T> List<T> valueOrEmpty(List<T> current) {
        return current == null ? new ArrayList<>() : current;
    }
}
