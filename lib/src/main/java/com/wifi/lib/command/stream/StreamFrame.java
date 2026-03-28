package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.transfer.TransferChecksums;

import java.util.Arrays;

public final class StreamFrame {
    @NonNull
    private final String sessionId;
    private final int sequence;
    private final long timestampMs;
    @NonNull
    private final byte[] payload;
    @NonNull
    private final String crc32;
    private final boolean endOfStream;

    public StreamFrame(
            @NonNull String sessionId,
            int sequence,
            long timestampMs,
            @NonNull byte[] payload,
            @Nullable String crc32,
            boolean endOfStream
    ) {
        this.sessionId = sessionId == null ? "" : sessionId.trim();
        this.sequence = sequence;
        this.timestampMs = Math.max(0L, timestampMs);
        this.payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        this.crc32 = crc32 == null || crc32.trim().isEmpty()
                ? TransferChecksums.crc32Hex(this.payload)
                : crc32.trim();
        this.endOfStream = endOfStream;

        if (this.sessionId.isEmpty()) {
            throw new IllegalArgumentException("stream frame sessionId 不能为空");
        }
        if (this.sequence <= 0) {
            throw new IllegalArgumentException("stream frame sequence 必须从 1 开始");
        }
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public int getSequence() {
        return sequence;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    @NonNull
    public byte[] getPayload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public int getPayloadSize() {
        return payload.length;
    }

    @NonNull
    public String getCrc32() {
        return crc32;
    }

    public boolean isEndOfStream() {
        return endOfStream;
    }

    public boolean isCrcValid() {
        return crc32.equalsIgnoreCase(TransferChecksums.crc32Hex(payload));
    }
}
