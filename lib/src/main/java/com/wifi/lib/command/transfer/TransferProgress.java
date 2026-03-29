package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class TransferProgress {
    @NonNull
    private final String sessionId;
    private final long totalBytes;
    private final long transferredBytes;
    private final int totalChunks;
    private final int transferredChunks;
    @NonNull
    private final TransferSessionState state;

    public TransferProgress(
            @NonNull String sessionId,
            long totalBytes,
            long transferredBytes,
            int totalChunks,
            int transferredChunks,
            @NonNull TransferSessionState state
    ) {
        this.sessionId = normalizeSessionId(sessionId);
        this.totalBytes = Math.max(0L, totalBytes);
        this.transferredBytes = Math.max(0L, transferredBytes);
        this.totalChunks = Math.max(0, totalChunks);
        this.transferredChunks = Math.max(0, transferredChunks);
        this.state = resolveState(state);
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getTransferredBytes() {
        return transferredBytes;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getTransferredChunks() {
        return transferredChunks;
    }

    @NonNull
    public TransferSessionState getState() {
        return state;
    }

    public int getPercent() {
        return resolvePercent(totalBytes, transferredBytes, state);
    }

    public boolean isCompleted() {
        return state == TransferSessionState.COMPLETED;
    }

    @NonNull
    private static String normalizeSessionId(@Nullable String sessionId) {
        return sessionId == null ? "" : sessionId.trim();
    }

    @NonNull
    private static TransferSessionState resolveState(@Nullable TransferSessionState state) {
        return state == null ? TransferSessionState.IDLE : state;
    }

    private static int resolvePercent(
            long totalBytes,
            long transferredBytes,
            @NonNull TransferSessionState state
    ) {
        if (totalBytes <= 0L) {
            return state == TransferSessionState.COMPLETED ? 100 : 0;
        }
        return (int) Math.min(100L, (transferredBytes * 100L) / totalBytes);
    }
}
