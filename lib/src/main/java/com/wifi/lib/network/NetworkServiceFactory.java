package com.wifi.lib.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class NetworkServiceFactory {
    private NetworkServiceFactory() {
    }

    @NonNull
    public static <Service> Service createService(
            @NonNull NetworkConfig config,
            @NonNull Class<Service> serviceClass
    ) {
        return createRetrofit(config).create(serviceClass);
    }

    @NonNull
    public static Retrofit createRetrofit(@NonNull NetworkConfig config) {
        return new Retrofit.Builder()
                .baseUrl(config.getBaseUrl())
                .client(createOkHttpClient(config))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @NonNull
    public static OkHttpClient createOkHttpClient(@NonNull NetworkConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(config.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(config.getWriteTimeoutSeconds(), TimeUnit.SECONDS)
                .addInterceptor(createHeaderInterceptor(config));
        if (config.isNetworkLogEnabled()) {
            builder.addInterceptor(new DLogNetworkInterceptor());
        }
        for (Interceptor interceptor : config.getInterceptors()) {
            builder.addInterceptor(interceptor);
        }
        return builder.build();
    }

    @NonNull
    private static Interceptor createHeaderInterceptor(@NonNull NetworkConfig config) {
        return new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request.Builder builder = originalRequest.newBuilder();
                appendHeaders(builder, config.getStaticHeaders());
                NetworkHeadersProvider provider = config.getDynamicHeadersProvider();
                if (provider != null) {
                    appendHeaders(builder, provider.provideHeaders());
                }
                return chain.proceed(builder.build());
            }
        };
    }

    private static void appendHeaders(
            @NonNull Request.Builder builder,
            Map<String, String> headers
    ) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (TextUtils.isEmpty(entry.getKey()) || entry.getValue() == null) {
                continue;
            }
            builder.header(entry.getKey(), entry.getValue());
        }
    }
}
