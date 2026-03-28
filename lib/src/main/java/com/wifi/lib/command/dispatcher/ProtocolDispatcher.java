package com.wifi.lib.command.dispatcher;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.CommandCode;
import com.wifi.lib.command.InboundCommand;
import com.wifi.lib.command.ack.AckChannel;
import com.wifi.lib.command.ack.AckMessage;
import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.gateway.ProtocolPayloadType;
import com.wifi.lib.command.stream.StreamDirection;
import com.wifi.lib.command.stream.StreamFrame;
import com.wifi.lib.command.stream.StreamMetadata;
import com.wifi.lib.command.stream.StreamReceiver;
import com.wifi.lib.command.stream.StreamSessionState;
import com.wifi.lib.command.stream.StreamStats;
import com.wifi.lib.command.transfer.TransferChunk;
import com.wifi.lib.command.transfer.TransferDirection;
import com.wifi.lib.command.transfer.TransferMetadata;
import com.wifi.lib.command.transfer.TransferProgress;
import com.wifi.lib.command.transfer.TransferReceiver;
import com.wifi.lib.command.transfer.TransferSessionState;
import com.wifi.lib.log.DLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtocolDispatcher {
    private static final String TAG = "ProtocolDispatcher";

    @NonNull
    private final Map<String, CommandUseCase> commandUseCases = new ConcurrentHashMap<>();
    @NonNull
    private final Map<String, AckUseCase> ackRefUseCases = new ConcurrentHashMap<>();
    @NonNull
    private final Map<String, AckUseCase> ackChannelRefUseCases = new ConcurrentHashMap<>();
    @NonNull
    private final Map<String, AckUseCase> ackSessionUseCases = new ConcurrentHashMap<>();
    @NonNull
    private final Map<String, TransferRoute> transferRoutes = new ConcurrentHashMap<>();
    @NonNull
    private final Map<String, StreamRoute> streamRoutes = new ConcurrentHashMap<>();

    @Nullable
    private volatile CommandUseCase commandFallbackUseCase;
    @Nullable
    private volatile AckUseCase ackFallbackUseCase;
    @Nullable
    private volatile TransferUseCase transferFallbackUseCase;
    @Nullable
    private volatile StreamUseCase streamFallbackUseCase;
    @Nullable
    private volatile ProtocolUseCase unknownUseCase;
    @Nullable
    private volatile ProtocolUseCase invalidUseCase;

    public void registerCommandUseCase(@NonNull String code, @NonNull CommandUseCase useCase) {
        String normalizedCode = normalizeCode(code);
        commandUseCases.put(normalizedCode, useCase);
        DLog.i(TAG, "注册 command 业务处理器，code=" + normalizedCode);
    }

    public void unregisterCommandUseCase(@NonNull String code) {
        String normalizedCode = normalizeCode(code);
        commandUseCases.remove(normalizedCode);
        DLog.i(TAG, "移除 command 业务处理器，code=" + normalizedCode);
    }

    public void registerAckUseCase(@NonNull String reference, @NonNull AckUseCase useCase) {
        String normalizedRef = normalize(reference);
        ackRefUseCases.put(normalizedRef, useCase);
        DLog.i(TAG, "注册 ACK 业务处理器，ref=" + normalizedRef);
    }

    public void registerAckUseCase(@NonNull AckChannel channel, @NonNull String reference, @NonNull AckUseCase useCase) {
        String routeKey = buildAckChannelRouteKey(channel, reference);
        ackChannelRefUseCases.put(routeKey, useCase);
        DLog.i(TAG, "注册 ACK 业务处理器，route=" + routeKey);
    }

    public void registerAckSessionUseCase(@NonNull String sessionId, @NonNull AckUseCase useCase) {
        String normalizedSessionId = normalize(sessionId);
        ackSessionUseCases.put(normalizedSessionId, useCase);
        DLog.i(TAG, "注册 ACK 会话处理器，sessionId=" + normalizedSessionId);
    }

    public void unregisterAckUseCase(@NonNull String reference) {
        String normalizedRef = normalize(reference);
        ackRefUseCases.remove(normalizedRef);
        DLog.i(TAG, "移除 ACK 业务处理器，ref=" + normalizedRef);
    }

    public void unregisterAckUseCase(@NonNull AckChannel channel, @NonNull String reference) {
        String routeKey = buildAckChannelRouteKey(channel, reference);
        ackChannelRefUseCases.remove(routeKey);
        DLog.i(TAG, "移除 ACK 业务处理器，route=" + routeKey);
    }

    public void unregisterAckSessionUseCase(@NonNull String sessionId) {
        String normalizedSessionId = normalize(sessionId);
        ackSessionUseCases.remove(normalizedSessionId);
        DLog.i(TAG, "移除 ACK 会话处理器，sessionId=" + normalizedSessionId);
    }

    public void registerTransferSession(@NonNull TransferMetadata metadata, @NonNull TransferUseCase useCase) {
        registerTransferSession(metadata, true, useCase);
    }

    public void registerTransferSession(
            @NonNull TransferMetadata metadata,
            boolean autoRemoveOnTerminalState,
            @NonNull TransferUseCase useCase
    ) {
        TransferRoute route = new TransferRoute(metadata, useCase, autoRemoveOnTerminalState);
        transferRoutes.put(metadata.getSessionId(), route);
        DLog.i(TAG, "注册 transfer 会话处理器，sessionId=" + metadata.getSessionId());
    }

    public void unregisterTransferSession(@NonNull String sessionId) {
        String normalizedSessionId = normalize(sessionId);
        transferRoutes.remove(normalizedSessionId);
        DLog.i(TAG, "移除 transfer 会话处理器，sessionId=" + normalizedSessionId);
    }

    public void registerStreamSession(@NonNull StreamMetadata metadata, @NonNull StreamUseCase useCase) {
        registerStreamSession(metadata, true, useCase);
    }

    public void registerStreamSession(
            @NonNull StreamMetadata metadata,
            boolean autoRemoveOnTerminalState,
            @NonNull StreamUseCase useCase
    ) {
        StreamRoute route = new StreamRoute(metadata, useCase, autoRemoveOnTerminalState);
        streamRoutes.put(metadata.getSessionId(), route);
        DLog.i(TAG, "注册 stream 会话处理器，sessionId=" + metadata.getSessionId());
    }

    public void unregisterStreamSession(@NonNull String sessionId) {
        String normalizedSessionId = normalize(sessionId);
        streamRoutes.remove(normalizedSessionId);
        DLog.i(TAG, "移除 stream 会话处理器，sessionId=" + normalizedSessionId);
    }

    public void setCommandFallbackUseCase(@Nullable CommandUseCase useCase) {
        commandFallbackUseCase = useCase;
    }

    public void setAckFallbackUseCase(@Nullable AckUseCase useCase) {
        ackFallbackUseCase = useCase;
    }

    public void setTransferFallbackUseCase(@Nullable TransferUseCase useCase) {
        transferFallbackUseCase = useCase;
    }

    public void setStreamFallbackUseCase(@Nullable StreamUseCase useCase) {
        streamFallbackUseCase = useCase;
    }

    public void setUnknownUseCase(@Nullable ProtocolUseCase useCase) {
        unknownUseCase = useCase;
    }

    public void setInvalidUseCase(@Nullable ProtocolUseCase useCase) {
        invalidUseCase = useCase;
    }

    public void clearAll() {
        commandUseCases.clear();
        ackRefUseCases.clear();
        ackChannelRefUseCases.clear();
        ackSessionUseCases.clear();
        transferRoutes.clear();
        streamRoutes.clear();
        commandFallbackUseCase = null;
        ackFallbackUseCase = null;
        transferFallbackUseCase = null;
        streamFallbackUseCase = null;
        unknownUseCase = null;
        invalidUseCase = null;
        DLog.i(TAG, "已清空全部协议分发处理器");
    }

    @NonNull
    public ProtocolDispatchResult dispatch(@NonNull String clientId, @NonNull ProtocolInboundEvent event) {
        long dispatchedAtMillis = System.currentTimeMillis();
        ProtocolPayloadType payloadType = event.getPayloadType();
        try {
            switch (payloadType) {
                case COMMAND:
                    return dispatchCommand(clientId, event, dispatchedAtMillis);
                case ACK:
                    return dispatchAck(clientId, event, dispatchedAtMillis);
                case TRANSFER:
                    return dispatchTransfer(clientId, event, dispatchedAtMillis);
                case STREAM:
                    return dispatchStream(clientId, event, dispatchedAtMillis);
                case UNKNOWN:
                    return dispatchUnknown(clientId, event, dispatchedAtMillis);
                case INVALID:
                    return dispatchInvalid(clientId, event, dispatchedAtMillis);
                default:
                    return ProtocolDispatchResult.unhandled(payloadType, "", "未支持的 payloadType");
            }
        } catch (Exception exception) {
            DLog.e(TAG, "协议业务分发失败，payloadType=" + payloadType + ", raw=" + event.getRawMessage(), exception);
            return ProtocolDispatchResult.failed(payloadType, "", safeMessage(exception));
        }
    }

    @NonNull
    private ProtocolDispatchResult dispatchCommand(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        InboundCommand inboundCommand = event.getInboundCommand();
        if (inboundCommand == null) {
            return ProtocolDispatchResult.failed(ProtocolPayloadType.COMMAND, "", "command 事件缺少 InboundCommand");
        }
        String code = normalizeCode(inboundCommand.getCode());
        CommandUseCase useCase = commandUseCases.get(code);
        String routeKey = "command:" + code;
        if (useCase == null) {
            useCase = commandFallbackUseCase;
            routeKey = "command:*";
        }
        if (useCase == null) {
            DLog.w(TAG, "command 事件未命中处理器，code=" + code);
            return ProtocolDispatchResult.unhandled(ProtocolPayloadType.COMMAND, routeKey, "未找到匹配的 command 处理器");
        }

        CommandDispatchContext context = new CommandDispatchContext(clientId, event, inboundCommand, dispatchedAtMillis);
        useCase.onCommand(context);
        DLog.i(TAG, "command 事件已分发，code=" + code);
        return ProtocolDispatchResult.handled(ProtocolPayloadType.COMMAND, routeKey, "已分发 command 编码 " + code, false);
    }

    @NonNull
    private ProtocolDispatchResult dispatchAck(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        AckMessage ackMessage = event.getAckMessage();
        if (ackMessage == null) {
            return ProtocolDispatchResult.failed(ProtocolPayloadType.ACK, "", "ack 事件缺少 AckMessage");
        }
        String ref = normalize(ackMessage.getReference());
        String sessionId = normalize(ackMessage.getSessionId());
        String channelRouteKey = buildAckChannelRouteKey(ackMessage.getChannel(), ref);
        AckUseCase useCase = ackChannelRefUseCases.get(channelRouteKey);
        String routeKey = "ack:" + channelRouteKey;
        if (useCase == null && !ref.isEmpty()) {
            useCase = ackRefUseCases.get(ref);
            routeKey = "ack:" + ref;
        }
        if (useCase == null && !sessionId.isEmpty()) {
            useCase = ackSessionUseCases.get(sessionId);
            routeKey = "ack:session:" + sessionId;
        }
        if (useCase == null) {
            useCase = ackFallbackUseCase;
            routeKey = "ack:*";
        }
        if (useCase == null) {
            DLog.w(TAG, "ACK 事件未命中处理器，ref=" + ref + ", sessionId=" + sessionId);
            return ProtocolDispatchResult.unhandled(ProtocolPayloadType.ACK, routeKey, "未找到匹配的 ACK 处理器");
        }

        AckDispatchContext context = new AckDispatchContext(clientId, event, ackMessage, dispatchedAtMillis);
        useCase.onAck(context);
        DLog.i(TAG, "ACK 事件已分发，ref=" + ref + ", sessionId=" + sessionId);
        return ProtocolDispatchResult.handled(ProtocolPayloadType.ACK, routeKey, "已分发 ACK 回执", false);
    }

    @NonNull
    private ProtocolDispatchResult dispatchTransfer(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        TransferChunk transferChunk = event.getTransferChunk();
        if (transferChunk == null) {
            return ProtocolDispatchResult.failed(ProtocolPayloadType.TRANSFER, "", "transfer 事件缺少 TransferChunk");
        }
        String sessionId = transferChunk.getSessionId();
        TransferRoute route = transferRoutes.get(sessionId);
        TransferUseCase useCase = route == null ? transferFallbackUseCase : route.useCase;
        String routeKey = route == null ? "transfer:*" : "transfer:" + sessionId;
        if (useCase == null) {
            DLog.w(TAG, "transfer 事件未命中处理器，sessionId=" + sessionId);
            return ProtocolDispatchResult.unhandled(ProtocolPayloadType.TRANSFER, routeKey, "未找到匹配的 transfer 处理器");
        }

        TransferDispatchContext context;
        boolean autoRemoved = false;
        if (route == null) {
            TransferMetadata metadata = new TransferMetadata.Builder()
                    .setSessionId(sessionId)
                    .setDirection(TransferDirection.DEVICE_TO_APP)
                    .setFileName("unknown")
                    .setTotalBytes(0L)
                    .build();
            TransferProgress progress = new TransferProgress(
                    sessionId,
                    0L,
                    0L,
                    transferChunk.getTotalChunks(),
                    0,
                    TransferSessionState.PREPARED
            );
            context = new TransferDispatchContext(clientId, event, metadata, transferChunk, progress, null, dispatchedAtMillis);
        } else {
            try {
                TransferRoute.DispatchSnapshot snapshot = route.accept(clientId, event, transferChunk, dispatchedAtMillis);
                context = snapshot.context;
                autoRemoved = snapshot.autoRemoved;
            } catch (Exception exception) {
                if (route.autoRemoveOnTerminalState) {
                    transferRoutes.remove(sessionId);
                }
                DLog.e(TAG, "transfer 会话处理失败，sessionId=" + sessionId, exception);
                return ProtocolDispatchResult.failed(ProtocolPayloadType.TRANSFER, routeKey, safeMessage(exception));
            }
        }

        useCase.onTransfer(context);
        DLog.i(TAG, "transfer 事件已分发，sessionId=" + sessionId
                + ", percent=" + context.getProgress().getPercent()
                + ", autoRemoved=" + autoRemoved);
        return ProtocolDispatchResult.handled(ProtocolPayloadType.TRANSFER, routeKey, "已分发 transfer 分片", autoRemoved);
    }

    @NonNull
    private ProtocolDispatchResult dispatchStream(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        StreamFrame streamFrame = event.getStreamFrame();
        if (streamFrame == null) {
            return ProtocolDispatchResult.failed(ProtocolPayloadType.STREAM, "", "stream 事件缺少 StreamFrame");
        }
        String sessionId = streamFrame.getSessionId();
        StreamRoute route = streamRoutes.get(sessionId);
        StreamUseCase useCase = route == null ? streamFallbackUseCase : route.useCase;
        String routeKey = route == null ? "stream:*" : "stream:" + sessionId;
        if (useCase == null) {
            DLog.w(TAG, "stream 事件未命中处理器，sessionId=" + sessionId);
            return ProtocolDispatchResult.unhandled(ProtocolPayloadType.STREAM, routeKey, "未找到匹配的 stream 处理器");
        }

        StreamDispatchContext context;
        boolean autoRemoved = false;
        if (route == null) {
            StreamMetadata metadata = new StreamMetadata.Builder()
                    .setSessionId(sessionId)
                    .setDirection(StreamDirection.DEVICE_TO_APP)
                    .setStreamType("unknown")
                    .build();
            StreamStats stats = new StreamStats(sessionId, 0, 0L, 0, 0, 0L, StreamSessionState.PREPARED);
            context = new StreamDispatchContext(clientId, event, metadata, streamFrame, stats, dispatchedAtMillis);
        } else {
            try {
                StreamRoute.DispatchSnapshot snapshot = route.accept(clientId, event, streamFrame, dispatchedAtMillis);
                context = snapshot.context;
                autoRemoved = snapshot.autoRemoved;
            } catch (Exception exception) {
                if (route.autoRemoveOnTerminalState) {
                    streamRoutes.remove(sessionId);
                }
                DLog.e(TAG, "stream 会话处理失败，sessionId=" + sessionId, exception);
                return ProtocolDispatchResult.failed(ProtocolPayloadType.STREAM, routeKey, safeMessage(exception));
            }
        }

        useCase.onStream(context);
        DLog.i(TAG, "stream 事件已分发，sessionId=" + sessionId
                + ", frames=" + context.getStats().getTotalFrames()
                + ", autoRemoved=" + autoRemoved);
        return ProtocolDispatchResult.handled(ProtocolPayloadType.STREAM, routeKey, "已分发 stream 帧", autoRemoved);
    }

    @NonNull
    private ProtocolDispatchResult dispatchUnknown(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        if (unknownUseCase == null) {
            DLog.w(TAG, "收到 unknown 事件，raw=" + event.getRawMessage());
            return ProtocolDispatchResult.unhandled(ProtocolPayloadType.UNKNOWN, "unknown:*", "未注册 unknown 处理器");
        }
        unknownUseCase.onDispatch(new ProtocolDispatchContext(clientId, event, dispatchedAtMillis));
        return ProtocolDispatchResult.handled(ProtocolPayloadType.UNKNOWN, "unknown:*", "已分发 unknown 事件", false);
    }

    @NonNull
    private ProtocolDispatchResult dispatchInvalid(
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event,
            long dispatchedAtMillis
    ) {
        if (invalidUseCase == null) {
            DLog.w(TAG, "收到 invalid 事件，raw=" + event.getRawMessage() + ", error=" + event.getErrorMessage());
            return ProtocolDispatchResult.unhandled(ProtocolPayloadType.INVALID, "invalid:*", "未注册 invalid 处理器");
        }
        invalidUseCase.onDispatch(new ProtocolDispatchContext(clientId, event, dispatchedAtMillis));
        return ProtocolDispatchResult.handled(ProtocolPayloadType.INVALID, "invalid:*", "已分发 invalid 事件", false);
    }

    @NonNull
    private String normalizeCode(@NonNull String code) {
        return CommandCode.of(code).getValue();
    }

    @NonNull
    private String buildAckChannelRouteKey(@NonNull AckChannel channel, @Nullable String reference) {
        return channel.name() + "#" + normalize(reference);
    }

    @NonNull
    private String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage()) ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private final class TransferRoute {
        @NonNull
        private final TransferMetadata metadata;
        @NonNull
        private final TransferUseCase useCase;
        @NonNull
        private final TransferReceiver receiver;
        private final boolean autoRemoveOnTerminalState;

        private TransferRoute(
                @NonNull TransferMetadata metadata,
                @NonNull TransferUseCase useCase,
                boolean autoRemoveOnTerminalState
        ) {
            this.metadata = metadata;
            this.useCase = useCase;
            this.autoRemoveOnTerminalState = autoRemoveOnTerminalState;
            this.receiver = new TransferReceiver();
            this.receiver.start(metadata);
        }

        @NonNull
        private DispatchSnapshot accept(
                @NonNull String clientId,
                @NonNull ProtocolInboundEvent event,
                @NonNull TransferChunk transferChunk,
                long dispatchedAtMillis
        ) {
            TransferDispatchContext context;
            boolean shouldAutoRemove;
            synchronized (this) {
                TransferProgress progress = receiver.acceptChunk(transferChunk);
                byte[] completedPayload = receiver.isCompleted() ? receiver.buildPayload() : null;
                context = new TransferDispatchContext(
                        clientId,
                        event,
                        metadata,
                        transferChunk,
                        progress,
                        completedPayload,
                        dispatchedAtMillis
                );
                shouldAutoRemove = autoRemoveOnTerminalState && isTerminal(progress.getState());
            }
            if (shouldAutoRemove) {
                transferRoutes.remove(metadata.getSessionId(), this);
            }
            return new DispatchSnapshot(context, shouldAutoRemove);
        }

        private boolean isTerminal(@NonNull TransferSessionState state) {
            return state == TransferSessionState.COMPLETED
                    || state == TransferSessionState.FAILED
                    || state == TransferSessionState.CANCELED;
        }

        private final class DispatchSnapshot {
            @NonNull
            private final TransferDispatchContext context;
            private final boolean autoRemoved;

            private DispatchSnapshot(@NonNull TransferDispatchContext context, boolean autoRemoved) {
                this.context = context;
                this.autoRemoved = autoRemoved;
            }
        }
    }

    private final class StreamRoute {
        @NonNull
        private final StreamMetadata metadata;
        @NonNull
        private final StreamUseCase useCase;
        @NonNull
        private final StreamReceiver receiver;
        private final boolean autoRemoveOnTerminalState;

        private StreamRoute(
                @NonNull StreamMetadata metadata,
                @NonNull StreamUseCase useCase,
                boolean autoRemoveOnTerminalState
        ) {
            this.metadata = metadata;
            this.useCase = useCase;
            this.autoRemoveOnTerminalState = autoRemoveOnTerminalState;
            this.receiver = new StreamReceiver();
            this.receiver.start(metadata);
        }

        @NonNull
        private DispatchSnapshot accept(
                @NonNull String clientId,
                @NonNull ProtocolInboundEvent event,
                @NonNull StreamFrame streamFrame,
                long dispatchedAtMillis
        ) {
            StreamDispatchContext context;
            boolean shouldAutoRemove;
            synchronized (this) {
                StreamStats stats = receiver.acceptFrame(streamFrame);
                context = new StreamDispatchContext(
                        clientId,
                        event,
                        metadata,
                        streamFrame,
                        stats,
                        dispatchedAtMillis
                );
                shouldAutoRemove = autoRemoveOnTerminalState && isTerminal(stats.getState());
            }
            if (shouldAutoRemove) {
                streamRoutes.remove(metadata.getSessionId(), this);
            }
            return new DispatchSnapshot(context, shouldAutoRemove);
        }

        private boolean isTerminal(@NonNull StreamSessionState state) {
            return state == StreamSessionState.STOPPED
                    || state == StreamSessionState.FAILED
                    || state == StreamSessionState.CANCELED;
        }

        private final class DispatchSnapshot {
            @NonNull
            private final StreamDispatchContext context;
            private final boolean autoRemoved;

            private DispatchSnapshot(@NonNull StreamDispatchContext context, boolean autoRemoved) {
                this.context = context;
                this.autoRemoved = autoRemoved;
            }
        }
    }
}
