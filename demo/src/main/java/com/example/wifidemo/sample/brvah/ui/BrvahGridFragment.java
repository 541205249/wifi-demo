package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.chad.library.adapter4.layoutmanager.QuickGridLayoutManager;
import com.example.wifidemo.clinic.model.VisionChart;
import com.example.wifidemo.sample.brvah.ui.adapter.VisionChartAdapter;

import java.util.ArrayList;
import java.util.List;

public class BrvahGridFragment extends BaseBrvahScenarioFragment {
    private VisionChartAdapter adapter;
    private String selectedChartId;

    @Override
    protected void onSetupScenario(@Nullable Bundle savedInstanceState) {
        configureScenario(
                "宫格卡片",
                "演示 QuickGridLayoutManager 与卡片式宫格布局，适合视标库、图片资源库、项目入口等场景。",
                "轮换顺序",
                viewModel::rotateCharts,
                "恢复默认",
                viewModel::resetCharts
        );
        adapter = new VisionChartAdapter();
        adapter.setAnimationEnable(true);
        adapter.setItemAnimation(BaseQuickAdapter.AnimationType.ScaleIn);
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> handleChartClick(position));
        getScenarioRecyclerView().setLayoutManager(new QuickGridLayoutManager(requireContext(), 2));
        getScenarioRecyclerView().setAdapter(adapter);
    }

    @Override
    protected void observeUi() {
        viewModel.getChartsLiveData().observe(getViewLifecycleOwner(), this::renderCharts);
    }

    private void renderCharts(List<VisionChart> charts) {
        List<VisionChart> safeList = copyCharts(charts);
        selectedChartId = resolveSelectedChartId(safeList, selectedChartId);
        adapter.setSelectedChartId(selectedChartId);
        adapter.submitList(safeList);
        setScenarioState(buildScenarioState(safeList, selectedChartId));
    }

    private void handleChartClick(int position) {
        VisionChart item = adapter.getItem(position);
        if (item == null) {
            return;
        }
        selectedChartId = item.getId();
        adapter.setSelectedChartId(selectedChartId);
        adapter.notifyDataSetChanged();
        viewModel.notifyAction("预览视标：" + item.getTitle());
    }

    @NonNull
    private List<VisionChart> copyCharts(@Nullable List<VisionChart> charts) {
        return charts == null ? new ArrayList<>() : new ArrayList<>(charts);
    }

    @Nullable
    private String resolveSelectedChartId(
            @NonNull List<VisionChart> charts,
            @Nullable String currentSelectedChartId
    ) {
        if (charts.isEmpty()) {
            return null;
        }
        return containsSelectedChart(charts, currentSelectedChartId)
                ? currentSelectedChartId
                : charts.get(0).getId();
    }

    private boolean containsSelectedChart(@NonNull List<VisionChart> charts, @Nullable String chartId) {
        if (chartId == null) {
            return false;
        }
        for (VisionChart chart : charts) {
            if (chartId.equals(chart.getId())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String buildScenarioState(@NonNull List<VisionChart> charts, @Nullable String chartId) {
        return "当前共 " + charts.size() + " 张视标卡片，已选中：" + (chartId == null ? "无" : chartId) + "。";
    }
}
