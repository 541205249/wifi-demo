package com.example.wifidemo.sample.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.FragmentDemoFeatureBinding;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.log.JLog;
import com.wifi.lib.log.JLogExporter;
import com.wifi.lib.mvvm.BaseMvvmFragment;
import com.wifi.lib.utils.Toasty;

import java.util.List;

public class DemoFeatureFragment extends BaseMvvmFragment<FragmentDemoFeatureBinding, DemoViewModel> {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 2001;

    @NonNull
    @Override
    protected Class<DemoViewModel> getViewModelClass() {
        return DemoViewModel.class;
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
    protected boolean enableDefaultMessageObserver() {
        return false;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        binding.btnRefreshRecords.setOnClickListener(v -> viewModel.refreshRecords());
        binding.btnAddRecord.setOnClickListener(v -> viewModel.addMockRecord());
        binding.btnSaveNote.setOnClickListener(v -> {
            String note = binding.etNote.getText() == null ? "" : binding.etNote.getText().toString().trim();
            viewModel.appendNote(note);
            if (!TextUtils.isEmpty(note)) {
                binding.etNote.setText("");
            }
        });
        binding.btnShowConfirm.setOnClickListener(v -> showConfirmDialog());
        binding.btnShowSheet.setOnClickListener(v -> new DemoTipsBottomSheetDialog(requireContext()).show());
        binding.btnRequestPermission.setOnClickListener(v -> requestNotificationPermission());

        ComponentActivity activity = (ComponentActivity) requireActivity();
        JLogExporter.get().hookToExport(activity, binding.btnExportLocalLog, new JLogExporter.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                JLog.i("DemoFeatureFragment", "exportToLocal success: " + message);
                viewModel.appendSystemRecord("本地导出成功：" + message);
                Toasty.showLong(message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                JLog.e("DemoFeatureFragment", "exportToLocal failed: " + errorMessage);
                viewModel.appendSystemRecord("本地导出失败：" + errorMessage);
                Toasty.showLong(errorMessage);
            }
        });
        JLogExporter.get().hookToShare(activity, binding.btnShareLog, new JLogExporter.Callback() {
            @Override
            public void onSuccess(@NonNull String message) {
                JLog.i("DemoFeatureFragment", "shareToSocial success: " + message);
                viewModel.appendSystemRecord("社交分享已触发：" + message);
                Toasty.showLong(message);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                JLog.e("DemoFeatureFragment", "shareToSocial failed: " + errorMessage);
                viewModel.appendSystemRecord("社交分享失败：" + errorMessage);
                Toasty.showLong(errorMessage);
            }
        });
    }

    @Override
    protected void observeUi() {
        viewModel.getRecordsLiveData().observe(getViewLifecycleOwner(), this::renderRecords);
    }

    private void renderRecords(List<String> records) {
        binding.layoutRecords.removeAllViews();
        if (records == null || records.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText("暂无演示记录");
            emptyView.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
            binding.layoutRecords.addView(emptyView);
            return;
        }
        int limit = Math.min(records.size(), 8);
        for (int index = 0; index < limit; index++) {
            TextView recordView = new TextView(requireContext());
            recordView.setText(records.get(index));
            recordView.setBackgroundResource(R.drawable.demo_record_bg);
            recordView.setPadding(dp(12), dp(12), dp(12), dp(12));
            recordView.setTextColor(requireContext().getColor(R.color.brand_text_primary));
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            recordView.setLayoutParams(params);
            binding.layoutRecords.addView(recordView);
        }
    }

    private void showConfirmDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(requireContext());
        dialog.setTitleTxt("确认示例");
        dialog.setContentTxt("点击确定后，会向 Repository 追加一条由确认弹框触发的演示记录。");
        dialog.setOnOkClick(() -> viewModel.appendNote("确认弹框已执行"));
        dialog.show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.updatePermissionState(true);
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.updatePermissionState(true);
            return;
        }
        getPermissionDelegate()
                .setHintTxt("权限示例", "演示页会请求通知权限，方便你参考 BaseUI 的权限代理调用方式。")
                .requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION,
                        new com.wifi.lib.baseui.delegate.PermissionDelegate.Callback() {
                            @Override
                            public void granted() {
                                viewModel.updatePermissionState(true);
                            }

                            @Override
                            public void denied(List<String> deniedList) {
                                viewModel.updatePermissionState(false);
                            }
                        });
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
