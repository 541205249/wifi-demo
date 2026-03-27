package com.example.wifidemo.sample.brvah.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wifidemo.databinding.ActivityBrvahDemoBinding;
import com.google.android.material.tabs.TabLayoutMediator;
import com.wifi.lib.baseui.BaseConfirmDialog;
import com.wifi.lib.mvvm.BaseMvvmActivity;

public class BrvahDemoActivity extends BaseMvvmActivity<ActivityBrvahDemoBinding, BrvahDemoViewModel> {
    @NonNull
    @Override
    protected Class<BrvahDemoViewModel> getViewModelClass() {
        return BrvahDemoViewModel.class;
    }

    @Override
    protected boolean enableDefaultLoadingObserver() {
        return false;
    }

    @Override
    protected void initWidgets(@Nullable Bundle savedInstanceState) {
        getStatusBarUI().setLightMode();
        getPageTitleUI().initIvBack();
        getPageTitleUI().initTitle("BRVAH 场景集");
        getPageTitleUI().initTvRight("说明", v -> showIntroDialog());

        binding.viewPager.setOffscreenPageLimit(1);
        binding.viewPager.setAdapter(new BrvahDemoPagerAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(BrvahDemoPagerAdapter.getPageTitle(position))
        ).attach();
    }

    private void showIntroDialog() {
        BaseConfirmDialog dialog = new BaseConfirmDialog(this);
        dialog.setTitleTxt("示例内容");
        dialog.setContentTxt("这个页面集中演示了 BaseRecyclerViewAdapterHelper 4.x 在你当前 BaseUI + MVVM + Repository + viewBinding 框架里的常见用法，包括普通列表、宫格、多布局、分页加载和拖拽侧滑。");
        dialog.setOkTxt("知道了");
        dialog.hideCancelTv();
        dialog.show();
    }
}
