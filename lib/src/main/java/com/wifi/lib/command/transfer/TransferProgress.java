package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;

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
        this.sessionId = sessionId == null ? "" : sessionId.trim();
        this.totalBytes = Math.max(0L, totalBytes);
        this.transferredBytes = Math.max(0L, transferredBytes);
        this.totalChunks = Math.max(0, totalChunks);
        this.transferredChunks = Math.max(0, transferredChunks);
        this.state = state == null ? TransferSessionState.IDLE : state;
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
        if (totalBytes <= 0L) {
            return state == TransferSessionState.COMPLETED ? 100 : 0;
        }
        return (int) Math.min(100L, (transferredBytes * 100L) / totalBytes);
    }

    public boolean isCompleted() {
        return state == TransferSessionState.COMPLETED;
    }
}
