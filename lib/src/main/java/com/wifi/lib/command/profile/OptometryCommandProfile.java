package com.wifi.lib.command.profile;

import androidx.annotation.NonNull;

import com.wifi.lib.R;
import com.wifi.lib.command.CommandCatalog;

public final class OptometryCommandProfile implements CommandProfile {
    private static final OptometryCommandProfile INSTANCE = new OptometryCommandProfile();
    private static final String PROFILE_ID = "optometry";
    private static final String BUILT_IN_SOURCE_LABEL = "lib/raw/command_table_optometry_default.csv";

    private OptometryCommandProfile() {
    }

    @NonNull
    public static OptometryCommandProfile getInstance() {
        return INSTANCE;
    }

    @Override
    @NonNull
    public String getProfileId() {
        return PROFILE_ID;
    }

    @Override
    @NonNull
    public CommandCatalog getCatalog() {
        return OptometryCommandCatalogs.getCatalog();
    }

    @Override
    public int getBuiltInTableResId() {
        return R.raw.command_table_optometry_default;
    }

    @Override
    @NonNull
    public String getBuiltInSourceLabel() {
        return BUILT_IN_SOURCE_LABEL;
    }
}
