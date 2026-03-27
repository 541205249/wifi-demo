package com.wifi.lib.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class NetworkCallExecutor {
    private static final String TAG = "NetworkExec";
    private static final int UNKNOWN_ERROR_CODE = -1;

    private NetworkCallExecutor() {
    }

    public static <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull ApiResultCallback<T> callback
    ) {
        enqueue(call, ApiResponseParsers.identity(), callback);
    }

    public static <RAW, DATA> void enqueue(
            @NonNull Call<RAW> call,
            @NonNull ApiResponseParser<RAW, DATA> parser,
            @NonNull ApiResultCallback<DATA> callback
    ) {
        DLog.i(TAG, "开始执行网络请求，request=" + call.request().method() + " " + call.request().url());
        call.enqueue(new Callback<RAW>() {
            @Override
            public void onResponse(@NonNull Call<RAW> currentCall, @NonNull Response<RAW> response) {
                if (response.isSuccessful()) {
                    handleParsedSuccess(currentCall, response, parser, callback);
                    return;
                }

                String errorBody = readErrorBody(response);
                String message = resolveHttpErrorMessage(response, errorBody);
                DLog.w(TAG, "网络请求失败，code=" + response.code() + ", url=" + currentCall.request().url() + ", message=" + message);
                callback.onResult(ApiResult.failure(response.code(), response.code(), message, errorBody, null));
            }

            @Override
            public void onFailure(@NonNull Call<RAW> currentCall, @NonNull Throwable throwable) {
                String message;
                if (currentCall.isCanceled()) {
                    message = "请求已取消";
                } else if (!TextUtils.isEmpty(throwable.getMessage())) {
                    message = throwable.getMessage();
                } else {
                    message = "网络请求失败";
                }
                DLog.e(TAG, "网络请求异常，url=" + currentCall.request().url() + ", message=" + message, throwable);
                callback.onResult(ApiResult.failure(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_CODE, message, null, throwable));
            }
        });
    }

    private static <RAW, DATA> void handleParsedSuccess(
            @NonNull Call<RAW> currentCall,
            @NonNull Response<RAW> response,
            @NonNull ApiResponseParser<RAW, DATA> parser,
            @NonNull ApiResultCallback<DATA> callback
    ) {
        String responseMessage = TextUtils.isEmpty(response.message()) ? "请求成功" : response.message();
        try {
            ApiParsedResult<DATA> parsedResult = parser.parse(response.code(), responseMessage, response.body());
            if (parsedResult.isSuccess()) {
                DLog.i(TAG, "网络请求成功，httpCode=" + response.code()
                        + ", resultCode=" + parsedResult.getCode()
                        + ", url=" + currentCall.request().url());
                callback.onResult(ApiResult.success(
                        response.code(),
                        parsedResult.getCode(),
                        parsedResult.getData(),
                        parsedResult.getMessage()
                ));
                return;
            }

            DLog.w(TAG, "网络请求业务失败，httpCode=" + response.code()
                    + ", resultCode=" + parsedResult.getCode()
                    + ", url=" + currentCall.request().url()
                    + ", message=" + parsedResult.getMessage());
            callback.onResult(ApiResult.failure(
                    response.code(),
                    parsedResult.getCode(),
                    parsedResult.getMessage(),
                    null,
                    null
            ));
        } catch (Exception exception) {
            String message = TextUtils.isEmpty(exception.getMessage())
                    ? "结果解析失败"
                    : "结果解析失败: " + exception.getMessage();
            DLog.e(TAG, "网络请求解析异常，url=" + currentCall.request().url() + ", message=" + message, exception);
            callback.onResult(ApiResult.failure(
                    response.code(),
                    UNKNOWN_ERROR_CODE,
                    message,
                    null,
                    exception
            ));
        }
    }

    @NonNull
    private static <RAW> String resolveHttpErrorMessage(
            @NonNull Response<RAW> response,
            String errorBody
    ) {
        String responseMessage = TextUtils.isEmpty(response.message()) ? "" : response.message();
        if (TextUtils.isEmpty(errorBody)) {
            if (TextUtils.isEmpty(responseMessage)) {
                return "HTTP " + response.code() + " 请求失败";
            }
            return "HTTP " + response.code() + " 请求失败: " + responseMessage;
        }
        return "HTTP " + response.code() + " 请求失败: " + truncate(errorBody);
    }

    private static <RAW> String readErrorBody(@NonNull Response<RAW> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            return response.errorBody().string();
        } catch (IOException exception) {
            DLog.w(TAG, "读取错误响应体失败", exception);
            return null;
        }
    }

    @NonNull
    private static String truncate(@NonNull String text) {
        if (text.length() <= 512) {
            return text;
        }
        return text.substring(0, 512) + "...(truncated)";
    }
}
