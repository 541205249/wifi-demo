package com.wifi.lib.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ApiResponseParser<RAW, DATA> {
    @NonNull
    ApiParsedResult<DATA> parse(
            int httpCode,
            @NonNull String httpMessage,
            @Nullable RAW body
    ) throws Exception;
}
