package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class StreamMetadata {
    public static final int DEFAULT_FRAME_SIZE = 256;

    @NonNull
    private final String sessionId;
    @NonNull
    private final StreamDirection direction;
    @NonNull
    private final String streamType;
    private final int sampleRateHz;
    private final int frameSize;
    private final boolean checksumEnabled;
    @NonNull
    private final Map<String, String> extras;

    private StreamMetadata(@NonNull Builder builder) {
        this.sessionId = builder.sessionId;
        this.direction = builder.direction;
        this.streamType = builder.streamType;
        this.sampleRateHz = builder.sampleRateHz;
        this.frameSize = builder.frameSize;
        this.checksumEnabled = builder.checksumEnabled;
        this.extras = Collections.unmodifiableMap(new LinkedHashMap<>(builder.extras));
    }

    @NonNull
    public static String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @NonNull
    public StreamDirection getDirection() {
        return direction;
    }

    @NonNull
    public String getStreamType() {
        return streamType;
    }

    public int getSampleRateHz() {
        return sampleRateHz;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public boolean isChecksumEnabled() {
        return checksumEnabled;
    }

    @NonNull
    public Map<String, String> getExtras() {
        return extras;
    }

    public static final class Builder {
        @NonNull
        private String sessionId = "";
        @Nullable
        private StreamDirection direction;
        @NonNull
        private String streamType = "";
        private int sampleRateHz;
        private int frameSize = DEFAULT_FRAME_SIZE;
        private boolean checksumEnabled = true;
        @NonNull
        private final Map<String, String> extras = new LinkedHashMap<>();

        @NonNull
        public Builder setSessionId(@Nullable String sessionId) {
            this.sessionId = sessionId == null ? "" : sessionId.trim();
            return this;
        }

        @NonNull
        public Builder setDirection(@Nullable StreamDirection direction) {
            this.direction = direction;
            return this;
        }

        @NonNull
        public Builder setStreamType(@Nullable String streamType) {
            this.streamType = streamType == null ? "" : streamType.trim();
            return this;
        }

        @NonNull
        public Builder setSampleRateHz(int sampleRateHz) {
            this.sampleRateHz = Math.max(0, sampleRateHz);
            return this;
        }

        @NonNull
        public Builder setFrameSize(int frameSize) {
            this.frameSize = Math.max(1, frameSize);
            return this;
        }

        @NonNull
        public Builder setChecksumEnabled(boolean checksumEnabled) {
            this.checksumEnabled = checksumEnabled;
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
        public StreamMetadata build() {
            if (sessionId.isEmpty()) {
                throw new IllegalArgumentException("stream sessionId 不能为空");
            }
            if (direction == null) {
                throw new IllegalArgumentException("stream direction 不能为空");
            }
            if (streamType.isEmpty()) {
                throw new IllegalArgumentException("streamType 不能为空");
            }
            if (frameSize <= 0) {
                throw new IllegalArgumentException("frameSize 必须大于 0");
            }
            return new StreamMetadata(this);
        }
    }
}
