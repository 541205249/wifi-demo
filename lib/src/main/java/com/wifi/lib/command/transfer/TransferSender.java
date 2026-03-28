package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.DLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public final class TransferSender {
    private static final String TAG = "TransferSender";

    @NonNull
    private final TransferPacketCodec packetCodec = new TransferPacketCodec();

    @NonNull
    public TransferProgress sendBytes(
            @NonNull TransferMetadata metadata,
            @NonNull byte[] payload,
            @NonNull TransferTransport transport
    ) {
        return sendBytes(metadata, payload, transport, null);
    }

    @NonNull
    public TransferProgress sendBytes(
            @NonNull TransferMetadata metadata,
            @NonNull byte[] payload,
            @NonNull TransferTransport transport,
            @Nullable TransferProgressListener listener
    ) {
        validatePayloadLength(metadata, payload.length);
        validateMd5IfNecessary(metadata, payload);

        int totalChunks = metadata.getTotalChunks();
        int chunkSize = metadata.getChunkSize();
        int transferredChunks = 0;
        long transferredBytes = 0L;

        for (int offset = 0; offset < payload.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, payload.length);
            byte[] chunkPayload = Arrays.copyOfRange(payload, offset, end);
            TransferChunk chunk = new TransferChunk(
                    metadata.getSessionId(),
                    transferredChunks + 1,
                    totalChunks,
                    offset,
                    chunkPayload,
                    null
            );
            transport.send(packetCodec.encodeChunk(chunk));
            transferredChunks++;
            transferredBytes += chunkPayload.length;
            notifyProgress(listener, metadata, transferredBytes, transferredChunks, TransferSessionState.TRANSFERRING);
        }

        TransferProgress completed = new TransferProgress(
                metadata.getSessionId(),
                metadata.getTotalBytes(),
                transferredBytes,
                totalChunks,
                transferredChunks,
                TransferSessionState.COMPLETED
        );
        notifyProgress(listener, completed);
        DLog.i(TAG, "内存数据发送完成，sessionId=" + metadata.getSessionId() + ", chunks=" + transferredChunks);
        return completed;
    }

    @NonNull
    public TransferProgress sendFile(
            @NonNull TransferMetadata metadata,
            @NonNull File file,
            @NonNull TransferTransport transport
    ) throws IOException {
        return sendFile(metadata, file, transport, null);
    }

    @NonNull
    public TransferProgress sendFile(
            @NonNull TransferMetadata metadata,
            @NonNull File file,
            @NonNull TransferTransport transport,
            @Nullable TransferProgressListener listener
    ) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new IOException("待发送文件不存在: " + file.getAbsolutePath());
        }
        validatePayloadLength(metadata, file.length());

        int totalChunks = metadata.getTotalChunks();
        int transferredChunks = 0;
        long transferredBytes = 0L;
        byte[] buffer = new byte[metadata.getChunkSize()];

        try (FileInputStream inputStream = new FileInputStream(file)) {
            int readCount;
            while ((readCount = inputStream.read(buffer)) != -1) {
                byte[] chunkPayload = readCount == buffer.length
                        ? Arrays.copyOf(buffer, buffer.length)
                        : Arrays.copyOf(buffer, readCount);
                TransferChunk chunk = new TransferChunk(
                        metadata.getSessionId(),
                        transferredChunks + 1,
                        totalChunks,
                        transferredBytes,
                        chunkPayload,
                        null
                );
                transport.send(packetCodec.encodeChunk(chunk));
                transferredChunks++;
                transferredBytes += readCount;
                notifyProgress(listener, metadata, transferredBytes, transferredChunks, TransferSessionState.TRANSFERRING);
            }
        }

        TransferProgress completed = new TransferProgress(
                metadata.getSessionId(),
                metadata.getTotalBytes(),
                transferredBytes,
                totalChunks,
                transferredChunks,
                TransferSessionState.COMPLETED
        );
        notifyProgress(listener, completed);
        DLog.i(TAG, "文件发送完成，sessionId=" + metadata.getSessionId() + ", file=" + file.getName());
        return completed;
    }

    private void validatePayloadLength(@NonNull TransferMetadata metadata, long payloadLength) {
        if (metadata.getTotalBytes() != payloadLength) {
            throw new IllegalArgumentException("transfer totalBytes 与实际长度不一致，expected="
                    + metadata.getTotalBytes() + ", actual=" + payloadLength);
        }
    }

    private void validateMd5IfNecessary(@NonNull TransferMetadata metadata, @NonNull byte[] payload) {
        if (metadata.getMd5().isEmpty()) {
            return;
        }
        String actualMd5 = TransferChecksums.md5Hex(payload);
        if (!metadata.getMd5().equalsIgnoreCase(actualMd5)) {
            throw new IllegalArgumentException("transfer md5 校验失败，expected="
                    + metadata.getMd5() + ", actual=" + actualMd5);
        }
    }

    private void notifyProgress(
            @Nullable TransferProgressListener listener,
            @NonNull TransferMetadata metadata,
            long transferredBytes,
            int transferredChunks,
            @NonNull TransferSessionState state
    ) {
        if (listener == null) {
            return;
        }
        listener.onProgress(new TransferProgress(
                metadata.getSessionId(),
                metadata.getTotalBytes(),
                transferredBytes,
                metadata.getTotalChunks(),
                transferredChunks,
                state
        ));
    }

    private void notifyProgress(@Nullable TransferProgressListener listener, @NonNull TransferProgress progress) {
        if (listener == null) {
            return;
        }
        listener.onProgress(progress);
    }
}
