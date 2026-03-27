package com.wifi.lib.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Interceptor;

public final class NetworkConfig {
    private final String baseUrl;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int writeTimeoutSeconds;
    private final boolean networkLogEnabled;
    private final Map<String, String> staticHeaders;
    private final NetworkHeadersProvider dynamicHeadersProvider;
    private final List<Interceptor> interceptors;

    private NetworkConfig(@NonNull Builder builder) {
        this.baseUrl = normalizeBaseUrl(builder.baseUrl);
        this.connectTimeoutSeconds = builder.connectTimeoutSeconds;
        this.readTimeoutSeconds = builder.readTimeoutSeconds;
        this.writeTimeoutSeconds = builder.writeTimeoutSeconds;
        this.networkLogEnabled = builder.networkLogEnabled;
        this.staticHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(builder.staticHeaders));
        this.dynamicHeadersProvider = builder.dynamicHeadersProvider;
        this.interceptors = Collections.unmodifiableList(new ArrayList<>(builder.interceptors));
    }

    @NonNull
    public String getBaseUrl() {
        return baseUrl;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public int getWriteTimeoutSeconds() {
        return writeTimeoutSeconds;
    }

    public boolean isNetworkLogEnabled() {
        return networkLogEnabled;
    }

    @NonNull
    public Map<String, String> getStaticHeaders() {
        return staticHeaders;
    }

    @Nullable
    public NetworkHeadersProvider getDynamicHeadersProvider() {
        return dynamicHeadersProvider;
    }

    @NonNull
    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    @NonNull
    private static String normalizeBaseUrl(@NonNull String baseUrl) {
        String trimmedUrl = baseUrl.trim();
        if (TextUtils.isEmpty(trimmedUrl)) {
            throw new IllegalArgumentException("baseUrl 不能为空");
        }
        return trimmedUrl.endsWith("/") ? trimmedUrl : trimmedUrl + "/";
    }

    public static final class Builder {
        private final String baseUrl;
        private int connectTimeoutSeconds = 15;
        private int readTimeoutSeconds = 20;
        private int writeTimeoutSeconds = 20;
        private boolean networkLogEnabled = true;
        private final Map<String, String> staticHeaders = new LinkedHashMap<>();
        private NetworkHeadersProvider dynamicHeadersProvider;
        private final List<Interceptor> interceptors = new ArrayList<>();

        public Builder(@NonNull String baseUrl) {
            this.baseUrl = baseUrl;
        }

        @NonNull
        public Builder connectTimeoutSeconds(int seconds) {
            this.connectTimeoutSeconds = requirePositive(seconds, "connectTimeoutSeconds");
            return this;
        }

        @NonNull
        public Builder readTimeoutSeconds(int seconds) {
            this.readTimeoutSeconds = requirePositive(seconds, "readTimeoutSeconds");
            return this;
        }

        @NonNull
        public Builder writeTimeoutSeconds(int seconds) {
            this.writeTimeoutSeconds = requirePositive(seconds, "writeTimeoutSeconds");
            return this;
        }

        @NonNull
        public Builder networkLogEnabled(boolean enabled) {
            this.networkLogEnabled = enabled;
            return this;
        }

        @NonNull
        public Builder addHeader(@NonNull String name, @NonNull String value) {
            if (TextUtils.isEmpty(name.trim())) {
                throw new IllegalArgumentException("header 名称不能为空");
            }
            staticHeaders.put(name, value);
            return this;
        }

        @NonNull
        public Builder dynamicHeadersProvider(@Nullable NetworkHeadersProvider provider) {
            this.dynamicHeadersProvider = provider;
            return this;
        }

        @NonNull
        public Builder addInterceptor(@NonNull Interceptor interceptor) {
            interceptors.add(interceptor);
            return this;
        }

        @NonNull
        public NetworkConfig build() {
            return new NetworkConfig(this);
        }

        private int requirePositive(int value, @NonNull String fieldName) {
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " 必须大于 0");
            }
            return value;
        }
    }
}
