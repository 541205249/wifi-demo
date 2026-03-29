package com.example.wifidemo.sample.brvah.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter4.BaseMultiItemAdapter;
import com.example.wifidemo.R;
import com.example.wifidemo.databinding.ItemBrvahWorkflowActionBinding;
import com.example.wifidemo.databinding.ItemBrvahWorkflowHeaderBinding;
import com.example.wifidemo.databinding.ItemBrvahWorkflowStepBinding;
import com.example.wifidemo.sample.brvah.model.BrvahWorkflowItem;

public class WorkflowTimelineAdapter extends BaseMultiItemAdapter<BrvahWorkflowItem> {
    private static final int ACTION_BADGE_COLOR_RES = R.color.brand_secondary;

    public WorkflowTimelineAdapter() {
        onItemViewType((position, list) -> list.get(position).getViewType());
        addHeaderItemType();
        addStepItemType();
        addActionItemType();
    }

    private void addHeaderItemType() {
        addItemType(BrvahWorkflowItem.TYPE_HEADER,
                new BaseMultiItemAdapter.OnMultiItemAdapterListener<BrvahWorkflowItem, BindingHolder<ItemBrvahWorkflowHeaderBinding>>() {
                    @NonNull
                    @Override
                    public BindingHolder<ItemBrvahWorkflowHeaderBinding> onCreate(
                            @NonNull Context context,
                            @NonNull android.view.ViewGroup parent,
                            int viewType
                    ) {
                        return createHeaderHolder(context, parent);
                    }

                    @Override
                    public void onBind(@NonNull BindingHolder<ItemBrvahWorkflowHeaderBinding> holder, int position, BrvahWorkflowItem item) {
                        bindHeaderItem(holder.binding, item);
                    }
                });
    }

    private void addStepItemType() {
        addItemType(BrvahWorkflowItem.TYPE_STEP,
                new BaseMultiItemAdapter.OnMultiItemAdapterListener<BrvahWorkflowItem, BindingHolder<ItemBrvahWorkflowStepBinding>>() {
                    @NonNull
                    @Override
                    public BindingHolder<ItemBrvahWorkflowStepBinding> onCreate(
                            @NonNull Context context,
                            @NonNull android.view.ViewGroup parent,
                            int viewType
                    ) {
                        return createStepHolder(context, parent);
                    }

                    @Override
                    public void onBind(@NonNull BindingHolder<ItemBrvahWorkflowStepBinding> holder, int position, BrvahWorkflowItem item) {
                        bindStepItem(holder.binding, position, item);
                    }
                });
    }

    private void addActionItemType() {
        addItemType(BrvahWorkflowItem.TYPE_ACTION,
                new BaseMultiItemAdapter.OnMultiItemAdapterListener<BrvahWorkflowItem, BindingHolder<ItemBrvahWorkflowActionBinding>>() {
                    @NonNull
                    @Override
                    public BindingHolder<ItemBrvahWorkflowActionBinding> onCreate(
                            @NonNull Context context,
                            @NonNull android.view.ViewGroup parent,
                            int viewType
                    ) {
                        return createActionHolder(context, parent);
                    }

                    @Override
                    public void onBind(@NonNull BindingHolder<ItemBrvahWorkflowActionBinding> holder, int position, BrvahWorkflowItem item) {
                        bindActionItem(holder.binding, item);
                    }
                });
    }

    @NonNull
    private BindingHolder<ItemBrvahWorkflowHeaderBinding> createHeaderHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent
    ) {
        return new BindingHolder<>(ItemBrvahWorkflowHeaderBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @NonNull
    private BindingHolder<ItemBrvahWorkflowStepBinding> createStepHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent
    ) {
        return new BindingHolder<>(ItemBrvahWorkflowStepBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @NonNull
    private BindingHolder<ItemBrvahWorkflowActionBinding> createActionHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent
    ) {
        return new BindingHolder<>(ItemBrvahWorkflowActionBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    private void bindHeaderItem(
            @NonNull ItemBrvahWorkflowHeaderBinding binding,
            @NonNull BrvahWorkflowItem item
    ) {
        binding.tvWorkflowHeaderTitle.setText(item.getTitle());
        binding.tvWorkflowHeaderSubtitle.setText(item.getSubtitle());
        binding.tvWorkflowHeaderDetail.setText(item.getDetail());
        binding.tvWorkflowHeaderBadge.setText(item.getStatusLabel());
    }

    private void bindStepItem(
            @NonNull ItemBrvahWorkflowStepBinding binding,
            int position,
            @NonNull BrvahWorkflowItem item
    ) {
        binding.tvWorkflowStepIndex.setText(String.valueOf(position));
        binding.tvWorkflowStepTitle.setText(item.getTitle());
        binding.tvWorkflowStepSubtitle.setText(item.getSubtitle());
        binding.tvWorkflowStepDetail.setText(item.getDetail());
        binding.tvWorkflowStepState.setText(item.getStatusLabel());
    }

    private void bindActionItem(
            @NonNull ItemBrvahWorkflowActionBinding binding,
            @NonNull BrvahWorkflowItem item
    ) {
        binding.tvWorkflowActionTitle.setText(item.getTitle());
        binding.tvWorkflowActionSubtitle.setText(item.getSubtitle());
        binding.tvWorkflowActionDetail.setText(item.getDetail());
        binding.tvWorkflowActionBadge.setText(item.getStatusLabel());
        binding.tvWorkflowActionBadge.setTextColor(ContextCompat.getColor(
                binding.getRoot().getContext(),
                ACTION_BADGE_COLOR_RES
        ));
    }
}
