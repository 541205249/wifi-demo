package com.wifi.lib.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;

public final class ApiResponseParsers {
    private ApiResponseParsers() {
    }

    @NonNull
    public static <T> ApiResponseParser<T, T> identity() {
        return (httpCode, httpMessage, body) -> ApiParsedResult.success(
                httpCode,
                body,
                TextUtils.isEmpty(httpMessage) ? "请求成功" : httpMessage
        );
    }
}
