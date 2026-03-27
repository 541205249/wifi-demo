package com.wifi.lib.baseui.delegate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.wifi.lib.R;

public class PageTitleUIDelegate {
    public interface BackAction {
        void backPressed();
    }

    private final View viewRoot;
    private final int statusBarHeight;
    private final BackAction backAction;

    private View titleRoot;
    private View statusSpacer;
    private TextView tvTitle;
    private TextView tvLeft;
    private TextView tvRight;
    private ImageView ivLeft;
    private ImageView ivRight;

    public PageTitleUIDelegate(@NonNull View viewRoot, int statusBarHeight, @NonNull BackAction backAction) {
        this.viewRoot = viewRoot;
        this.statusBarHeight = statusBarHeight;
        this.backAction = backAction;
    }

    public void initTitle(@StringRes int titleResId) {
        initTitle(viewRoot.getContext().getString(titleResId));
    }

    public void initTitle(@NonNull String title) {
        ensureView();
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText(title);
    }

    public void initTitle(@NonNull String title, @ColorRes int colorRes) {
        ensureView();
        tvTitle.setVisibility(View.VISIBLE);
        tvTitle.setText(title);
        tvTitle.setTextColor(ContextCompat.getColor(viewRoot.getContext(), colorRes));
    }

    public void initTitleRootBg(@ColorRes int colorRes) {
        ensureView();
        titleRoot.setBackgroundColor(ContextCompat.getColor(viewRoot.getContext(), colorRes));
    }

    public void initIvBack() {
        initIvBack(R.drawable.ic_page_title_back);
    }

    public void initIvBack(@DrawableRes int drawableRes) {
        initIvLeft(drawableRes, v -> backAction.backPressed());
    }

    public void initIvLeft(@DrawableRes int drawableRes, @NonNull View.OnClickListener listener) {
        ensureView();
        ivLeft.setVisibility(View.VISIBLE);
        ivLeft.setImageResource(drawableRes);
        ivLeft.setOnClickListener(listener);
    }

    public void initIvRight(@DrawableRes int drawableRes, @NonNull View.OnClickListener listener) {
        ensureView();
        ivRight.setVisibility(View.VISIBLE);
        ivRight.setImageResource(drawableRes);
        ivRight.setOnClickListener(listener);
    }

    public void initTvBack(@NonNull String text) {
        initTvLeft(text, v -> backAction.backPressed());
    }

    public void initTvBack(@StringRes int textResId) {
        initTvBack(viewRoot.getContext().getString(textResId));
    }

    public void initTvLeft(@NonNull String text, @NonNull View.OnClickListener listener) {
        ensureView();
        tvLeft.setVisibility(View.VISIBLE);
        tvLeft.setText(text);
        tvLeft.setOnClickListener(listener);
    }

    public void initTvRight(@NonNull String text, @NonNull View.OnClickListener listener) {
        ensureView();
        tvRight.setVisibility(View.VISIBLE);
        tvRight.setText(text);
        tvRight.setOnClickListener(listener);
    }

    public void initTvRight(@StringRes int textResId, @NonNull View.OnClickListener listener) {
        initTvRight(viewRoot.getContext().getString(textResId), listener);
    }

    public View getTitleView() {
        ensureView();
        return titleRoot;
    }

    public TextView getTvTitle() {
        ensureView();
        return tvTitle;
    }

    public TextView getTvLeft() {
        ensureView();
        return tvLeft;
    }

    public TextView getTvRight() {
        ensureView();
        return tvRight;
    }

    public ImageView getIvLeft() {
        ensureView();
        return ivLeft;
    }

    public ImageView getIvRight() {
        ensureView();
        return ivRight;
    }

    private void ensureView() {
        if (titleRoot != null) {
            return;
        }
        if (!(viewRoot instanceof ViewGroup)) {
            throw new IllegalStateException("PageTitleUIDelegate requires a ViewGroup root");
        }
        ViewGroup parent = (ViewGroup) viewRoot;
        titleRoot = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_title_view, parent, false);
        statusSpacer = titleRoot.findViewById(R.id.viewStatusBarSpacer);
        tvTitle = titleRoot.findViewById(R.id.tv_title);
        tvLeft = titleRoot.findViewById(R.id.tv_left);
        tvRight = titleRoot.findViewById(R.id.tv_right);
        ivLeft = titleRoot.findViewById(R.id.iv_left);
        ivRight = titleRoot.findViewById(R.id.iv_more);

        ViewGroup.LayoutParams spacerParams = statusSpacer.getLayoutParams();
        spacerParams.height = Math.max(statusBarHeight, 0);
        statusSpacer.setLayoutParams(spacerParams);

        parent.addView(titleRoot);
    }
}
