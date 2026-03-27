package com.wifi.lib.baseui.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
        ensureView();
        progressLayout.setVisibility(View.VISIBLE);
    }

    public void show(@NonNull CharSequence message) {
        ensureView();
        tvLoading.setText(message);
        progressLayout.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (progressLayout != null) {
            progressLayout.setVisibility(View.GONE);
        }
    }

    public void setMessage(@NonNull CharSequence message) {
        ensureView();
        tvLoading.setText(message);
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
        if (!(viewRoot instanceof ViewGroup)) {
            throw new IllegalStateException("PageLoadingUIDelegate requires a ViewGroup root");
        }
        ViewGroup parent = (ViewGroup) viewRoot;
        progressLayout = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_loading_view, parent, false);
        progressBar = progressLayout.findViewById(R.id.pb);
        tvLoading = progressLayout.findViewById(R.id.tv);
        parent.addView(progressLayout);
    }
}
