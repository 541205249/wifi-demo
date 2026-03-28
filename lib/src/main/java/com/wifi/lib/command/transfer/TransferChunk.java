package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Objects;

public final class TransferChunk {
    @NonNull
    private final String sessionId;
    private final int index;
    private final int totalChunks;
    private final long offset;
    @NonNull
    private final byte[] payload;
    @NonNull
    private final String crc32;

    public TransferChunk(
            @NonNull String sessionId,
            int index,
            int totalChunks,
            long offset,
            @NonNull byte[] payload,
            @Nullable String crc32
    ) {
        this.sessionId = sessionId == null ? "" : sessionId.trim();
        this.index = index;
        this.totalChunks = totalChunks;
        this.offset = offset;
        this.payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        this.crc32 = crc32 == null || crc32.trim().isEmpty()
                ? TransferChecksums.crc32Hex(this.payload)
                : crc32.trim();

        if (this.sessionId.isEmpty()) {
            throw new IllegalArgumentException("transfer chunk sessionId 不能为空");
        }
        if (this.index <= 0) {
            throw new IllegalArgumentException("transfer chunk index 必须从 1 开始");
        }
        if (this.totalChunks <= 0) {
            throw new IllegalArgumentException("transfer chunk totalChunks 必须大于 0");
        }
        if (this.index > this.totalChunks) {
            throw new IllegalArgumentException("transfer chunk index 不能超过 totalChunks");
        }
        if (this.offset < 0L) {
            throw new IllegalArgumentException("transfer chunk offset 不能小于 0");
        }
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public int getIndex() {
        return index;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public long getOffset() {
        return offset;
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

    public boolean isCrcValid() {
        return crc32.equalsIgnoreCase(TransferChecksums.crc32Hex(payload));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TransferChunk)) {
            return false;
        }
        TransferChunk other = (TransferChunk) object;
        return index == other.index && Objects.equals(sessionId, other.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, index);
    }
}
