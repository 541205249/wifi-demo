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
            return ApiParsedResult.failure(RESULT_EMPTY_BODY, "响应体为空");
        }
        try {
            String rawJson = body.string();
            if (TextUtils.isEmpty(rawJson)) {
                return ApiParsedResult.failure(RESULT_EMPTY_BODY, "响应体为空字符串");
            }
            T data = gson.fromJson(rawJson, targetType);
            if (data == null) {
                return ApiParsedResult.failure(RESULT_PARSE_ERROR, "Gson 解析结果为空");
            }
            return ApiParsedResult.success(RESULT_OK, data, "Gson 解析成功");
        } catch (JsonSyntaxException | IOException exception) {
            return ApiParsedResult.failure(RESULT_PARSE_ERROR, "Gson 解析失败: " + safeMessage(exception));
        }
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage())
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}
