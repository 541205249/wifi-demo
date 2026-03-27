package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter4.dragswipe.QuickDragAndSwipe;
import com.chad.library.adapter4.dragswipe.listener.DragAndSwipeDataCallback;
import com.chad.library.adapter4.dragswipe.listener.OnItemDragListener;
import com.chad.library.adapter4.dragswipe.listener.OnItemSwipeListener;
import com.example.wifidemo.clinic.model.ExamProgram;
import com.example.wifidemo.sample.brvah.ui.adapter.ProgramOrderAdapter;

import java.util.ArrayList;
import java.util.List;

public class BrvahDragSwipeFragment extends BaseBrvahScenarioFragment {
    private ProgramOrderAdapter adapter;
    private boolean swiped;

    @Override
    protected void onSetupScenario(@Nullable Bundle savedInstanceState) {
        configureScenario(
                "拖拽侧滑",
                "演示 QuickDragAndSwipe，适合流程排序、收藏夹、任务队列等支持重排的列表。",
                "恢复默认",
                viewModel::resetPrograms,
                "插入流程",
                viewModel::insertFollowUpProgram
        );

        adapter = new ProgramOrderAdapter();
        adapter.setOnItemClickListener((baseQuickAdapter, view, position) -> {
            ExamProgram item = adapter.getItem(position);
            if (item != null) {
                viewModel.notifyAction("流程：" + item.getTitle() + "，共 " + item.getSteps().size() + " 步");
            }
        });
        getScenarioRecyclerView().setLayoutManager(new LinearLayoutManager(requireContext()));
        getScenarioRecyclerView().setAdapter(adapter);

        new QuickDragAndSwipe()
                .setDragMoveFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN)
                .setSwipeMoveFlags(ItemTouchHelper.LEFT)
                .setLongPressDragEnabled(true)
                .setItemViewSwipeEnabled(true)
                .setDataCallback(new DragAndSwipeDataCallback() {
                    @Override
                    public void dataMove(int fromPosition, int toPosition) {
                        adapter.move(fromPosition, toPosition);
                    }

                    @Override
                    public void dataRemoveAt(int position) {
                        swiped = true;
                        adapter.removeAt(position);
                    }
                })
                .setItemDragListener(new OnItemDragListener() {
                    @Override
                    public void onItemDragStart(RecyclerView.ViewHolder viewHolder, int position) {
                        setScenarioState("正在拖拽第 " + (position + 1) + " 个流程...");
                    }

                    @Override
                    public void onItemDragMoving(
                            RecyclerView.ViewHolder source,
                            int from,
                            RecyclerView.ViewHolder target,
                            int to
                    ) {
                        setScenarioState("拖拽中：" + (from + 1) + " -> " + (to + 1));
                    }

                    @Override
                    public void onItemDragEnd(RecyclerView.ViewHolder viewHolder, int position) {
                        viewModel.persistPrograms(new ArrayList<>(adapter.getItems()));
                    }
                })
                .setItemSwipeListener(new OnItemSwipeListener() {
                    @Override
                    public void onItemSwipeStart(RecyclerView.ViewHolder viewHolder, int position) {
                        setScenarioState("正在侧滑删除第 " + (position + 1) + " 项...");
                    }

                    @Override
                    public void onItemSwipeEnd(RecyclerView.ViewHolder viewHolder, int position) {
                        if (swiped) {
                            swiped = false;
                            viewModel.persistPrograms(new ArrayList<>(adapter.getItems()));
                            viewModel.notifyAction("已删除一个流程卡片");
                        }
                    }

                    @Override
                    public void onItemSwiped(RecyclerView.ViewHolder viewHolder, int position, int direction) {
                    }

                    @Override
                    public void onItemSwipeMoving(
                            android.graphics.Canvas canvas,
                            RecyclerView.ViewHolder viewHolder,
                            float dX,
                            float dY,
                            boolean isCurrentlyActive
                    ) {
                    }
                })
                .attachToRecyclerView(getScenarioRecyclerView());
    }

    @Override
    protected void observeUi() {
        viewModel.getProgramsLiveData().observe(getViewLifecycleOwner(), this::renderPrograms);
    }

    private void renderPrograms(List<ExamProgram> programs) {
        List<ExamProgram> safeList = programs == null ? new ArrayList<>() : new ArrayList<>(programs);
        adapter.submitList(safeList);
        setScenarioState("长按拖动、左滑删除，当前共 " + safeList.size() + " 个流程。");
    }
}
