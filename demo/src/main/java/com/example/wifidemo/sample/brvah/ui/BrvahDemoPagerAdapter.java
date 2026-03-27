package com.example.wifidemo.sample.brvah.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class BrvahDemoPagerAdapter extends FragmentStateAdapter {
    private static final String[] PAGE_TITLES = new String[]{
            "列表",
            "宫格",
            "多布局",
            "分页",
            "拖拽"
    };

    public BrvahDemoPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public static String getPageTitle(int position) {
        return PAGE_TITLES[position];
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new BrvahSimpleListFragment();
            case 1:
                return new BrvahGridFragment();
            case 2:
                return new BrvahMultiTypeFragment();
            case 3:
                return new BrvahLoadMoreFragment();
            case 4:
            default:
                return new BrvahDragSwipeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_TITLES.length;
    }
}
