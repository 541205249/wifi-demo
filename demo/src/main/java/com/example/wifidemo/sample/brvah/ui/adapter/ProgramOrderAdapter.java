package com.example.wifidemo.sample.brvah.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.chad.library.adapter4.BaseQuickAdapter;
import com.example.wifidemo.clinic.model.ExamProgram;
import com.example.wifidemo.databinding.ItemBrvahProgramBinding;

public class ProgramOrderAdapter extends BaseQuickAdapter<ExamProgram, BindingHolder<ItemBrvahProgramBinding>> {
    @NonNull
    @Override
    protected BindingHolder<ItemBrvahProgramBinding> onCreateViewHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent,
            int viewType
    ) {
        return new BindingHolder<>(ItemBrvahProgramBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull BindingHolder<ItemBrvahProgramBinding> holder, int position, ExamProgram item) {
        ItemBrvahProgramBinding binding = holder.binding;
        binding.tvProgramOrder.setText(String.valueOf(position + 1));
        binding.tvProgramTitle.setText(item.getTitle());
        binding.tvProgramSummary.setText(item.getSummary());
        binding.tvProgramDetail.setText(item.getDescription());
        binding.tvProgramStepCount.setText(item.getSteps().size() + " 步");
    }
}
