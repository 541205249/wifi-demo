package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.transfer.TransferChunk;
import com.wifi.lib.command.transfer.TransferMetadata;
import com.wifi.lib.command.transfer.TransferProgress;

import java.util.Arrays;

public final class TransferDispatchContext extends ProtocolDispatchContext {
    @NonNull
    private final TransferMetadata metadata;
    @NonNull
    private final TransferChunk transferChunk;
    @NonNull
    private final TransferProgress progress;
    @Nullable
    private final byte[] completedPayload;

    TransferDispatchContext(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            @NonNull TransferMetadata metadata,
            @NonNull TransferChunk transferChunk,
            @NonNull TransferProgress progress,
            @Nullable byte[] completedPayload,
            long dispatchedAtMillis
    ) {
        super(clientId, event, dispatchedAtMillis);
        this.metadata = metadata;
        this.transferChunk = transferChunk;
        this.progress = progress;
        this.completedPayload = completedPayload == null ? null : Arrays.copyOf(completedPayload, completedPayload.length);
    }

    @NonNull
    public TransferMetadata getMetadata() {
        return metadata;
    }

    @NonNull
    public TransferChunk getTransferChunk() {
        return transferChunk;
    }

    @NonNull
    public TransferProgress getProgress() {
        return progress;
    }

    public boolean hasCompletedPayload() {
        return completedPayload != null && completedPayload.length > 0;
    }

    @Nullable
    public byte[] getCompletedPayload() {
        return completedPayload == null ? null : Arrays.copyOf(completedPayload, completedPayload.length);
    }
}
