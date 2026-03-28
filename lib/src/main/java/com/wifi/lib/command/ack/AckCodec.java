package com.wifi.lib.command.ack;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AckCodec {
    private static final String TAG = "AckCodec";
    private static final String EXTRA_PREFIX = "EXT_";

    @NonNull
    public String encode(@NonNull AckMessage ackMessage) {
        StringBuilder builder = new StringBuilder(96);
        builder.append(ackMessage.getStatus().getPrefix());
        appendField(builder, "TYPE", ackMessage.getChannel().getWireValue());
        appendField(builder, "REF", ackMessage.getReference());
        if (ackMessage.hasSessionId()) {
            appendField(builder, "SESSION", ackMessage.getSessionId());
        }
        appendField(builder, "TS", String.valueOf(ackMessage.getTimestampMs()));
        if (ackMessage.isFailure()) {
            appendField(builder, "CODE", ackMessage.getErrorCode());
        }
        if (!ackMessage.getMessage().isEmpty()) {
            appendField(builder, "MSG", ackMessage.getMessage());
        }
        for (Map.Entry<String, String> entry : ackMessage.getExtras().entrySet()) {
            appendField(builder, EXTRA_PREFIX + normalizeExtraKey(entry.getKey()), entry.getValue());
        }
        String raw = builder.toString();
        DLog.i(TAG, "ACK 已编码，status=" + ackMessage.getStatus() + ", channel=" + ackMessage.getChannel());
        return raw;
    }

    @NonNull
    public AckMessage decode(@NonNull String rawMessage) {
        AckStatus status = AckStatus.fromRawMessage(rawMessage);
        String body = rawMessage.substring(status.getPrefix().length());
        String[] segments = body.split(",");
        Map<String, String> values = new LinkedHashMap<>();
        for (String segment : segments) {
            int separatorIndex = segment.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = segment.substring(0, separatorIndex).trim();
            String value = segment.substring(separatorIndex + 1).trim();
            values.put(key, Uri.decode(value));
        }

        AckMessage.Builder builder = new AckMessage.Builder()
                .setStatus(status)
                .setChannel(AckChannel.fromWireValue(require(values, "TYPE")))
                .setReference(require(values, "REF"))
                .setSessionId(values.get("SESSION"))
                .setTimestampMs(parseLong(require(values, "TS"), "TS"))
                .setErrorCode(values.get("CODE"))
                .setMessage(values.get("MSG"));

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().startsWith(EXTRA_PREFIX)) {
                builder.putExtra(entry.getKey().substring(EXTRA_PREFIX.length()), entry.getValue());
            }
        }
        AckMessage ackMessage = builder.build();
        DLog.i(TAG, "ACK 已解码，status=" + ackMessage.getStatus() + ", channel=" + ackMessage.getChannel());
        return ackMessage;
    }

    private void appendField(@NonNull StringBuilder builder, @NonNull String key, @NonNull String value) {
        if (builder.charAt(builder.length() - 1) != '+') {
            builder.append(',');
        }
        builder.append(key).append('=').append(Uri.encode(value));
    }

    @NonNull
    private String require(@NonNull Map<String, String> values, @NonNull String key) {
        String value = values.get(key);
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException("ACK 缺少字段: " + key);
        }
        return value;
    }

    private long parseLong(@NonNull String rawValue, @NonNull String key) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ACK 字段不是长整数: " + key + "=" + rawValue, exception);
        }
    }

    @NonNull
    private String normalizeExtraKey(@NonNull String key) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < key.length(); index++) {
            char current = key.charAt(index);
            if (Character.isLetterOrDigit(current) || current == '_') {
                builder.append(current);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }
}
