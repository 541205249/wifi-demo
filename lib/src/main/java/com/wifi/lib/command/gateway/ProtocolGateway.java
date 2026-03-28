package com.wifi.lib.command.gateway;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.CommandEngine;
import com.wifi.lib.command.CommandSettingsRepository;
import com.wifi.lib.command.InboundCommand;
import com.wifi.lib.command.OutboundCommand;
import com.wifi.lib.command.ack.AckCodec;
import com.wifi.lib.command.ack.AckMessage;
import com.wifi.lib.command.profile.CommandProfile;
import com.wifi.lib.command.stream.StreamFrame;
import com.wifi.lib.command.stream.StreamMetadata;
import com.wifi.lib.command.stream.StreamPacketCodec;
import com.wifi.lib.command.stream.StreamSender;
import com.wifi.lib.command.stream.StreamStats;
import com.wifi.lib.command.stream.StreamStatsListener;
import com.wifi.lib.command.transfer.TransferChunk;
import com.wifi.lib.command.transfer.TransferMetadata;
import com.wifi.lib.command.transfer.TransferPacketCodec;
import com.wifi.lib.command.transfer.TransferProgress;
import com.wifi.lib.command.transfer.TransferProgressListener;
import com.wifi.lib.command.transfer.TransferSender;
import com.wifi.lib.log.DLog;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class ProtocolGateway {
    private static final String TAG = "ProtocolGateway";

    @NonNull
    private final CommandSettingsRepository commandSettingsRepository;
    @NonNull
    private final CommandEngine commandEngine;
    @NonNull
    private final AckCodec ackCodec = new AckCodec();
    @NonNull
    private final TransferPacketCodec transferPacketCodec = new TransferPacketCodec();
    @NonNull
    private final StreamPacketCodec streamPacketCodec = new StreamPacketCodec();

    public ProtocolGateway(@NonNull Context context, @NonNull CommandProfile commandProfile) {
        commandSettingsRepository = CommandSettingsRepository.getInstance(context, commandProfile);
        commandEngine = commandSettingsRepository.getCommandEngine();
        ensureCommandTableLoaded();
    }

    @NonNull
    public CommandSettingsRepository getCommandSettingsRepository() {
        return commandSettingsRepository;
    }

    @NonNull
    public CommandEngine getCommandEngine() {
        return commandEngine;
    }

    public void ensureCommandTableLoaded() {
        if (!commandSettingsRepository.snapshot().getCommandTable().isEmpty()) {
            return;
        }
        try {
            CommandSettingsRepository.LoadResult loadResult = commandSettingsRepository.reloadLast();
            if (loadResult != null) {
                DLog.i(TAG, "已恢复上次使用的编码表，source=" + loadResult.getSourceLabel());
                return;
            }
        } catch (IOException exception) {
            DLog.w(TAG, "恢复上次编码表失败，准备回退到内置示例", exception);
        }
        try {
            CommandSettingsRepository.LoadResult loadResult = commandSettingsRepository.loadBuiltInSample();
            DLog.i(TAG, "已加载内置编码表，source=" + loadResult.getSourceLabel());
        } catch (IOException exception) {
            DLog.e(TAG, "加载内置编码表失败", exception);
        }
    }

    @NonNull
    public ProtocolInboundEvent resolveInbound(@Nullable String rawMessage) {
        String normalizedMessage = normalizeMessage(rawMessage);
        if (normalizedMessage.isEmpty()) {
            return ProtocolInboundEvent.unknown("");
        }
        try {
            if (normalizedMessage.startsWith("ACK+") || normalizedMessage.startsWith("ERR+")) {
                return ProtocolInboundEvent.ack(normalizedMessage, ackCodec.decode(normalizedMessage));
            }
            if (normalizedMessage.startsWith("TF+")) {
                return ProtocolInboundEvent.transfer(normalizedMessage, transferPacketCodec.decodeChunk(normalizedMessage));
            }
            if (normalizedMessage.startsWith("SF+")) {
                return ProtocolInboundEvent.stream(normalizedMessage, streamPacketCodec.decodeFrame(normalizedMessage));
            }

            InboundCommand inboundCommand = commandEngine.resolveInbound(normalizedMessage);
            if (inboundCommand != null) {
                return ProtocolInboundEvent.command(normalizedMessage, inboundCommand);
            }
            return ProtocolInboundEvent.unknown(normalizedMessage);
        } catch (Exception exception) {
            DLog.e(TAG, "协议消息解析失败，raw=" + normalizedMessage, exception);
            return ProtocolInboundEvent.invalid(normalizedMessage, safeMessage(exception));
        }
    }

    @NonNull
    public OutboundCommand sendCommand(
            @NonNull String code,
            @Nullable Map<String, String> arguments,
            @NonNull ProtocolMessageTransport transport
    ) {
        OutboundCommand outboundCommand = commandEngine.prepareOutbound(code, arguments);
        transport.send(outboundCommand.getRawMessage());
        DLog.i(TAG, "协议网关已发送命令，code=" + outboundCommand.getCode());
        return outboundCommand;
    }

    public void sendAck(@NonNull AckMessage ackMessage, @NonNull ProtocolMessageTransport transport) {
        transport.send(ackCodec.encode(ackMessage));
        DLog.i(TAG, "协议网关已发送 ACK，ref=" + ackMessage.getReference());
    }

    @NonNull
    public TransferProgress sendTransferBytes(
            @NonNull TransferMetadata metadata,
            @NonNull byte[] payload,
            @NonNull ProtocolMessageTransport transport,
            @Nullable TransferProgressListener listener
    ) {
        TransferSender sender = new TransferSender();
        return sender.sendBytes(metadata, payload, transport::send, listener);
    }

    @NonNull
    public TransferProgress sendTransferFile(
            @NonNull TransferMetadata metadata,
            @NonNull File file,
            @NonNull ProtocolMessageTransport transport,
            @Nullable TransferProgressListener listener
    ) throws IOException {
        TransferSender sender = new TransferSender();
        return sender.sendFile(metadata, file, transport::send, listener);
    }

    @NonNull
    public StreamSender createStreamSender(@NonNull StreamMetadata metadata) {
        StreamSender sender = new StreamSender();
        sender.start(metadata);
        return sender;
    }

    @NonNull
    public StreamStats sendStreamPayload(
            @NonNull StreamSender sender,
            @NonNull byte[] payload,
            @NonNull ProtocolMessageTransport transport,
            @Nullable StreamStatsListener listener
    ) {
        return sender.sendPayload(payload, transport::send, listener);
    }

    @NonNull
    public StreamStats finishStream(
            @NonNull StreamSender sender,
            @NonNull ProtocolMessageTransport transport,
            @Nullable StreamStatsListener listener
    ) {
        return sender.finish(transport::send, listener);
    }

    public void sendStreamFrame(@NonNull StreamFrame streamFrame, @NonNull ProtocolMessageTransport transport) {
        transport.send(streamPacketCodec.encodeFrame(streamFrame));
        DLog.i(TAG, "协议网关已发送 stream frame，session=" + streamFrame.getSessionId()
                + ", sequence=" + streamFrame.getSequence());
    }

    @NonNull
    private String normalizeMessage(@Nullable String rawMessage) {
        if (rawMessage == null) {
            return "";
        }
        String normalized = rawMessage.trim();
        return TextUtils.isEmpty(normalized) ? "" : normalized;
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage()) ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
