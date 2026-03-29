package com.wifi.lib.baseui.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.R;

public class PageLoadingUIDelegate {
    private final View viewRoot;

    private View progressLayout;
    private TextView tvLoading;
    private ProgressBar progressBar;

    public PageLoadingUIDelegate(@NonNull View viewRoot) {
        this.viewRoot = viewRoot;
    }

    public void show() {
        showInternal(null);
    }

    public void show(@Nullable CharSequence message) {
        showInternal(message);
    }

    public void hide() {
        if (progressLayout != null) {
            progressLayout.setVisibility(View.GONE);
        }
    }

    public void setMessage(@Nullable CharSequence message) {
        ensureView();
        tvLoading.setText(message == null ? "" : message);
    }

    public TextView getTvLoading() {
        ensureView();
        return tvLoading;
    }

    public ProgressBar getProgressBar() {
        ensureView();
        return progressBar;
    }

    private void ensureView() {
        if (progressLayout != null) {
            return;
        }
        ViewGroup parent = requireViewGroupRoot();
        progressLayout = inflateProgressLayout(parent);
        progressBar = progressLayout.findViewById(R.id.pb);
        tvLoading = progressLayout.findViewById(R.id.tv);
        parent.addView(progressLayout);
    }

    private void showInternal(@Nullable CharSequence message) {
        ensureView();
        if (message != null) {
            tvLoading.setText(message);
        }
        progressLayout.setVisibility(View.VISIBLE);
    }

    @NonNull
    private ViewGroup requireViewGroupRoot() {
        if (!(viewRoot instanceof ViewGroup)) {
            throw new IllegalStateException("PageLoadingUIDelegate requires a ViewGroup root");
        }
        return (ViewGroup) viewRoot;
    }

    @NonNull
    private View inflateProgressLayout(@NonNull ViewGroup parent) {
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_loading_view, parent, false);
    }
}
