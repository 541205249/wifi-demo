package com.wifi.lib.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApiParsedResult<T> {
    private final boolean success;
    private final int code;
    @Nullable
    private final T data;
    @NonNull
    private final String message;

    private ApiParsedResult(
            boolean success,
            int code,
            @Nullable T data,
            @NonNull String message
    ) {
        this.success = success;
        this.code = code;
        this.data = data;
        this.message = message;
    }

    @NonNull
    public static <T> ApiParsedResult<T> success(int code, @Nullable T data, @NonNull String message) {
        return new ApiParsedResult<>(true, code, data, message);
    }

    @NonNull
    public static <T> ApiParsedResult<T> failure(int code, @NonNull String message) {
        return new ApiParsedResult<>(false, code, null, message);
    }

    public boolean isSuccess() {
        return success;
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
}
