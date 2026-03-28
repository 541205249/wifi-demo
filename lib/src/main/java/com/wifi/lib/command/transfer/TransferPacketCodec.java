package com.wifi.lib.command.transfer;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文本帧格式：
 * TF+SID=<sessionId>,IDX=<index>,TOTAL=<total>,OFFSET=<offset>,SIZE=<size>,CRC=<crc32>,DATA=<base64>
 */
public final class TransferPacketCodec {
    private static final String PREFIX = "TF+";

    @NonNull
    public String encodeChunk(@NonNull TransferChunk chunk) {
        String base64Payload = Base64.encodeToString(chunk.getPayload(), Base64.NO_WRAP);
        StringBuilder builder = new StringBuilder(PREFIX.length() + base64Payload.length() + 128);
        builder.append(PREFIX)
                .append("SID=").append(chunk.getSessionId())
                .append(",IDX=").append(chunk.getIndex())
                .append(",TOTAL=").append(chunk.getTotalChunks())
                .append(",OFFSET=").append(chunk.getOffset())
                .append(",SIZE=").append(chunk.getPayloadSize())
                .append(",CRC=").append(chunk.getCrc32())
                .append(",DATA=").append(base64Payload);
        return builder.toString();
    }

    @NonNull
    public TransferChunk decodeChunk(@NonNull String frame) {
        if (frame == null || !frame.startsWith(PREFIX)) {
            throw new IllegalArgumentException("非法 transfer frame: " + frame);
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
        int index = parseInt(require(values, "IDX"), "IDX");
        int total = parseInt(require(values, "TOTAL"), "TOTAL");
        long offset = parseLong(require(values, "OFFSET"), "OFFSET");
        int size = parseInt(require(values, "SIZE"), "SIZE");
        String crc = require(values, "CRC");
        String data = require(values, "DATA");
        byte[] payload = TextUtils.isEmpty(data) ? new byte[0] : Base64.decode(data, Base64.NO_WRAP);
        if (payload.length != size) {
            throw new IllegalArgumentException("transfer frame SIZE 与实际 payload 长度不一致");
        }
        return new TransferChunk(sessionId, index, total, offset, payload, crc);
    }

    @NonNull
    private String require(@NonNull Map<String, String> values, @NonNull String key) {
        String value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("transfer frame 缺少字段: " + key);
        }
        return value;
    }

    private int parseInt(@NonNull String rawValue, @NonNull String key) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("transfer frame 字段不是整数: " + key + "=" + rawValue, exception);
        }
    }

    private long parseLong(@NonNull String rawValue, @NonNull String key) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("transfer frame 字段不是长整数: " + key + "=" + rawValue, exception);
        }
    }
}
