package com.wifi.lib.network;

import androidx.annotation.NonNull;

public interface ApiResultCallback<T> {
    void onResult(@NonNull ApiResult<T> result);
}
