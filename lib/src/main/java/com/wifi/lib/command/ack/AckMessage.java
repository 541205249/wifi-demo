package com.wifi.lib.command.ack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AckMessage {
    @NonNull
    private final AckStatus status;
    @NonNull
    private final AckChannel channel;
    @NonNull
    private final String reference;
    @NonNull
    private final String sessionId;
    @NonNull
    private final String errorCode;
    @NonNull
    private final String message;
    private final long timestampMs;
    @NonNull
    private final Map<String, String> extras;

    private AckMessage(@NonNull Builder builder) {
        this.status = builder.status;
        this.channel = builder.channel;
        this.reference = builder.reference;
        this.sessionId = builder.sessionId;
        this.errorCode = builder.errorCode;
        this.message = builder.message;
        this.timestampMs = builder.timestampMs > 0L ? builder.timestampMs : System.currentTimeMillis();
        this.extras = Collections.unmodifiableMap(new LinkedHashMap<>(builder.extras));
    }

    @NonNull
    public AckStatus getStatus() {
        return status;
    }

    @NonNull
    public AckChannel getChannel() {
        return channel;
    }

    @NonNull
    public String getReference() {
        return reference;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @NonNull
    public String getErrorCode() {
        return errorCode;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    @NonNull
    public Map<String, String> getExtras() {
        return extras;
    }

    public boolean isSuccess() {
        return status == AckStatus.SUCCESS;
    }

    public boolean isFailure() {
        return status == AckStatus.FAILURE;
    }

    public boolean hasSessionId() {
        return !sessionId.isEmpty();
    }

    public boolean hasErrorCode() {
        return !errorCode.isEmpty();
    }

    public static final class Builder {
        @Nullable
        private AckStatus status;
        @Nullable
        private AckChannel channel;
        @NonNull
        private String reference = "";
        @NonNull
        private String sessionId = "";
        @NonNull
        private String errorCode = "";
        @NonNull
        private String message = "";
        private long timestampMs;
        @NonNull
        private final Map<String, String> extras = new LinkedHashMap<>();

        @NonNull
        public Builder setStatus(@Nullable AckStatus status) {
            this.status = status;
            return this;
        }

        @NonNull
        public Builder setChannel(@Nullable AckChannel channel) {
            this.channel = channel;
            return this;
        }

        @NonNull
        public Builder setReference(@Nullable String reference) {
            this.reference = normalize(reference);
            return this;
        }

        @NonNull
        public Builder setSessionId(@Nullable String sessionId) {
            this.sessionId = normalize(sessionId);
            return this;
        }

        @NonNull
        public Builder setErrorCode(@Nullable String errorCode) {
            this.errorCode = normalize(errorCode);
            return this;
        }

        @NonNull
        public Builder setMessage(@Nullable String message) {
            this.message = normalize(message);
            return this;
        }

        @NonNull
        public Builder setTimestampMs(long timestampMs) {
            this.timestampMs = Math.max(0L, timestampMs);
            return this;
        }

        @NonNull
        public Builder putExtra(@Nullable String key, @Nullable String value) {
            String normalizedKey = normalize(key);
            if (normalizedKey.isEmpty()) {
                return this;
            }
            extras.put(normalizedKey, normalize(value));
            return this;
        }

        @NonNull
        public AckMessage build() {
            if (status == null) {
                throw new IllegalArgumentException("ACK status 不能为空");
            }
            if (channel == null) {
                throw new IllegalArgumentException("ACK channel 不能为空");
            }
            if (reference.isEmpty()) {
                throw new IllegalArgumentException("ACK reference 不能为空");
            }
            if (status == AckStatus.FAILURE && errorCode.isEmpty()) {
                throw new IllegalArgumentException("失败 ACK 必须带 errorCode");
            }
            return new AckMessage(this);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AckMessage)) {
            return false;
        }
        AckMessage other = (AckMessage) object;
        return Objects.equals(status, other.status)
                && Objects.equals(channel, other.channel)
                && Objects.equals(reference, other.reference)
                && Objects.equals(sessionId, other.sessionId)
                && Objects.equals(errorCode, other.errorCode)
                && Objects.equals(message, other.message)
                && Objects.equals(extras, other.extras);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, channel, reference, sessionId, errorCode, message, extras);
    }

    @NonNull
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
