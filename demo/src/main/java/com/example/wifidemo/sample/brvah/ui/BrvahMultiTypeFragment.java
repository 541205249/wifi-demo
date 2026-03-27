package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.wifidemo.sample.brvah.model.BrvahWorkflowItem;
import com.example.wifidemo.sample.brvah.ui.adapter.WorkflowTimelineAdapter;

import java.util.ArrayList;
import java.util.List;

public class BrvahMultiTypeFragment extends BaseBrvahScenarioFragment {
    private WorkflowTimelineAdapter adapter;

    @Override
    protected void onSetupScenario(@Nullable Bundle savedInstanceState) {
        configureScenario(
                "多布局流程",
                "演示 BaseMultiItemAdapter，适合流程编排、消息流、设备状态混排等复杂列表。",
                "追加提醒",
                viewModel::appendWorkflowAlert,
                "重置流程",
                viewModel::resetWorkflow
        );
        adapter = new WorkflowTimelineAdapter();
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> {
            BrvahWorkflowItem item = adapter.getItem(position);
            if (item != null) {
                viewModel.notifyAction(item.getTitle() + " - " + item.getStatusLabel());
            }
        });
        getScenarioRecyclerView().setLayoutManager(new LinearLayoutManager(requireContext()));
        getScenarioRecyclerView().setAdapter(adapter);
    }

    @Override
    protected void observeUi() {
        viewModel.getWorkflowLiveData().observe(getViewLifecycleOwner(), this::renderWorkflow);
    }

    private void renderWorkflow(List<BrvahWorkflowItem> items) {
        List<BrvahWorkflowItem> safeList = items == null ? new ArrayList<>() : new ArrayList<>(items);
        adapter.submitList(safeList);
        setScenarioState("当前混排 " + safeList.size() + " 个节点，包含头部、步骤和动作 3 种 itemType。");
    }
}
