package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.DLog;

public final class StreamSender {
    private static final String TAG = "StreamSender";

    @NonNull
    private final StreamPacketCodec packetCodec = new StreamPacketCodec();

    @NonNull
    private StreamSessionState state = StreamSessionState.IDLE;
    @NonNull
    private StreamMetadata metadata = new StreamMetadata.Builder()
            .setSessionId("empty")
            .setDirection(StreamDirection.APP_TO_DEVICE)
            .setStreamType("empty")
            .build();
    private int nextSequence = 1;
    private int totalFrames;
    private long totalBytes;

    public void start(@NonNull StreamMetadata metadata) {
        this.metadata = metadata;
        this.nextSequence = 1;
        this.totalFrames = 0;
        this.totalBytes = 0L;
        this.state = StreamSessionState.PREPARED;
        DLog.i(TAG, "实时流发送已开始，sessionId=" + metadata.getSessionId()
                + ", type=" + metadata.getStreamType());
    }

    @NonNull
    public StreamStats sendPayload(@NonNull byte[] payload, @NonNull StreamTransport transport) {
        return sendPayload(payload, transport, null);
    }

    @NonNull
    public StreamStats sendPayload(
            @NonNull byte[] payload,
            @NonNull StreamTransport transport,
            @Nullable StreamStatsListener listener
    ) {
        requireStarted();
        if (payload.length > metadata.getFrameSize()) {
            throw new IllegalArgumentException("stream payload 超过 frameSize，size="
                    + payload.length + ", frameSize=" + metadata.getFrameSize());
        }
        StreamFrame frame = new StreamFrame(
                metadata.getSessionId(),
                nextSequence++,
                System.currentTimeMillis(),
                payload,
                null,
                false
        );
        transport.send(packetCodec.encodeFrame(frame));
        totalFrames++;
        totalBytes += payload.length;
        state = StreamSessionState.STREAMING;
        StreamStats stats = snapshot(frame.getSequence(), frame.getTimestampMs());
        notifyStats(listener, stats);
        return stats;
    }

    @NonNull
    public StreamStats finish(@NonNull StreamTransport transport) {
        return finish(transport, null);
    }

    @NonNull
    public StreamStats finish(@NonNull StreamTransport transport, @Nullable StreamStatsListener listener) {
        requireStarted();
        StreamFrame endFrame = new StreamFrame(
                metadata.getSessionId(),
                nextSequence++,
                System.currentTimeMillis(),
                new byte[0],
                null,
                true
        );
        transport.send(packetCodec.encodeFrame(endFrame));
        state = StreamSessionState.STOPPED;
        StreamStats stats = snapshot(endFrame.getSequence(), endFrame.getTimestampMs());
        notifyStats(listener, stats);
        DLog.i(TAG, "实时流发送结束，sessionId=" + metadata.getSessionId()
                + ", frames=" + totalFrames + ", bytes=" + totalBytes);
        return stats;
    }

    public void cancel() {
        state = StreamSessionState.CANCELED;
        nextSequence = 1;
        totalFrames = 0;
        totalBytes = 0L;
    }

    @NonNull
    public StreamStats snapshot() {
        return snapshot(Math.max(0, nextSequence - 1), 0L);
    }

    @NonNull
    private StreamStats snapshot(int lastSequence, long lastTimestampMs) {
        return new StreamStats(
                metadata.getSessionId(),
                totalFrames,
                totalBytes,
                lastSequence,
                0,
                lastTimestampMs,
                state
        );
    }

    private void notifyStats(@Nullable StreamStatsListener listener, @NonNull StreamStats stats) {
        if (listener == null) {
            return;
        }
        listener.onStats(stats);
    }

    private void requireStarted() {
        if (state != StreamSessionState.PREPARED && state != StreamSessionState.STREAMING) {
            throw new IllegalStateException("stream 发送会话尚未 start");
        }
    }
}
