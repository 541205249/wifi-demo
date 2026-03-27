package com.wifi.lib.network;

import androidx.annotation.NonNull;

import java.util.Map;

public interface NetworkHeadersProvider {
    @NonNull
    Map<String, String> provideHeaders();
}
