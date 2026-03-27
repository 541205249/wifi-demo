package com.wifi.lib.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApiResult<T> {
    private final boolean success;
    private final int httpCode;
    private final int code;
    @Nullable
    private final T data;
    @NonNull
    private final String message;
    @Nullable
    private final String errorBody;
    @Nullable
    private final Throwable throwable;

    private ApiResult(
            boolean success,
            int httpCode,
            int code,
            @Nullable T data,
            @NonNull String message,
            @Nullable String errorBody,
            @Nullable Throwable throwable
    ) {
        this.success = success;
        this.httpCode = httpCode;
        this.code = code;
        this.data = data;
        this.message = message;
        this.errorBody = errorBody;
        this.throwable = throwable;
    }

    @NonNull
    public static <T> ApiResult<T> success(int code, @Nullable T data, @NonNull String message) {
        return success(code, code, data, message);
    }

    @NonNull
    public static <T> ApiResult<T> success(int httpCode, int code, @Nullable T data, @NonNull String message) {
        return new ApiResult<>(true, httpCode, code, data, message, null, null);
    }

    @NonNull
    public static <T> ApiResult<T> failure(
            int code,
            @NonNull String message,
            @Nullable String errorBody,
            @Nullable Throwable throwable
    ) {
        return failure(code, code, message, errorBody, throwable);
    }

    @NonNull
    public static <T> ApiResult<T> failure(
            int httpCode,
            int code,
            @NonNull String message,
            @Nullable String errorBody,
            @Nullable Throwable throwable
    ) {
        return new ApiResult<>(false, httpCode, code, null, message, errorBody, throwable);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getErrorBody() {
        return errorBody;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }
}
