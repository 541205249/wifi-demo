package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

import com.wifi.lib.command.ack.AckMessage;
import com.wifi.lib.command.gateway.ProtocolInboundEvent;

public final class AckDispatchContext extends ProtocolDispatchContext {
    @NonNull
    private final AckMessage ackMessage;

    AckDispatchContext(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            @NonNull AckMessage ackMessage,
            long dispatchedAtMillis
    ) {
        super(clientId, event, dispatchedAtMillis);
        this.ackMessage = ackMessage;
    }

    @NonNull
    public AckMessage getAckMessage() {
        return ackMessage;
    }
}
