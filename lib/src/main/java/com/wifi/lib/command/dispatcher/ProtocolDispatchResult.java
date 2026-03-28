package com.wifi.lib.command.dispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.command.gateway.ProtocolPayloadType;

public final class ProtocolDispatchResult {
    @NonNull
    private final ProtocolDispatchStatus status;
    @NonNull
    private final ProtocolPayloadType payloadType;
    @NonNull
    private final String routeKey;
    @NonNull
    private final String detail;
    private final boolean autoRemoved;

    private ProtocolDispatchResult(
            @NonNull ProtocolDispatchStatus status,
            @NonNull ProtocolPayloadType payloadType,
            @Nullable String routeKey,
            @Nullable String detail,
            boolean autoRemoved
    ) {
        this.status = status;
        this.payloadType = payloadType;
        this.routeKey = routeKey == null ? "" : routeKey.trim();
        this.detail = detail == null ? "" : detail.trim();
        this.autoRemoved = autoRemoved;
    }

    @NonNull
    public static ProtocolDispatchResult handled(
            @NonNull ProtocolPayloadType payloadType,
            @Nullable String routeKey,
            @Nullable String detail,
            boolean autoRemoved
    ) {
        return new ProtocolDispatchResult(ProtocolDispatchStatus.HANDLED, payloadType, routeKey, detail, autoRemoved);
    }

    @NonNull
    public static ProtocolDispatchResult unhandled(
            @NonNull ProtocolPayloadType payloadType,
            @Nullable String routeKey,
            @Nullable String detail
    ) {
        return new ProtocolDispatchResult(ProtocolDispatchStatus.UNHANDLED, payloadType, routeKey, detail, false);
    }

    @NonNull
    public static ProtocolDispatchResult failed(
            @NonNull ProtocolPayloadType payloadType,
            @Nullable String routeKey,
            @Nullable String detail
    ) {
        return new ProtocolDispatchResult(ProtocolDispatchStatus.FAILED, payloadType, routeKey, detail, false);
    }

    @NonNull
    public ProtocolDispatchStatus getStatus() {
        return status;
    }

    @NonNull
    public ProtocolPayloadType getPayloadType() {
        return payloadType;
    }

    @NonNull
    public String getRouteKey() {
        return routeKey;
    }

    @NonNull
    public String getDetail() {
        return detail;
    }

    public boolean isAutoRemoved() {
        return autoRemoved;
    }

    public boolean isHandled() {
        return status == ProtocolDispatchStatus.HANDLED;
    }

    public boolean isUnhandled() {
        return status == ProtocolDispatchStatus.UNHANDLED;
    }

    public boolean isFailed() {
        return status == ProtocolDispatchStatus.FAILED;
    }
}
