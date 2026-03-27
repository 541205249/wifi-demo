package com.wifi.optometry.ui.main;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.wifi.lib.mvvm.BaseMvvmFragment;
import com.wifi.lib.utils.Toasty;
import com.wifi.optometry.R;
import com.wifi.optometry.ui.MainActivity;
import com.wifi.optometry.ui.state.ClinicViewModel;

public abstract class BaseClinicFragment<VB extends ViewBinding> extends BaseMvvmFragment<VB, ClinicViewModel> {
    protected ClinicViewModel clinicViewModel;

    @NonNull
    @Override
    protected Class<ClinicViewModel> getViewModelClass() {
        return ClinicViewModel.class;
    }

    @Override
    protected boolean useActivityViewModel() {
        return true;
    }

    @Override
    protected void onViewModelCreated(@NonNull ClinicViewModel viewModel) {
        clinicViewModel = viewModel;
    }

    @Override
    protected boolean enableDefaultLoadingObserver() {
        return false;
    }

    @Override
    protected boolean enableDefaultMessageObserver() {
        return false;
    }

    protected MainActivity mainActivity() {
        return (MainActivity) requireActivity();
    }

    protected int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    protected void showToast(String message) {
        Toasty.showShort(message);
    }

    protected MaterialCardView createCard() {
        MaterialCardView cardView = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(12);
        cardView.setLayoutParams(params);
        cardView.setRadius(dp(18));
        cardView.setUseCompatPadding(true);
        cardView.setCardBackgroundColor(requireContext().getColor(R.color.brand_surface));
        return cardView;
    }

    protected LinearLayout createCardContent(@NonNull MaterialCardView cardView) {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        cardView.addView(content);
        return content;
    }

    protected TextView createText(Context context, String text, float textSizeSp, @ColorInt int color, boolean bold) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(textSizeSp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return textView;
    }

    protected MaterialButton createActionButton(String text) {
        MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        return button;
    }
}
