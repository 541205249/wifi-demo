package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

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
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> {
            VisionChart item = adapter.getItem(position);
            if (item == null) {
                return;
            }
            selectedChartId = item.getId();
            adapter.setSelectedChartId(selectedChartId);
            adapter.notifyDataSetChanged();
            viewModel.notifyAction("预览视标：" + item.getTitle());
        });
        getScenarioRecyclerView().setLayoutManager(new QuickGridLayoutManager(requireContext(), 2));
        getScenarioRecyclerView().setAdapter(adapter);
    }

    @Override
    protected void observeUi() {
        viewModel.getChartsLiveData().observe(getViewLifecycleOwner(), this::renderCharts);
    }

    private void renderCharts(List<VisionChart> charts) {
        List<VisionChart> safeList = charts == null ? new ArrayList<>() : new ArrayList<>(charts);
        if (!safeList.isEmpty()) {
            boolean selectedExists = false;
            for (VisionChart chart : safeList) {
                if (chart.getId().equals(selectedChartId)) {
                    selectedExists = true;
                    break;
                }
            }
            if (!selectedExists) {
                selectedChartId = safeList.get(0).getId();
            }
        } else {
            selectedChartId = null;
        }
        adapter.setSelectedChartId(selectedChartId);
        adapter.submitList(safeList);
        setScenarioState("当前共 " + safeList.size() + " 张视标卡片，已选中：" + (selectedChartId == null ? "无" : selectedChartId) + "。");
    }
}
