package com.example.wifidemo.sample.network.data;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.example.wifidemo.sample.network.model.EchoEnvelope;
import com.wifi.lib.network.ApiParsedResult;
import com.wifi.lib.network.ApiResponseParser;
import com.wifi.lib.network.gson.GsonBodyParser;

import okhttp3.ResponseBody;

public class EchoEnvelopeParser implements ApiResponseParser<ResponseBody, EchoEnvelope> {
    public static final int RESULT_OK = 0;
    public static final int RESULT_EMPTY_BODY = 1001;
    public static final int RESULT_INVALID_ECHO = 1002;

    private final GsonBodyParser<EchoEnvelope> gsonBodyParser = new GsonBodyParser<>(EchoEnvelope.class);

    @NonNull
    @Override
    public ApiParsedResult<EchoEnvelope> parse(
            int httpCode,
            @NonNull String httpMessage,
            ResponseBody body
    ) {
        ApiParsedResult<EchoEnvelope> gsonResult = gsonBodyParser.parse(httpCode, httpMessage, body);
        if (!gsonResult.isSuccess()) {
            return gsonResult;
        }
        EchoEnvelope envelope = gsonResult.getData();
        if (envelope == null) {
            return ApiParsedResult.failure(RESULT_EMPTY_BODY, "响应体为空");
        }
        if (TextUtils.isEmpty(envelope.getUrl())) {
            return ApiParsedResult.failure(RESULT_INVALID_ECHO, "回显数据不完整，缺少 url");
        }
        return ApiParsedResult.success(RESULT_OK, envelope, "Gson 业务解析成功");
    }
}
