package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;

import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.stream.StreamFrame;
import com.wifi.lib.command.stream.StreamMetadata;
import com.wifi.lib.command.stream.StreamStats;

public final class StreamDispatchContext extends ProtocolDispatchContext {
    @NonNull
    private final StreamMetadata metadata;
    @NonNull
    private final StreamFrame streamFrame;
    @NonNull
    private final StreamStats stats;

    StreamDispatchContext(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            @NonNull StreamMetadata metadata,
            @NonNull StreamFrame streamFrame,
            @NonNull StreamStats stats,
            long dispatchedAtMillis
    ) {
        super(clientId, event, dispatchedAtMillis);
        this.metadata = metadata;
        this.streamFrame = streamFrame;
        this.stats = stats;
    }

    @NonNull
    public StreamMetadata getMetadata() {
        return metadata;
    }

    @NonNull
    public StreamFrame getStreamFrame() {
        return streamFrame;
    }

    @NonNull
    public StreamStats getStats() {
        return stats;
    }
}
