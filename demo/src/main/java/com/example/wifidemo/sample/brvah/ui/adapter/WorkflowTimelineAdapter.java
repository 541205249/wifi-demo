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
    public WorkflowTimelineAdapter() {
        onItemViewType((position, list) -> list.get(position).getViewType());
        addItemType(BrvahWorkflowItem.TYPE_HEADER,
                new BaseMultiItemAdapter.OnMultiItemAdapterListener<BrvahWorkflowItem, BindingHolder<ItemBrvahWorkflowHeaderBinding>>() {
                    @NonNull
                    @Override
                    public BindingHolder<ItemBrvahWorkflowHeaderBinding> onCreate(
                            @NonNull Context context,
                            @NonNull android.view.ViewGroup parent,
                            int viewType
                    ) {
                        return new BindingHolder<>(ItemBrvahWorkflowHeaderBinding.inflate(LayoutInflater.from(context), parent, false));
                    }

                    @Override
                    public void onBind(@NonNull BindingHolder<ItemBrvahWorkflowHeaderBinding> holder, int position, BrvahWorkflowItem item) {
                        holder.binding.tvWorkflowHeaderTitle.setText(item.getTitle());
                        holder.binding.tvWorkflowHeaderSubtitle.setText(item.getSubtitle());
                        holder.binding.tvWorkflowHeaderDetail.setText(item.getDetail());
                        holder.binding.tvWorkflowHeaderBadge.setText(item.getStatusLabel());
                    }
                });
        addItemType(BrvahWorkflowItem.TYPE_STEP,
                new BaseMultiItemAdapter.OnMultiItemAdapterListener<BrvahWorkflowItem, BindingHolder<ItemBrvahWorkflowStepBinding>>() {
                    @NonNull
                    @Override
                    public BindingHolder<ItemBrvahWorkflowStepBinding> onCreate(
                            @NonNull Context context,
                            @NonNull android.view.ViewGroup parent,
                            int viewType
                    ) {
                        return new BindingHolder<>(ItemBrvahWorkflowStepBinding.inflate(LayoutInflater.from(context), parent, false));
                    }

                    @Override
                    public void onBind(@NonNull BindingHolder<ItemBrvahWorkflowStepBinding> holder, int position, BrvahWorkflowItem item) {
                        holder.binding.tvWorkflowStepIndex.setText(String.valueOf(position));
                        holder.binding.tvWorkflowStepTitle.setText(item.getTitle());
                        holder.binding.tvWorkflowStepSubtitle.setText(item.getSubtitle());
                        holder.binding.tvWorkflowStepDetail.setText(item.getDetail());
                        holder.binding.tvWorkflowStepState.setText(item.getStatusLabel());
                    }
                });
        addItemType(BrvahWorkflowItem.TYPE_ACTION,
                new BaseMultiItemAdapter.OnMultiItemAdapterListener<BrvahWorkflowItem, BindingHolder<ItemBrvahWorkflowActionBinding>>() {
                    @NonNull
                    @Override
                    public BindingHolder<ItemBrvahWorkflowActionBinding> onCreate(
                            @NonNull Context context,
                            @NonNull android.view.ViewGroup parent,
                            int viewType
                    ) {
                        return new BindingHolder<>(ItemBrvahWorkflowActionBinding.inflate(LayoutInflater.from(context), parent, false));
                    }

                    @Override
                    public void onBind(@NonNull BindingHolder<ItemBrvahWorkflowActionBinding> holder, int position, BrvahWorkflowItem item) {
                        holder.binding.tvWorkflowActionTitle.setText(item.getTitle());
                        holder.binding.tvWorkflowActionSubtitle.setText(item.getSubtitle());
                        holder.binding.tvWorkflowActionDetail.setText(item.getDetail());
                        holder.binding.tvWorkflowActionBadge.setText(item.getStatusLabel());
                        holder.binding.tvWorkflowActionBadge.setTextColor(ContextCompat.getColor(
                                holder.binding.getRoot().getContext(),
                                R.color.brand_secondary
                        ));
                    }
                });
    }
}
