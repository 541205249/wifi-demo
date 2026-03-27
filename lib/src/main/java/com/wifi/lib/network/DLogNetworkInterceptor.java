package com.wifi.lib.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.DLog;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class DLogNetworkInterceptor implements Interceptor {
    private static final String TAG = "NetworkHttp";
    private static final long MAX_PREVIEW_BYTES = 2 * 1024L;

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        long startNanos = System.nanoTime();
        DLog.d(TAG, buildRequestLog(request));
        try {
            Response response = chain.proceed(request);
            long tookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            DLog.d(TAG, buildResponseLog(request, response, tookMillis));
            return response;
        } catch (IOException exception) {
            long tookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            DLog.e(TAG, "<-- HTTP FAILED " + request.method() + " " + request.url() + " (" + tookMillis + " ms)", exception);
            throw exception;
        }
    }

    @NonNull
    private String buildRequestLog(@NonNull Request request) {
        StringBuilder builder = new StringBuilder();
        builder.append("--> ")
                .append(request.method())
                .append(' ')
                .append(request.url());
        RequestBody body = request.body();
        if (body == null) {
            return builder.toString();
        }
        MediaType mediaType = body.contentType();
        if (mediaType != null) {
            builder.append("\nContent-Type: ").append(mediaType);
        }
        long contentLength = safeContentLength(body);
        if (contentLength >= 0) {
            builder.append("\nContent-Length: ").append(contentLength);
        }
        String preview = requestBodyPreview(body);
        if (!TextUtils.isEmpty(preview)) {
            builder.append("\nBody: ").append(preview);
        }
        return builder.toString();
    }

    @NonNull
    private String buildResponseLog(
            @NonNull Request request,
            @NonNull Response response,
            long tookMillis
    ) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("<-- ")
                .append(response.code());
        if (!TextUtils.isEmpty(response.message())) {
            builder.append(' ').append(response.message());
        }
        builder.append(' ')
                .append(request.method())
                .append(' ')
                .append(request.url())
                .append(" (")
                .append(tookMillis)
                .append(" ms)");
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return builder.toString();
        }
        MediaType mediaType = responseBody.contentType();
        if (mediaType != null) {
            builder.append("\nContent-Type: ").append(mediaType);
        }
        ResponseBody previewBody = response.peekBody(MAX_PREVIEW_BYTES);
        String preview = responseBodyPreview(previewBody);
        if (!TextUtils.isEmpty(preview)) {
            builder.append("\nBody: ").append(preview);
        }
        return builder.toString();
    }

    private long safeContentLength(@NonNull RequestBody body) {
        try {
            return body.contentLength();
        } catch (IOException exception) {
            return -1L;
        }
    }

    @Nullable
    private String requestBodyPreview(@NonNull RequestBody body) {
        MediaType mediaType = body.contentType();
        if (!isPlainText(mediaType)) {
            return buildBinaryBodyHint(mediaType, safeContentLength(body));
        }
        try {
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            Charset charset = resolveCharset(mediaType);
            return truncate(buffer.readString(charset));
        } catch (IOException exception) {
            DLog.w(TAG, "读取请求体预览失败", exception);
            return "读取请求体失败: " + safeMessage(exception);
        }
    }

    @Nullable
    private String responseBodyPreview(@NonNull ResponseBody body) throws IOException {
        MediaType mediaType = body.contentType();
        if (!isPlainText(mediaType)) {
            return buildBinaryBodyHint(mediaType, body.contentLength());
        }
        return truncate(body.string());
    }

    private boolean isPlainText(@Nullable MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        if ("text".equalsIgnoreCase(mediaType.type())) {
            return true;
        }
        String subtype = mediaType.subtype();
        if (subtype == null) {
            return false;
        }
        String safeSubtype = subtype.toLowerCase();
        return safeSubtype.contains("json")
                || safeSubtype.contains("xml")
                || safeSubtype.contains("html")
                || safeSubtype.contains("x-www-form-urlencoded")
                || safeSubtype.contains("plain");
    }

    @Nullable
    private String buildBinaryBodyHint(@Nullable MediaType mediaType, long contentLength) {
        if (mediaType == null && contentLength < 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder("二进制/复合内容");
        if (mediaType != null) {
            builder.append(" type=").append(mediaType);
        }
        if (contentLength >= 0) {
            builder.append(", length=").append(contentLength);
        }
        return builder.toString();
    }

    @NonNull
    private Charset resolveCharset(@Nullable MediaType mediaType) {
        if (mediaType == null) {
            return StandardCharsets.UTF_8;
        }
        Charset charset = mediaType.charset(StandardCharsets.UTF_8);
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    @NonNull
    private String truncate(@NonNull String source) {
        if (source.length() <= MAX_PREVIEW_BYTES) {
            return source;
        }
        return source.substring(0, (int) MAX_PREVIEW_BYTES) + "...(truncated)";
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage()) ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
