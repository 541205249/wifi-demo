package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chad.library.adapter4.QuickAdapterHelper;
import com.chad.library.adapter4.loadState.LoadState;
import com.chad.library.adapter4.loadState.trailing.TrailingLoadStateAdapter;
import com.example.wifidemo.clinic.model.ReportRecord;
import com.example.wifidemo.sample.brvah.model.BrvahReportUiState;
import com.example.wifidemo.sample.brvah.ui.adapter.ReportRecordAdapter;
import com.example.wifidemo.sample.brvah.ui.adapter.SingleInfoAdapter;

import java.util.ArrayList;

public class BrvahLoadMoreFragment extends BaseBrvahScenarioFragment {
    private ReportRecordAdapter adapter;
    private SingleInfoAdapter headerAdapter;
    private SingleInfoAdapter footerAdapter;
    private QuickAdapterHelper quickAdapterHelper;

    @Override
    protected void onSetupScenario(@Nullable Bundle savedInstanceState) {
        configureScenario(
                "分页加载",
                "演示 QuickAdapterHelper、Header/Footer 与尾部自动加载状态，适合报告列表、历史记录、日志分页等页面。",
                "手动加载",
                viewModel::loadNextReportPage,
                "重置分页",
                viewModel::resetReportPaging
        );

        adapter = new ReportRecordAdapter();
        adapter.setEmptyView(createEmptyView("暂无报告", "这里可以直接承接报告中心、日志中心等分页空态。"));
        adapter.setEmptyViewEnable(true);
        adapter.setOnReportActionClickListener(item -> viewModel.notifyAction("分享报告二维码：" + item.getId()));
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> {
            ReportRecord item = adapter.getItem(position);
            if (item != null) {
                viewModel.notifyAction("打开报告：" + item.getPatientName() + " / " + item.getProgramName());
            }
        });

        headerAdapter = new SingleInfoAdapter("Header");
        footerAdapter = new SingleInfoAdapter("Footer");

        getScenarioRecyclerView().setLayoutManager(new LinearLayoutManager(requireContext()));
        quickAdapterHelper = new QuickAdapterHelper.Builder(adapter)
                .setTrailingLoadStateAdapter(new TrailingLoadStateAdapter.OnTrailingListener() {
                    @Override
                    public void onLoad() {
                        viewModel.loadNextReportPage();
                    }

                    @Override
                    public void onFailRetry() {
                        viewModel.retryLoadMoreReports();
                    }
                })
                .isTrailAutoLoadMore(true)
                .setTrailPreloadSize(1)
                .attachTo(getScenarioRecyclerView());
        quickAdapterHelper.addBeforeAdapter(headerAdapter);
        quickAdapterHelper.addAfterAdapter(footerAdapter);
    }

    @Override
    protected void observeUi() {
        viewModel.getReportUiStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            adapter.submitList(new ArrayList<>(state.getRecords()));
            headerAdapter.setMessage("Header: " + state.getSummary());
            footerAdapter.setMessage("Footer: " + state.getFooterTip());
            setScenarioState(state.getSummary());
            quickAdapterHelper.setTrailingLoadState(mapLoadState(state));
        });
    }

    private LoadState mapLoadState(BrvahReportUiState state) {
        switch (state.getStatus()) {
            case BrvahReportUiState.STATUS_LOADING:
                return LoadState.Loading.INSTANCE;
            case BrvahReportUiState.STATUS_ERROR:
                return new LoadState.Error(new IllegalStateException(state.getErrorMessage()));
            case BrvahReportUiState.STATUS_COMPLETE:
                return LoadState.NotLoading.getComplete();
            case BrvahReportUiState.STATUS_IDLE:
            default:
                return state.hasMore() ? LoadState.NotLoading.getIncomplete() : LoadState.NotLoading.getComplete();
        }
    }
}
