package com.wifi.optometry.ui.main;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.wifi.optometry.R;
import com.wifi.optometry.databinding.FragmentPatientBinding;
import com.wifi.optometry.domain.model.PatientProfile;
import com.wifi.optometry.ui.shared.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.List;

public class PatientFragment extends BaseClinicFragment<FragmentPatientBinding> {
    private EditText etSearch;
    private LinearLayout layoutPatients;
    private EditText etSelectedPatient;
    private final List<PatientProfile> patientList = new ArrayList<>();
    private PatientProfile currentPatient;

    @Nullable
    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        etSearch = binding.etSearchPatient;
        layoutPatients = binding.layoutPatients;
        etSelectedPatient = binding.etSelectedPatientSummary;

        binding.btnAddPatient.setOnClickListener(v -> showPatientEditor(null));
        binding.btnImportPatient.setOnClickListener(v -> showImportDialog());
        etSearch.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clinicViewModel.searchPatients(s == null ? "" : s.toString());
            }
        });
    }

    @Override
    protected void observeUi() {
        clinicViewModel.getPatients().observe(getViewLifecycleOwner(), patients -> {
            patientList.clear();
            if (patients != null) {
                patientList.addAll(patients);
            }
            renderPatients();
        });
        clinicViewModel.getSession().observe(getViewLifecycleOwner(), session -> {
            currentPatient = session == null ? null : session.getPatient();
            if (currentPatient == null) {
                etSelectedPatient.setText("当前未选择被测者，可手动录入或导入扫码串。");
            } else {
                etSelectedPatient.setText(currentPatient.getDisplayName() + " | " + currentPatient.getPhone()
                        + "\n" + currentPatient.getGender() + " | " + currentPatient.getBirthDate()
                        + "\n" + currentPatient.getAddress()
                        + "\n备注：" + currentPatient.getNote());
            }
        });
    }

    private void renderPatients() {
        layoutPatients.removeAllViews();
        if (patientList.isEmpty()) {
            layoutPatients.addView(createText(requireContext(), "没有匹配到被测者。", 14,
                    requireContext().getColor(R.color.brand_text_secondary), false));
            return;
        }
        for (PatientProfile patient : patientList) {
            com.google.android.material.card.MaterialCardView cardView = createCard();
            LinearLayout content = createCardContent(cardView);
            content.addView(createText(requireContext(), patient.getDisplayName(), 18,
                    requireContext().getColor(R.color.brand_text_primary), true));
            content.addView(createText(requireContext(),
                    patient.getPhone() + " | " + patient.getGender() + " | " + patient.getBirthDate(),
                    14, requireContext().getColor(R.color.brand_text_secondary), false));
            content.addView(createText(requireContext(),
                    patient.getAddress() + "\n备注：" + patient.getNote(),
                    14, requireContext().getColor(R.color.brand_text_secondary), false));

            LinearLayout actions = new LinearLayout(requireContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setPadding(0, dp(12), 0, 0);

            com.google.android.material.button.MaterialButton btnSelect = createActionButton("选中");
            btnSelect.setOnClickListener(v -> clinicViewModel.selectPatient(patient.getId()));
            actions.addView(btnSelect);

            com.google.android.material.button.MaterialButton btnEdit = createActionButton("编辑");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = dp(8);
            btnEdit.setLayoutParams(params);
            btnEdit.setOnClickListener(v -> showPatientEditor(patient));
            actions.addView(btnEdit);

            content.addView(actions);
            layoutPatients.addView(cardView);
        }
    }

    private void showPatientEditor(@Nullable PatientProfile source) {
        PatientProfile editable = source == null ? new PatientProfile() : source.copy();
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextInputEditText etName = createInput("姓名", editable.getName(), InputType.TYPE_CLASS_TEXT);
        TextInputEditText etPhone = createInput("电话", editable.getPhone(), InputType.TYPE_CLASS_PHONE);
        TextInputEditText etGender = createInput("性别", editable.getGender(), InputType.TYPE_CLASS_TEXT);
        TextInputEditText etBirth = createInput("出生日期", editable.getBirthDate(), InputType.TYPE_CLASS_DATETIME);
        TextInputEditText etAddress = createInput("地址", editable.getAddress(), InputType.TYPE_CLASS_TEXT);
        TextInputEditText etNote = createInput("备注", editable.getNote(), InputType.TYPE_CLASS_TEXT);

        form.addView(etName);
        form.addView(etPhone);
        form.addView(etGender);
        form.addView(etBirth);
        form.addView(etAddress);
        form.addView(etNote);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(source == null ? "新增被测者" : "编辑被测者")
                .setView(form)
                .setPositiveButton("保存", (dialog, which) -> {
                    editable.setName(readValue(etName));
                    editable.setPhone(readValue(etPhone));
                    editable.setGender(readValue(etGender));
                    editable.setBirthDate(readValue(etBirth));
                    editable.setAddress(readValue(etAddress));
                    editable.setNote(readValue(etNote));
                    clinicViewModel.savePatient(editable);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showImportDialog() {
        TextInputEditText input = createInput("扫码串，格式：姓名|电话|性别|出生日期|地址|备注", "", InputType.TYPE_CLASS_TEXT);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("导入扫码串")
                .setView(input)
                .setPositiveButton("导入", (dialog, which) -> clinicViewModel.importPatientFromCode(readValue(input)))
                .setNegativeButton("取消", null)
                .show();
    }

    private TextInputEditText createInput(String hint, String value, int inputType) {
        TextInputEditText editText = new TextInputEditText(requireContext());
        editText.setHint(hint);
        editText.setInputType(inputType);
        editText.setText(value);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(8);
        editText.setLayoutParams(params);
        return editText;
    }

    private String readValue(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
