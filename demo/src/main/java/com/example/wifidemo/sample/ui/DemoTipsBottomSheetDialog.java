package com.example.wifidemo.sample.ui;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.wifidemo.databinding.DialogDemoTipsBinding;
import com.wifi.lib.baseui.BaseVBBottomSheetDialog;

public class DemoTipsBottomSheetDialog extends BaseVBBottomSheetDialog<DialogDemoTipsBinding> {
    public DemoTipsBottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void initWidgets() {
        binding.tvTips.setText("BaseVBBottomSheetDialog 适合放操作说明、轻量表单或二级菜单，这里保留了最小示例。");
        binding.btnClose.setOnClickListener(v -> dismiss());
    }
}
