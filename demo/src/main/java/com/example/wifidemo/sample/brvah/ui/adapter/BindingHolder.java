package com.example.wifidemo.sample.brvah.ui.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public class BindingHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {
    public final VB binding;

    public BindingHolder(@NonNull VB binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
