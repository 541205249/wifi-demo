package com.wifi.lib.network.gson;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.wifi.lib.network.ApiParsedResult;
import com.wifi.lib.network.ApiResponseParser;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;

public class GsonEnvelopeParser<T> implements ApiResponseParser<ResponseBody, T> {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EMPTY_BODY = 1001;
    public static final int RESULT_PARSE_ERROR = 1002;
    public static final int RESULT_INVALID_ENVELOPE = 1003;

    private final Gson gson;
    private final Type dataType;
    private final String codeFieldName;
    private final String messageFieldName;
    private final String dataFieldName;
    private final int successCode;

    public GsonEnvelopeParser(@NonNull Class<T> dataClass) {
        this(new Gson(), dataClass);
    }

    public GsonEnvelopeParser(@NonNull Type dataType) {
        this(new Gson(), dataType);
    }

    public GsonEnvelopeParser(@NonNull Gson gson, @NonNull Type dataType) {
        this(gson, dataType, "code", "message", "data", RESULT_OK);
    }

    public GsonEnvelopeParser(
            @NonNull Gson gson,
            @NonNull Type dataType,
            @NonNull String codeFieldName,
            @NonNull String messageFieldName,
            @NonNull String dataFieldName,
            int successCode
    ) {
        this.gson = gson;
        this.dataType = dataType;
        this.codeFieldName = codeFieldName;
        this.messageFieldName = messageFieldName;
        this.dataFieldName = dataFieldName;
        this.successCode = successCode;
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
            JsonElement rootElement = JsonParser.parseString(rawJson);
            if (!rootElement.isJsonObject()) {
                return ApiParsedResult.failure(RESULT_INVALID_ENVELOPE, "响应体不是 JSON Object");
            }
            JsonObject rootObject = rootElement.getAsJsonObject();
            int resultCode = readCode(rootObject);
            String resultMessage = readMessage(rootObject, httpMessage);
            if (resultCode != successCode) {
                return ApiParsedResult.failure(resultCode, resultMessage);
            }

            JsonElement dataElement = rootObject.get(dataFieldName);
            if (dataElement == null || dataElement.isJsonNull()) {
                return ApiParsedResult.success(resultCode, null, resultMessage);
            }
            T data = gson.fromJson(dataElement, dataType);
            return ApiParsedResult.success(resultCode, data, resultMessage);
        } catch (JsonSyntaxException | IOException exception) {
            return ApiParsedResult.failure(RESULT_PARSE_ERROR, "Gson 解析失败: " + safeMessage(exception));
        }
    }

    private int readCode(@NonNull JsonObject object) {
        JsonElement codeElement = object.get(codeFieldName);
        if (codeElement == null || codeElement.isJsonNull()) {
            return RESULT_INVALID_ENVELOPE;
        }
        try {
            return codeElement.getAsInt();
        } catch (RuntimeException exception) {
            return RESULT_INVALID_ENVELOPE;
        }
    }

    @NonNull
    private String readMessage(@NonNull JsonObject object, @NonNull String fallback) {
        JsonElement messageElement = object.get(messageFieldName);
        if (messageElement == null || messageElement.isJsonNull()) {
            return TextUtils.isEmpty(fallback) ? "请求成功" : fallback;
        }
        try {
            String message = messageElement.getAsString();
            return TextUtils.isEmpty(message)
                    ? (TextUtils.isEmpty(fallback) ? "请求成功" : fallback)
                    : message;
        } catch (RuntimeException exception) {
            return TextUtils.isEmpty(fallback) ? "请求成功" : fallback;
        }
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage())
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }
}
