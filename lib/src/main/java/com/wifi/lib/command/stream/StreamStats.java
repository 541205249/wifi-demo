package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class StreamStats {
    @NonNull
    private final String sessionId;
    private final int totalFrames;
    private final long totalBytes;
    private final int lastSequence;
    private final int droppedFrames;
    private final long lastTimestampMs;
    @NonNull
    private final StreamSessionState state;

    public StreamStats(
            @NonNull String sessionId,
            int totalFrames,
            long totalBytes,
            int lastSequence,
            int droppedFrames,
            long lastTimestampMs,
            @NonNull StreamSessionState state
    ) {
        this.sessionId = normalizeSessionId(sessionId);
        this.totalFrames = Math.max(0, totalFrames);
        this.totalBytes = Math.max(0L, totalBytes);
        this.lastSequence = Math.max(0, lastSequence);
        this.droppedFrames = Math.max(0, droppedFrames);
        this.lastTimestampMs = Math.max(0L, lastTimestampMs);
        this.state = resolveState(state);
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public int getTotalFrames() {
        return totalFrames;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public int getLastSequence() {
        return lastSequence;
    }

    public int getDroppedFrames() {
        return droppedFrames;
    }

    public long getLastTimestampMs() {
        return lastTimestampMs;
    }

    @NonNull
    public StreamSessionState getState() {
        return state;
    }

    @NonNull
    private static String normalizeSessionId(@Nullable String sessionId) {
        return sessionId == null ? "" : sessionId.trim();
    }

    @NonNull
    private static StreamSessionState resolveState(@Nullable StreamSessionState state) {
        return state == null ? StreamSessionState.IDLE : state;
    }
}
