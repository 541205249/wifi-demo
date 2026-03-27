package com.wifi.lib.baseui;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.wifi.lib.databinding.DialogBaseConfirmBinding;

public class BaseConfirmDialog extends BaseVBDialog<DialogBaseConfirmBinding> {
    public interface OnOkClick {
        void onConfirm();
    }

    public interface OnCancelClick {
        void onCancel();
    }

    private OnOkClick onOkClick;
    private OnCancelClick onCancelClick;

    public BaseConfirmDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initWidgets() {
        binding.tvCancel.setOnClickListener(v -> {
            dismiss();
            if (onCancelClick != null) {
                onCancelClick.onCancel();
            }
        });
        binding.tvOk.setOnClickListener(v -> {
            dismiss();
            if (onOkClick != null) {
                onOkClick.onConfirm();
            }
        });
    }

    public void setNoTitleMode() {
        binding.tvTitle.setVisibility(View.GONE);
        binding.tvContent.setVisibility(View.GONE);
        binding.tvContentOnly.setVisibility(View.VISIBLE);
    }

    public void setOnOkClick(OnOkClick onOkClick) {
        this.onOkClick = onOkClick;
    }

    public void setOnCancelClick(OnCancelClick onCancelClick) {
        this.onCancelClick = onCancelClick;
    }

    public void setTitleTxt(@NonNull String title) {
        binding.tvTitle.setText(title);
    }

    public void setTitleTxt(@StringRes int titleResId) {
        binding.tvTitle.setText(titleResId);
    }

    public void setContentTxt(@NonNull String content) {
        if (binding.tvContent.getVisibility() == View.VISIBLE) {
            binding.tvContent.setText(content);
        } else {
            binding.tvContentOnly.setText(content);
        }
    }

    public void setContentTxt(@StringRes int contentResId) {
        if (binding.tvContent.getVisibility() == View.VISIBLE) {
            binding.tvContent.setText(contentResId);
        } else {
            binding.tvContentOnly.setText(contentResId);
        }
    }

    public void setCancelTxt(@NonNull String cancel) {
        binding.tvCancel.setText(cancel);
    }

    public void setCancelTxt(@StringRes int cancelResId) {
        binding.tvCancel.setText(cancelResId);
    }

    public void setOkTxt(@NonNull String ok) {
        binding.tvOk.setText(ok);
    }

    public void setOkTxt(@StringRes int okResId) {
        binding.tvOk.setText(okResId);
    }

    public void hideCancelTv() {
        binding.tvCancel.setVisibility(View.GONE);
        binding.border.setVisibility(View.GONE);
    }
}
