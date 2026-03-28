package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.gateway.ProtocolPayloadType;

public class ProtocolDispatchContext {
    @NonNull
    private final String clientId;
    @NonNull
    private final ProtocolInboundEvent event;
    private final long dispatchedAtMillis;

    ProtocolDispatchContext(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        this.clientId = clientId;
        this.event = event;
        this.dispatchedAtMillis = dispatchedAtMillis;
    }

    @NonNull
    public String getClientId() {
        return clientId;
    }

    @NonNull
    public ProtocolInboundEvent getEvent() {
        return event;
    }

    @NonNull
    public ProtocolPayloadType getPayloadType() {
        return event.getPayloadType();
    }

    @NonNull
    public String getRawMessage() {
        return event.getRawMessage();
    }

    public long getDispatchedAtMillis() {
        return dispatchedAtMillis;
    }
}
