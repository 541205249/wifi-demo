package com.wifi.lib.network.gson;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wifi.lib.network.ApiParsedResult;
import com.wifi.lib.network.ApiResponseParser;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;

public class GsonBodyParser<T> implements ApiResponseParser<ResponseBody, T> {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EMPTY_BODY = 1001;
    public static final int RESULT_PARSE_ERROR = 1002;
    private static final String MESSAGE_EMPTY_BODY = "响应体为空";
    private static final String MESSAGE_EMPTY_BODY_STRING = "响应体为空字符串";
    private static final String MESSAGE_PARSE_RESULT_EMPTY = "Gson 解析结果为空";
    private static final String MESSAGE_PARSE_SUCCESS = "Gson 解析成功";
    private static final String MESSAGE_PARSE_FAILED_PREFIX = "Gson 解析失败: ";

    private final Gson gson;
    private final Type targetType;

    public GsonBodyParser(@NonNull Class<T> targetClass) {
        this(new Gson(), targetClass);
    }

    public GsonBodyParser(@NonNull Type targetType) {
        this(new Gson(), targetType);
    }

    public GsonBodyParser(@NonNull Gson gson, @NonNull Class<T> targetClass) {
        this(gson, (Type) targetClass);
    }

    public GsonBodyParser(@NonNull Gson gson, @NonNull Type targetType) {
        this.gson = gson;
        this.targetType = targetType;
    }

    @NonNull
    @Override
    public ApiParsedResult<T> parse(
            int httpCode,
            @NonNull String httpMessage,
            @Nullable ResponseBody body
    ) {
        if (body == null) {
            return failureEmptyBody(MESSAGE_EMPTY_BODY);
        }
        try {
            String rawJson = readBody(body);
            if (TextUtils.isEmpty(rawJson)) {
                return failureEmptyBody(MESSAGE_EMPTY_BODY_STRING);
            }
            T data = parseBody(rawJson);
            if (data == null) {
                return failureParseError(MESSAGE_PARSE_RESULT_EMPTY);
            }
            return ApiParsedResult.success(RESULT_OK, data, MESSAGE_PARSE_SUCCESS);
        } catch (JsonSyntaxException | IOException exception) {
            return failureParseError(MESSAGE_PARSE_FAILED_PREFIX + safeMessage(exception));
        }
    }

    @NonNull
    private String readBody(@NonNull ResponseBody body) throws IOException {
        return body.string();
    }

    @Nullable
    private T parseBody(@NonNull String rawJson) {
        return gson.fromJson(rawJson, targetType);
    }

    @NonNull
    private ApiParsedResult<T> failureEmptyBody(@NonNull String message) {
        return ApiParsedResult.failure(RESULT_EMPTY_BODY, message);
    }

    @NonNull
    private ApiParsedResult<T> failureParseError(@NonNull String message) {
        return ApiParsedResult.failure(RESULT_PARSE_ERROR, message);
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage())
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}
