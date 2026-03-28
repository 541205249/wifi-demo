package com.wifi.lib.command.ack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AckFactory {
    private AckFactory() {
    }

    @NonNull
    public static AckMessage successForCommand(@NonNull String reference, @Nullable String message) {
        return base(AckStatus.SUCCESS, AckChannel.COMMAND, reference, null, null, message).build();
    }

    @NonNull
    public static AckMessage failureForCommand(
            @NonNull String reference,
            @NonNull String errorCode,
            @Nullable String message
    ) {
        return base(AckStatus.FAILURE, AckChannel.COMMAND, reference, null, errorCode, message).build();
    }

    @NonNull
    public static AckMessage successForTransfer(
            @NonNull String reference,
            @Nullable String sessionId,
            @Nullable String message
    ) {
        return base(AckStatus.SUCCESS, AckChannel.TRANSFER, reference, sessionId, null, message).build();
    }

    @NonNull
    public static AckMessage failureForTransfer(
            @NonNull String reference,
            @Nullable String sessionId,
            @NonNull String errorCode,
            @Nullable String message
    ) {
        return base(AckStatus.FAILURE, AckChannel.TRANSFER, reference, sessionId, errorCode, message).build();
    }

    @NonNull
    public static AckMessage successForStream(
            @NonNull String reference,
            @Nullable String sessionId,
            @Nullable String message
    ) {
        return base(AckStatus.SUCCESS, AckChannel.STREAM, reference, sessionId, null, message).build();
    }

    @NonNull
    public static AckMessage failureForStream(
            @NonNull String reference,
            @Nullable String sessionId,
            @NonNull String errorCode,
            @Nullable String message
    ) {
        return base(AckStatus.FAILURE, AckChannel.STREAM, reference, sessionId, errorCode, message).build();
    }

    @NonNull
    public static AckMessage.Builder base(
            @NonNull AckStatus status,
            @NonNull AckChannel channel,
            @NonNull String reference,
            @Nullable String sessionId,
            @Nullable String errorCode,
            @Nullable String message
    ) {
        return new AckMessage.Builder()
                .setStatus(status)
                .setChannel(channel)
                .setReference(reference)
                .setSessionId(sessionId)
                .setErrorCode(errorCode)
                .setMessage(message);
    }
}
