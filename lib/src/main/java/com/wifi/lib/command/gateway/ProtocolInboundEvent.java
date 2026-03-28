package com.wifi.lib.command.gateway;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.InboundCommand;
import com.wifi.lib.command.ack.AckMessage;
import com.wifi.lib.command.stream.StreamFrame;
import com.wifi.lib.command.transfer.TransferChunk;

public final class ProtocolInboundEvent {
    @NonNull
    private final ProtocolPayloadType payloadType;
    @NonNull
    private final String rawMessage;
    @Nullable
    private final InboundCommand inboundCommand;
    @Nullable
    private final AckMessage ackMessage;
    @Nullable
    private final TransferChunk transferChunk;
    @Nullable
    private final StreamFrame streamFrame;
    @NonNull
    private final String errorMessage;

    private ProtocolInboundEvent(
            @NonNull ProtocolPayloadType payloadType,
            @NonNull String rawMessage,
            @Nullable InboundCommand inboundCommand,
            @Nullable AckMessage ackMessage,
            @Nullable TransferChunk transferChunk,
            @Nullable StreamFrame streamFrame,
            @Nullable String errorMessage
    ) {
        this.payloadType = payloadType;
        this.rawMessage = rawMessage == null ? "" : rawMessage;
        this.inboundCommand = inboundCommand;
        this.ackMessage = ackMessage;
        this.transferChunk = transferChunk;
        this.streamFrame = streamFrame;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    @NonNull
    public static ProtocolInboundEvent command(@NonNull String rawMessage, @NonNull InboundCommand inboundCommand) {
        return new ProtocolInboundEvent(ProtocolPayloadType.COMMAND, rawMessage, inboundCommand, null, null, null, null);
    }

    @NonNull
    public static ProtocolInboundEvent ack(@NonNull String rawMessage, @NonNull AckMessage ackMessage) {
        return new ProtocolInboundEvent(ProtocolPayloadType.ACK, rawMessage, null, ackMessage, null, null, null);
    }

    @NonNull
    public static ProtocolInboundEvent transfer(@NonNull String rawMessage, @NonNull TransferChunk transferChunk) {
        return new ProtocolInboundEvent(ProtocolPayloadType.TRANSFER, rawMessage, null, null, transferChunk, null, null);
    }

    @NonNull
    public static ProtocolInboundEvent stream(@NonNull String rawMessage, @NonNull StreamFrame streamFrame) {
        return new ProtocolInboundEvent(ProtocolPayloadType.STREAM, rawMessage, null, null, null, streamFrame, null);
    }

    @NonNull
    public static ProtocolInboundEvent unknown(@NonNull String rawMessage) {
        return new ProtocolInboundEvent(ProtocolPayloadType.UNKNOWN, rawMessage, null, null, null, null, null);
    }

    @NonNull
    public static ProtocolInboundEvent invalid(@NonNull String rawMessage, @Nullable String errorMessage) {
        return new ProtocolInboundEvent(ProtocolPayloadType.INVALID, rawMessage, null, null, null, null, errorMessage);
    }

    @NonNull
    public ProtocolPayloadType getPayloadType() {
        return payloadType;
    }

    @NonNull
    public String getRawMessage() {
        return rawMessage;
    }

    @Nullable
    public InboundCommand getInboundCommand() {
        return inboundCommand;
    }

    @Nullable
    public AckMessage getAckMessage() {
        return ackMessage;
    }

    @Nullable
    public TransferChunk getTransferChunk() {
        return transferChunk;
    }

    @Nullable
    public StreamFrame getStreamFrame() {
        return streamFrame;
    }

    @NonNull
    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isStructured() {
        return payloadType == ProtocolPayloadType.COMMAND
                || payloadType == ProtocolPayloadType.ACK
                || payloadType == ProtocolPayloadType.TRANSFER
                || payloadType == ProtocolPayloadType.STREAM;
    }

    public boolean isInvalid() {
        return payloadType == ProtocolPayloadType.INVALID;
    }
}
