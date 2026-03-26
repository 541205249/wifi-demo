package com.wifi.optometry.ui.main;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.optometry.R;
import com.wifi.optometry.data.ExamSeedData;
import com.wifi.optometry.domain.model.ClinicSettings;

public class SettingsFragment extends BaseClinicFragment {
    private EditText etCompanyName;
    private Switch switchCloudEnabled;
    private EditText etCloudUrl;
    private EditText etCloudAccount;
    private EditText etCloudPassword;
    private EditText etLanguage;
    private Switch switchDuration;
    private EditText etDateUnit;
    private EditText etTimeUnit;
    private EditText etSphStep;
    private EditText etSphShiftStep;
    private EditText etCylStep;
    private EditText etCylShiftStep;
    private EditText etAxisStep;
    private EditText etAxisShiftStep;
    private EditText etPrismStep;
    private EditText etPrismShiftStep;
    private EditText etPdStep;
    private EditText etPdShiftStep;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindSharedViewModel();
        bindViews(view);

        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            clinicViewModel.saveSettings(readSettingsFromForm());
            showToast("设置已保存");
        });
        view.findViewById(R.id.btnResetSettings).setOnClickListener(v -> {
            ClinicSettings defaults = ExamSeedData.createSettings();
            bindSettings(defaults);
            clinicViewModel.saveSettings(defaults);
            showToast("已恢复默认设置");
        });

        clinicViewModel.getSettings().observe(getViewLifecycleOwner(), this::bindSettings);
    }

    private void bindViews(View view) {
        etCompanyName = view.findViewById(R.id.etCompanyName);
        switchCloudEnabled = view.findViewById(R.id.switchCloudEnabled);
        etCloudUrl = view.findViewById(R.id.etCloudUrl);
        etCloudAccount = view.findViewById(R.id.etCloudAccount);
        etCloudPassword = view.findViewById(R.id.etCloudPassword);
        etLanguage = view.findViewById(R.id.etLanguage);
        switchDuration = view.findViewById(R.id.switchDuration);
        etDateUnit = view.findViewById(R.id.etDateUnit);
        etTimeUnit = view.findViewById(R.id.etTimeUnit);
        etSphStep = view.findViewById(R.id.etSphStep);
        etSphShiftStep = view.findViewById(R.id.etSphShiftStep);
        etCylStep = view.findViewById(R.id.etCylStep);
        etCylShiftStep = view.findViewById(R.id.etCylShiftStep);
        etAxisStep = view.findViewById(R.id.etAxisStep);
        etAxisShiftStep = view.findViewById(R.id.etAxisShiftStep);
        etPrismStep = view.findViewById(R.id.etPrismStep);
        etPrismShiftStep = view.findViewById(R.id.etPrismShiftStep);
        etPdStep = view.findViewById(R.id.etPdStep);
        etPdShiftStep = view.findViewById(R.id.etPdShiftStep);
    }

    private void bindSettings(ClinicSettings settings) {
        if (settings == null) {
            return;
        }
        etCompanyName.setText(settings.getCompanyName());
        switchCloudEnabled.setChecked(settings.isCloudEnabled());
        etCloudUrl.setText(settings.getCloudUrl());
        etCloudAccount.setText(settings.getCloudAccount());
        etCloudPassword.setText(settings.getCloudPassword());
        etLanguage.setText(settings.getLanguage());
        switchDuration.setChecked(settings.isShowDisplayDuration());
        etDateUnit.setText(settings.getDateUnit());
        etTimeUnit.setText(settings.getTimeUnit());
        etSphStep.setText(formatNumber(settings.getSphStep()));
        etSphShiftStep.setText(formatNumber(settings.getSphShiftStep()));
        etCylStep.setText(formatNumber(settings.getCylStep()));
        etCylShiftStep.setText(formatNumber(settings.getCylShiftStep()));
        etAxisStep.setText(formatNumber(settings.getAxisStep()));
        etAxisShiftStep.setText(formatNumber(settings.getAxisShiftStep()));
        etPrismStep.setText(formatNumber(settings.getPrismStep()));
        etPrismShiftStep.setText(formatNumber(settings.getPrismShiftStep()));
        etPdStep.setText(formatNumber(settings.getPdStep()));
        etPdShiftStep.setText(formatNumber(settings.getPdShiftStep()));
    }

    private ClinicSettings readSettingsFromForm() {
        ClinicSettings settings = new ClinicSettings();
        settings.setCompanyName(readText(etCompanyName));
        settings.setCloudEnabled(switchCloudEnabled.isChecked());
        settings.setCloudUrl(readText(etCloudUrl));
        settings.setCloudAccount(readText(etCloudAccount));
        settings.setCloudPassword(readText(etCloudPassword));
        settings.setLanguage(readText(etLanguage));
        settings.setShowDisplayDuration(switchDuration.isChecked());
        settings.setDateUnit(readText(etDateUnit));
        settings.setTimeUnit(readText(etTimeUnit));
        settings.setSphStep(readDouble(etSphStep, 0.25));
        settings.setSphShiftStep(readDouble(etSphShiftStep, 1.0));
        settings.setCylStep(readDouble(etCylStep, 0.25));
        settings.setCylShiftStep(readDouble(etCylShiftStep, 1.0));
        settings.setAxisStep(readDouble(etAxisStep, 5));
        settings.setAxisShiftStep(readDouble(etAxisShiftStep, 30));
        settings.setPrismStep(readDouble(etPrismStep, 0.5));
        settings.setPrismShiftStep(readDouble(etPrismShiftStep, 2.0));
        settings.setPdStep(readDouble(etPdStep, 0.5));
        settings.setPdShiftStep(readDouble(etPdShiftStep, 3.0));
        return settings;
    }

    private String readText(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private double readDouble(EditText editText, double defaultValue) {
        String value = readText(editText);
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String formatNumber(double value) {
        if (Math.floor(value) == value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}
