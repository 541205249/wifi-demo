package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public final class TransferReceiver {
    private static final String TAG = "TransferReceiver";

    @NonNull
    private final TransferPacketCodec packetCodec = new TransferPacketCodec();
    @NonNull
    private final Map<Integer, TransferChunk> chunkMap = new TreeMap<>();

    @NonNull
    private TransferSessionState state = TransferSessionState.IDLE;
    @NonNull
    private TransferMetadata metadata = new TransferMetadata.Builder()
            .setSessionId("empty")
            .setDirection(TransferDirection.DEVICE_TO_APP)
            .setFileName("empty")
            .build();
    private boolean prepared;
    private long receivedBytes;

    public void start(@NonNull TransferMetadata metadata) {
        this.metadata = metadata;
        this.chunkMap.clear();
        this.receivedBytes = 0L;
        this.prepared = true;
        this.state = TransferSessionState.PREPARED;
        DLog.i(TAG, "接收会话已开始，sessionId=" + metadata.getSessionId() + ", totalBytes=" + metadata.getTotalBytes());
    }

    @NonNull
    public TransferProgress acceptFrame(@NonNull String frame) {
        return acceptChunk(packetCodec.decodeChunk(frame));
    }

    @NonNull
    public TransferProgress acceptChunk(@NonNull TransferChunk chunk) {
        requirePrepared();
        if (!metadata.getSessionId().equals(chunk.getSessionId())) {
            state = TransferSessionState.FAILED;
            throw new IllegalArgumentException("收到的 transfer chunk sessionId 不匹配，expected="
                    + metadata.getSessionId() + ", actual=" + chunk.getSessionId());
        }
        if (!chunk.isCrcValid()) {
            state = TransferSessionState.FAILED;
            throw new IllegalArgumentException("收到的 transfer chunk CRC 校验失败，index=" + chunk.getIndex());
        }
        if (chunk.getTotalChunks() != metadata.getTotalChunks()) {
            state = TransferSessionState.FAILED;
            throw new IllegalArgumentException("收到的 transfer chunk totalChunks 不匹配，expected="
                    + metadata.getTotalChunks() + ", actual=" + chunk.getTotalChunks());
        }
        long expectedOffset = (long) (chunk.getIndex() - 1) * metadata.getChunkSize();
        if (chunk.getOffset() != expectedOffset) {
            state = TransferSessionState.FAILED;
            throw new IllegalArgumentException("收到的 transfer chunk offset 不匹配，index=" + chunk.getIndex()
                    + ", expected=" + expectedOffset + ", actual=" + chunk.getOffset());
        }
        if (chunk.getPayloadSize() > metadata.getChunkSize()) {
            state = TransferSessionState.FAILED;
            throw new IllegalArgumentException("收到的 transfer chunk 长度超过 chunkSize，index=" + chunk.getIndex());
        }
        if (chunkMap.containsKey(chunk.getIndex())) {
            throw new IllegalStateException("重复收到 transfer chunk，index=" + chunk.getIndex());
        }

        chunkMap.put(chunk.getIndex(), chunk);
        receivedBytes += chunk.getPayloadSize();
        state = isCompleted() ? TransferSessionState.COMPLETED : TransferSessionState.TRANSFERRING;

        TransferProgress progress = new TransferProgress(
                metadata.getSessionId(),
                metadata.getTotalBytes(),
                receivedBytes,
                metadata.getTotalChunks(),
                chunkMap.size(),
                state
        );
        DLog.i(TAG, "接收 transfer chunk 成功，sessionId=" + metadata.getSessionId()
                + ", index=" + chunk.getIndex()
                + ", percent=" + progress.getPercent());
        return progress;
    }

    public boolean isCompleted() {
        return prepared
                && chunkMap.size() == metadata.getTotalChunks()
                && receivedBytes == metadata.getTotalBytes();
    }

    @NonNull
    public byte[] buildPayload() {
        requirePrepared();
        if (!isCompleted()) {
            throw new IllegalStateException("transfer 尚未接收完成，不能组装 payload");
        }

        int initialSize = metadata.getTotalBytes() > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) metadata.getTotalBytes();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(initialSize);
        for (int index = 1; index <= metadata.getTotalChunks(); index++) {
            TransferChunk chunk = chunkMap.get(index);
            if (chunk == null) {
                throw new IllegalStateException("缺少 transfer chunk，index=" + index);
            }
            outputStream.write(chunk.getPayload(), 0, chunk.getPayloadSize());
        }
        byte[] payload = outputStream.toByteArray();
        if (payload.length != metadata.getTotalBytes()) {
            throw new IllegalStateException("组装后的 transfer payload 长度不正确，expected="
                    + metadata.getTotalBytes() + ", actual=" + payload.length);
        }
        if (!metadata.getMd5().isEmpty()) {
            String actualMd5 = TransferChecksums.md5Hex(payload);
            if (!metadata.getMd5().equalsIgnoreCase(actualMd5)) {
                throw new IllegalStateException("组装后的 transfer payload MD5 校验失败，expected="
                        + metadata.getMd5() + ", actual=" + actualMd5);
            }
        }
        return payload;
    }

    public void writeTo(@NonNull File targetFile) throws IOException {
        byte[] payload = buildPayload();
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            outputStream.write(payload);
            outputStream.flush();
        }
    }

    public void cancel() {
        state = TransferSessionState.CANCELED;
        chunkMap.clear();
        receivedBytes = 0L;
        prepared = false;
    }

    public void reset() {
        cancel();
        state = TransferSessionState.IDLE;
    }

    @NonNull
    public TransferProgress snapshot() {
        return new TransferProgress(
                metadata.getSessionId(),
                metadata.getTotalBytes(),
                receivedBytes,
                metadata.getTotalChunks(),
                chunkMap.size(),
                state
        );
    }

    private void requirePrepared() {
        if (!prepared) {
            throw new IllegalStateException("transfer 接收会话尚未 start");
        }
    }
}
