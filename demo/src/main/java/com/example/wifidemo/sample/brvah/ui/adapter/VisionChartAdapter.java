package com.example.wifidemo.sample.brvah.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.example.wifidemo.R;
import com.example.wifidemo.clinic.model.VisionChart;
import com.example.wifidemo.databinding.ItemBrvahChartBinding;

public class VisionChartAdapter extends BaseQuickAdapter<VisionChart, BindingHolder<ItemBrvahChartBinding>> {
    private String selectedChartId;

    public void setSelectedChartId(String selectedChartId) {
        this.selectedChartId = selectedChartId;
    }

    @NonNull
    @Override
    protected BindingHolder<ItemBrvahChartBinding> onCreateViewHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent,
            int viewType
    ) {
        return new BindingHolder<>(ItemBrvahChartBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull BindingHolder<ItemBrvahChartBinding> holder, int position, VisionChart item) {
        boolean selected = item.getId().equals(selectedChartId);
        ItemBrvahChartBinding binding = holder.binding;
        binding.ivChartPreview.setImageResource(item.getImageResId());
        binding.tvChartTitle.setText(item.getTitle());
        binding.tvChartSubtitle.setText(item.getSubtitle());
        binding.tvChartDesc.setText(item.getDescription());
        binding.tvChartBadge.setText(selected ? "当前预览" : "点击预览");
        binding.tvChartBadge.setBackgroundResource(selected ? R.drawable.bg_brvah_tag_selected : R.drawable.bg_brvah_tag);
        binding.tvChartBadge.setTextColor(ContextCompat.getColor(
                binding.getRoot().getContext(),
                selected ? R.color.brand_primary : R.color.brand_text_secondary
        ));
        binding.cardChart.setStrokeColor(ContextCompat.getColor(
                binding.getRoot().getContext(),
                selected ? R.color.brand_primary : R.color.brand_surface_variant
        ));
        binding.cardChart.setCardBackgroundColor(ContextCompat.getColor(
                binding.getRoot().getContext(),
                selected ? R.color.white : R.color.brand_surface
        ));
    }
}
