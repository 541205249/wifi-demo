package com.example.wifidemo.sample.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.wifidemo.R;
import com.example.wifidemo.databinding.FragmentDemoFeatureBinding;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.baseui.delegate.PermissionDelegate;
import com.wifi.lib.mvvm.BaseMvvmFragment;

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
        binding.btnSaveNote.setOnClickListener(v -> handleSaveNote());
        binding.btnShowConfirm.setOnClickListener(v -> showConfirmDialog());
        binding.btnShowSheet.setOnClickListener(v -> new DemoTipsBottomSheetDialog(requireContext()).show());
        binding.btnRequestPermission.setOnClickListener(v -> requestNotificationPermission());
    }

    @Override
    protected void observeUi() {
        viewModel.getRecordsLiveData().observe(getViewLifecycleOwner(), this::renderRecords);
    }

    private void renderRecords(List<String> records) {
        binding.layoutRecords.removeAllViews();
        if (records == null || records.isEmpty()) {
            renderEmptyRecordsHint();
            return;
        }
        int limit = Math.min(records.size(), 8);
        for (int index = 0; index < limit; index++) {
            binding.layoutRecords.addView(createRecordView(records.get(index)));
        }
    }

    private void handleSaveNote() {
        String note = readNoteInput();
        viewModel.appendNote(note);
        if (!TextUtils.isEmpty(note)) {
            binding.etNote.setText("");
        }
    }

    @NonNull
    private String readNoteInput() {
        return binding.etNote.getText() == null ? "" : binding.etNote.getText().toString().trim();
    }

    private void renderEmptyRecordsHint() {
        TextView emptyView = new TextView(requireContext());
        emptyView.setText("暂无演示记录");
        emptyView.setTextColor(requireContext().getColor(R.color.brand_text_secondary));
        binding.layoutRecords.addView(emptyView);
    }

    @NonNull
    private TextView createRecordView(@NonNull String record) {
        TextView recordView = new TextView(requireContext());
        recordView.setText(record);
        recordView.setBackgroundResource(R.drawable.demo_record_bg);
        recordView.setPadding(dp(12), dp(12), dp(12), dp(12));
        recordView.setTextColor(requireContext().getColor(R.color.brand_text_primary));
        recordView.setLayoutParams(createRecordLayoutParams());
        return recordView;
    }

    @NonNull
    private LinearLayout.LayoutParams createRecordLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(8);
        return params;
    }

    private void showConfirmDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(requireContext());
        dialog.setTitleTxt("确认示例");
        dialog.setContentTxt("点击确定后，会向 Repository 追加一条由确认弹框触发的演示记录。");
        dialog.setOnOkClick(() -> viewModel.appendNote("确认弹框已执行"));
        dialog.show();
    }

    private void requestNotificationPermission() {
        if (canUseNotificationWithoutRuntimeRequest() || hasNotificationPermission()) {
            viewModel.updatePermissionState(true);
            return;
        }
        requestNotificationPermissionWithDelegate();
    }

    private boolean canUseNotificationWithoutRuntimeRequest() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
    }

    private boolean hasNotificationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermissionWithDelegate() {
        getPermissionDelegate()
                .setHintTxt("权限示例", "演示页会请求通知权限，方便你参考 BaseUI 的权限代理调用方式。")
                .requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION,
                        createNotificationPermissionCallback());
    }

    @NonNull
    private PermissionDelegate.Callback createNotificationPermissionCallback() {
        return new PermissionDelegate.Callback() {
            @Override
            public void granted() {
                viewModel.updatePermissionState(true);
            }

            @Override
            public void denied(List<String> deniedList) {
                viewModel.updatePermissionState(false);
            }
        };
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }
}
