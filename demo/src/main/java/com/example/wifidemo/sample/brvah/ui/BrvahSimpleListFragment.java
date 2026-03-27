package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.example.wifidemo.clinic.model.KnownDeviceSummary;
import com.example.wifidemo.sample.brvah.ui.adapter.KnownDeviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class BrvahSimpleListFragment extends BaseBrvahScenarioFragment {
    private KnownDeviceAdapter adapter;

    @Override
    protected void onSetupScenario(@Nullable Bundle savedInstanceState) {
        configureScenario(
                "普通列表",
                "演示 BaseQuickAdapter、item click、child click、动画和 EmptyView 的组合写法。",
                "清空列表",
                viewModel::clearDevices,
                "恢复示例",
                viewModel::resetDevices
        );
        adapter = new KnownDeviceAdapter();
        adapter.setAnimationEnable(true);
        adapter.setItemAnimation(BaseQuickAdapter.AnimationType.AlphaIn);
        adapter.setEmptyView(createEmptyView("暂无设备", "这里使用了 BRVAH 的 EmptyView，适合设备档案、历史记录等空态页面。"));
        adapter.setEmptyViewEnable(true);
        adapter.setOnDeviceActionClickListener(item -> viewModel.notifyAction("查看设备档案：" + item.getDeviceId()));
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> {
            KnownDeviceSummary item = adapter.getItem(position);
            if (item != null) {
                viewModel.notifyAction("点击设备：" + item.getDisplayLabel());
            }
        });
        getScenarioRecyclerView().setLayoutManager(new LinearLayoutManager(requireContext()));
        getScenarioRecyclerView().setAdapter(adapter);
    }

    @Override
    protected void observeUi() {
        viewModel.getDevicesLiveData().observe(getViewLifecycleOwner(), this::renderDevices);
    }

    private void renderDevices(List<KnownDeviceSummary> devices) {
        List<KnownDeviceSummary> safeList = devices == null ? new ArrayList<>() : new ArrayList<>(devices);
        adapter.submitList(safeList);
        setScenarioState("当前共 " + safeList.size() + " 个设备卡片，子按钮演示 child click。");
    }
}
