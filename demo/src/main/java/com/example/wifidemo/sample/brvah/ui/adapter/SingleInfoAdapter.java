package com.example.wifidemo.sample.brvah.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.chad.library.adapter4.BaseSingleItemAdapter;
import com.example.wifidemo.databinding.ItemBrvahSingleInfoBinding;

public class SingleInfoAdapter extends BaseSingleItemAdapter<String, BindingHolder<ItemBrvahSingleInfoBinding>> {
    public SingleInfoAdapter(String message) {
        super(message);
    }

    @NonNull
    @Override
    protected BindingHolder<ItemBrvahSingleInfoBinding> onCreateViewHolder(
            @NonNull Context context,
            @NonNull android.view.ViewGroup parent,
            int viewType
    ) {
        return new BindingHolder<>(ItemBrvahSingleInfoBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull BindingHolder<ItemBrvahSingleInfoBinding> holder, String item) {
        holder.binding.tvSingleInfo.setText(item == null ? "" : item);
    }

    public void setMessage(@NonNull String message) {
        setItem(message);
    }
}
