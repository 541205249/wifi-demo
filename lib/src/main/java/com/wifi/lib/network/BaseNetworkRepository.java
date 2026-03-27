package com.wifi.lib.network;

import androidx.annotation.NonNull;

import com.wifi.lib.mvvm.BaseRepository;

import retrofit2.Call;

public abstract class BaseNetworkRepository extends BaseRepository {
    @NonNull
    protected <Service> Service createService(
            @NonNull NetworkConfig config,
            @NonNull Class<Service> serviceClass
    ) {
        return NetworkServiceFactory.createService(config, serviceClass);
    }

    protected <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull ApiResultCallback<T> callback
    ) {
        NetworkCallExecutor.enqueue(call, callback);
    }

    protected <RAW, DATA> void enqueue(
            @NonNull Call<RAW> call,
            @NonNull ApiResponseParser<RAW, DATA> parser,
            @NonNull ApiResultCallback<DATA> callback
    ) {
        NetworkCallExecutor.enqueue(call, parser, callback);
    }
}
