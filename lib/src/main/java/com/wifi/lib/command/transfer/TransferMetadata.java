package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TransferMetadata {
    public static final int DEFAULT_CHUNK_SIZE = 2 * 1024;

    @NonNull
    private final String sessionId;
    @NonNull
    private final TransferDirection direction;
    @NonNull
    private final String fileName;
    @NonNull
    private final String mediaType;
    private final long totalBytes;
    private final int chunkSize;
    @NonNull
    private final String md5;
    @NonNull
    private final Map<String, String> extras;

    private TransferMetadata(@NonNull Builder builder) {
        this.sessionId = builder.sessionId;
        this.direction = builder.direction;
        this.fileName = builder.fileName;
        this.mediaType = builder.mediaType;
        this.totalBytes = builder.totalBytes;
        this.chunkSize = builder.chunkSize;
        this.md5 = builder.md5;
        this.extras = Collections.unmodifiableMap(new LinkedHashMap<>(builder.extras));
    }

    @NonNull
    public static String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @NonNull
    public static TransferMetadata fromFile(
            @NonNull String sessionId,
            @NonNull TransferDirection direction,
            @NonNull File file,
            @Nullable String mediaType,
            int chunkSize
    ) throws IOException {
        return new Builder()
                .setSessionId(sessionId)
                .setDirection(direction)
                .setFileName(file.getName())
                .setMediaType(mediaType)
                .setTotalBytes(file.length())
                .setChunkSize(chunkSize)
                .setMd5(TransferChecksums.md5Hex(file))
                .build();
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @NonNull
    public TransferDirection getDirection() {
        return direction;
    }

    @NonNull
    public String getFileName() {
        return fileName;
    }

    @NonNull
    public String getMediaType() {
        return mediaType;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    @NonNull
    public String getMd5() {
        return md5;
    }

    @NonNull
    public Map<String, String> getExtras() {
        return extras;
    }

    public int getTotalChunks() {
        if (totalBytes <= 0L) {
            return 0;
        }
        return (int) ((totalBytes + chunkSize - 1) / chunkSize);
    }

    public static final class Builder {
        @NonNull
        private String sessionId = "";
        @Nullable
        private TransferDirection direction;
        @NonNull
        private String fileName = "";
        @NonNull
        private String mediaType = "application/octet-stream";
        private long totalBytes;
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        @NonNull
        private String md5 = "";
        @NonNull
        private final Map<String, String> extras = new LinkedHashMap<>();

        @NonNull
        public Builder setSessionId(@Nullable String sessionId) {
            this.sessionId = sessionId == null ? "" : sessionId.trim();
            return this;
        }

        @NonNull
        public Builder setDirection(@Nullable TransferDirection direction) {
            this.direction = direction;
            return this;
        }

        @NonNull
        public Builder setFileName(@Nullable String fileName) {
            this.fileName = fileName == null ? "" : fileName.trim();
            return this;
        }

        @NonNull
        public Builder setMediaType(@Nullable String mediaType) {
            this.mediaType = mediaType == null || mediaType.trim().isEmpty()
                    ? "application/octet-stream"
                    : mediaType.trim();
            return this;
        }

        @NonNull
        public Builder setTotalBytes(long totalBytes) {
            this.totalBytes = Math.max(0L, totalBytes);
            return this;
        }

        @NonNull
        public Builder setChunkSize(int chunkSize) {
            this.chunkSize = Math.max(1, chunkSize);
            return this;
        }

        @NonNull
        public Builder setMd5(@Nullable String md5) {
            this.md5 = md5 == null ? "" : md5.trim();
            return this;
        }

        @NonNull
        public Builder putExtra(@Nullable String key, @Nullable String value) {
            if (key == null || key.trim().isEmpty()) {
                return this;
            }
            extras.put(key.trim(), value == null ? "" : value.trim());
            return this;
        }

        @NonNull
        public TransferMetadata build() {
            if (sessionId.isEmpty()) {
                throw new IllegalArgumentException("transfer sessionId 不能为空");
            }
            if (direction == null) {
                throw new IllegalArgumentException("transfer direction 不能为空");
            }
            if (fileName.isEmpty()) {
                throw new IllegalArgumentException("transfer fileName 不能为空");
            }
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("transfer chunkSize 必须大于 0");
            }
            return new TransferMetadata(this);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TransferMetadata)) {
            return false;
        }
        TransferMetadata other = (TransferMetadata) object;
        return Objects.equals(sessionId, other.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}
