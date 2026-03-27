package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifidemo.databinding.FragmentBrvahScenarioBinding;
import com.example.wifidemo.databinding.ViewBrvahEmptyBinding;
import com.wifi.lib.mvvm.BaseMvvmFragment;

public abstract class BaseBrvahScenarioFragment extends BaseMvvmFragment<FragmentBrvahScenarioBinding, BrvahDemoViewModel> {
    @NonNull
    @Override
    protected Class<BrvahDemoViewModel> getViewModelClass() {
        return BrvahDemoViewModel.class;
    }

    @Override
    protected boolean useActivityViewModel() {
        return true;
    }

    @Override
    protected boolean enableDefaultLoadingObserver() {
        return false;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
        onSetupScenario(savedInstanceState);
    }

    protected abstract void onSetupScenario(@Nullable Bundle savedInstanceState);

    protected final void configureScenario(
            @NonNull String title,
            @NonNull String description,
            @Nullable String primaryText,
            @Nullable Runnable primaryAction,
            @Nullable String secondaryText,
            @Nullable Runnable secondaryAction
    ) {
        binding.tvScenarioTitle.setText(title);
        binding.tvScenarioDesc.setText(description);
        configureActionButton(binding.btnPrimaryAction, primaryText, primaryAction);
        configureActionButton(binding.btnSecondaryAction, secondaryText, secondaryAction);
    }

    protected final void setScenarioState(@Nullable String state) {
        if (state == null || state.trim().isEmpty()) {
            binding.tvScenarioState.setVisibility(View.GONE);
            return;
        }
        binding.tvScenarioState.setVisibility(View.VISIBLE);
        binding.tvScenarioState.setText(state);
    }

    protected final RecyclerView getScenarioRecyclerView() {
        return binding.recyclerView;
    }

    protected final View createEmptyView(@NonNull String title, @NonNull String description) {
        ViewBrvahEmptyBinding emptyBinding = ViewBrvahEmptyBinding.inflate(
                LayoutInflater.from(requireContext()),
                new FrameLayout(requireContext()),
                false
        );
        emptyBinding.getRoot().setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        emptyBinding.tvEmptyTitle.setText(title);
        emptyBinding.tvEmptyDesc.setText(description);
        return emptyBinding.getRoot();
    }

    private void configureActionButton(
            @NonNull View button,
            @Nullable String text,
            @Nullable Runnable action
    ) {
        if (text == null || text.trim().isEmpty() || action == null) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        button.setVisibility(View.VISIBLE);
        if (button instanceof android.widget.TextView) {
            ((android.widget.TextView) button).setText(text);
        }
        button.setOnClickListener(v -> action.run());
    }
}
