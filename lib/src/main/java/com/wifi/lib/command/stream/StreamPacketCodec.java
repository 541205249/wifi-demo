package com.wifi.lib.command.stream;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StreamPacketCodec {
    private static final String PREFIX = "SF+";

    @NonNull
    public String encodeFrame(@NonNull StreamFrame frame) {
        String base64Payload = Base64.encodeToString(frame.getPayload(), Base64.NO_WRAP);
        StringBuilder builder = new StringBuilder(PREFIX.length() + base64Payload.length() + 96);
        builder.append(PREFIX)
                .append("SID=").append(frame.getSessionId())
                .append(",SEQ=").append(frame.getSequence())
                .append(",TS=").append(frame.getTimestampMs())
                .append(",SIZE=").append(frame.getPayloadSize())
                .append(",EOS=").append(frame.isEndOfStream() ? 1 : 0)
                .append(",CRC=").append(frame.getCrc32())
                .append(",DATA=").append(base64Payload);
        return builder.toString();
    }

    @NonNull
    public StreamFrame decodeFrame(@NonNull String frame) {
        if (frame == null || !frame.startsWith(PREFIX)) {
            throw new IllegalArgumentException("非法 stream frame: " + frame);
        }
        String body = frame.substring(PREFIX.length());
        String[] segments = body.split(",");
        Map<String, String> values = new LinkedHashMap<>();
        for (String segment : segments) {
            int separatorIndex = segment.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = segment.substring(0, separatorIndex).trim();
            String value = segment.substring(separatorIndex + 1).trim();
            values.put(key, value);
        }

        String sessionId = require(values, "SID");
        int sequence = parseInt(require(values, "SEQ"), "SEQ");
        long timestampMs = parseLong(require(values, "TS"), "TS");
        int size = parseInt(require(values, "SIZE"), "SIZE");
        boolean eos = parseInt(require(values, "EOS"), "EOS") == 1;
        String crc = require(values, "CRC");
        String data = require(values, "DATA");
        byte[] payload = TextUtils.isEmpty(data) ? new byte[0] : Base64.decode(data, Base64.NO_WRAP);
        if (payload.length != size) {
            throw new IllegalArgumentException("stream frame SIZE 与实际 payload 长度不一致");
        }
        return new StreamFrame(sessionId, sequence, timestampMs, payload, crc, eos);
    }

    @NonNull
    private String require(@NonNull Map<String, String> values, @NonNull String key) {
        String value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("stream frame 缺少字段: " + key);
        }
        return value;
    }

    private int parseInt(@NonNull String rawValue, @NonNull String key) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("stream frame 字段不是整数: " + key + "=" + rawValue, exception);
        }
    }

    private long parseLong(@NonNull String rawValue, @NonNull String key) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("stream frame 字段不是长整数: " + key + "=" + rawValue, exception);
        }
    }
}
