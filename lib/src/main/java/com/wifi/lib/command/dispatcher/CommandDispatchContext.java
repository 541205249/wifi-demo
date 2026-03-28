package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

import com.wifi.lib.command.InboundCommand;
import com.wifi.lib.command.gateway.ProtocolInboundEvent;

public final class CommandDispatchContext extends ProtocolDispatchContext {
    @NonNull
    private final InboundCommand inboundCommand;

    CommandDispatchContext(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            @NonNull InboundCommand inboundCommand,
            long dispatchedAtMillis
    ) {
        super(clientId, event, dispatchedAtMillis);
        this.inboundCommand = inboundCommand;
    }

    @NonNull
    public InboundCommand getInboundCommand() {
        return inboundCommand;
    }

    @NonNull
    public String getCode() {
        return inboundCommand.getCode();
    }
}
