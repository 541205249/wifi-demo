package com.wifi.lib.command.stream;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;

public final class StreamReceiver {
    private static final String TAG = "StreamReceiver";

    @NonNull
    private final StreamPacketCodec packetCodec = new StreamPacketCodec();

    @NonNull
    private StreamSessionState state = StreamSessionState.IDLE;
    @NonNull
    private StreamMetadata metadata = new StreamMetadata.Builder()
            .setSessionId("empty")
            .setDirection(StreamDirection.DEVICE_TO_APP)
            .setStreamType("empty")
            .build();
    private boolean prepared;
    private int totalFrames;
    private long totalBytes;
    private int lastSequence;
    private int droppedFrames;
    private long lastTimestampMs;

    public void start(@NonNull StreamMetadata metadata) {
        this.metadata = metadata;
        this.prepared = true;
        this.totalFrames = 0;
        this.totalBytes = 0L;
        this.lastSequence = 0;
        this.droppedFrames = 0;
        this.lastTimestampMs = 0L;
        this.state = StreamSessionState.PREPARED;
        DLog.i(TAG, "实时流接收已开始，sessionId=" + metadata.getSessionId()
                + ", type=" + metadata.getStreamType());
    }

    @NonNull
    public StreamStats acceptFrame(@NonNull String rawFrame) {
        return acceptFrame(packetCodec.decodeFrame(rawFrame));
    }

    @NonNull
    public StreamStats acceptFrame(@NonNull StreamFrame frame) {
        requirePrepared();
        if (!metadata.getSessionId().equals(frame.getSessionId())) {
            state = StreamSessionState.FAILED;
            throw new IllegalArgumentException("stream sessionId 不匹配，expected="
                    + metadata.getSessionId() + ", actual=" + frame.getSessionId());
        }
        if (metadata.isChecksumEnabled() && !frame.isCrcValid()) {
            state = StreamSessionState.FAILED;
            throw new IllegalArgumentException("stream CRC 校验失败，sequence=" + frame.getSequence());
        }
        if (frame.getPayloadSize() > metadata.getFrameSize()) {
            state = StreamSessionState.FAILED;
            throw new IllegalArgumentException("stream payload 超过 frameSize，sequence=" + frame.getSequence());
        }
        if (lastSequence > 0 && frame.getSequence() <= lastSequence) {
            state = StreamSessionState.FAILED;
            throw new IllegalStateException("stream sequence 不能回退，last="
                    + lastSequence + ", current=" + frame.getSequence());
        }
        if (lastSequence > 0 && frame.getSequence() > lastSequence + 1) {
            droppedFrames += frame.getSequence() - lastSequence - 1;
        }

        lastSequence = frame.getSequence();
        lastTimestampMs = frame.getTimestampMs();

        if (frame.isEndOfStream()) {
            state = StreamSessionState.STOPPED;
            DLog.i(TAG, "实时流接收结束，sessionId=" + metadata.getSessionId()
                    + ", frames=" + totalFrames + ", dropped=" + droppedFrames);
            return snapshot();
        }

        totalFrames++;
        totalBytes += frame.getPayloadSize();
        state = StreamSessionState.STREAMING;
        StreamStats stats = snapshot();
        DLog.i(TAG, "接收 stream frame 成功，sessionId=" + metadata.getSessionId()
                + ", sequence=" + frame.getSequence()
                + ", dropped=" + droppedFrames);
        return stats;
    }

    public void cancel() {
        state = StreamSessionState.CANCELED;
        prepared = false;
        totalFrames = 0;
        totalBytes = 0L;
        lastSequence = 0;
        droppedFrames = 0;
        lastTimestampMs = 0L;
    }

    public void reset() {
        cancel();
        state = StreamSessionState.IDLE;
    }

    @NonNull
    public StreamStats snapshot() {
        return new StreamStats(
                metadata.getSessionId(),
                totalFrames,
                totalBytes,
                lastSequence,
                droppedFrames,
                lastTimestampMs,
                state
        );
    }

    private void requirePrepared() {
        if (!prepared || (state != StreamSessionState.PREPARED && state != StreamSessionState.STREAMING)) {
            throw new IllegalStateException("stream 接收会话尚未 start");
        }
    }
}
