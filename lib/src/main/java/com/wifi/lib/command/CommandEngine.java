package com.wifi.lib.command;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.DLog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 收发两端共用的命令解析引擎。
 */
public final class CommandEngine {
    private static final String TAG = "CommandEngine";
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)\\}");

    @NonNull
    private volatile CommandTable commandTable = CommandTable.empty();
    @NonNull
    private final Map<String, InboundCommandHandler> inboundHandlers = new ConcurrentHashMap<>();

    public void replaceCommandTable(@NonNull CommandTable commandTable) {
        this.commandTable = commandTable;
        DLog.i(TAG, "已替换编码表，source=" + commandTable.getSourceName() + ", size=" + commandTable.size());
    }

    @NonNull
    public CommandTable getCommandTable() {
        return commandTable;
    }

    @NonNull
    public OutboundCommand prepareOutbound(@NonNull String code) {
        return prepareOutbound(code, Collections.emptyMap());
    }

    @NonNull
    public OutboundCommand prepareOutbound(@NonNull String code, @Nullable Map<String, String> arguments) {
        CommandDefinition definition = requireDefinition(code);
        if (!definition.getDirection().supportsOutbound()) {
            throw new IllegalArgumentException("编码 " + code + " 不是发送方向");
        }
        if (!definition.isEnabled()) {
            throw new IllegalStateException("编码 " + code + " 已被禁用");
        }
        if (!definition.isOutboundConfigured()) {
            throw new IllegalStateException("编码 " + code + " 尚未配置发送命令");
        }

        Map<String, String> safeArguments = arguments == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(arguments);
        String rawMessage = resolveTemplate(definition.getSendCommand(), safeArguments);
        OutboundCommand outboundCommand = new OutboundCommand(
                definition,
                rawMessage,
                safeArguments,
                System.currentTimeMillis()
        );
        DLog.i(TAG, "发送编码解析完成，code=" + code + ", raw=" + rawMessage);
        return outboundCommand;
    }

    public void sendByCode(@NonNull String code, @NonNull CommandTransport transport) {
        sendByCode(code, Collections.emptyMap(), transport);
    }

    public void sendByCode(
            @NonNull String code,
            @Nullable Map<String, String> arguments,
            @NonNull CommandTransport transport
    ) {
        OutboundCommand outboundCommand = prepareOutbound(code, arguments);
        transport.send(outboundCommand);
        DLog.i(TAG, "发送指令已交给传输层，code=" + code);
    }

    @Nullable
    public InboundCommand resolveInbound(@Nullable String rawMessage) {
        if (TextUtils.isEmpty(rawMessage)) {
            return null;
        }
        InboundCommand inboundCommand = commandTable.matchIncoming(rawMessage);
        if (inboundCommand != null) {
            DLog.i(TAG, "接收编码解析完成，code=" + inboundCommand.getCode() + ", raw=" + rawMessage);
        } else {
            DLog.w(TAG, "未找到可匹配的接收编码，raw=" + rawMessage);
        }
        return inboundCommand;
    }

    public boolean dispatchInbound(@Nullable String rawMessage) {
        InboundCommand inboundCommand = resolveInbound(rawMessage);
        if (inboundCommand == null) {
            return false;
        }

        InboundCommandHandler handler = inboundHandlers.get(inboundCommand.getCode());
        if (handler == null) {
            DLog.w(TAG, "收到已匹配命令但未注册处理器，code=" + inboundCommand.getCode());
            return false;
        }

        handler.onCommand(inboundCommand);
        DLog.i(TAG, "接收命令已分发，code=" + inboundCommand.getCode());
        return true;
    }

    public void registerInboundHandler(@NonNull String code, @NonNull InboundCommandHandler handler) {
        String normalizedCode = CommandCode.of(code).getValue();
        inboundHandlers.put(normalizedCode, handler);
        DLog.i(TAG, "注册接收处理器，code=" + normalizedCode);
    }

    public void unregisterInboundHandler(@NonNull String code) {
        String normalizedCode = CommandCode.of(code).getValue();
        inboundHandlers.remove(normalizedCode);
        DLog.i(TAG, "移除接收处理器，code=" + normalizedCode);
    }

    @NonNull
    private CommandDefinition requireDefinition(@NonNull String code) {
        CommandDefinition definition = commandTable.findByCode(code);
        if (definition != null) {
            return definition;
        }
        throw new IllegalArgumentException("编码表中不存在指令编码: " + code);
    }

    @NonNull
    private String resolveTemplate(@NonNull String template, @NonNull Map<String, String> arguments) {
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = arguments.get(key);
            if (replacement == null) {
                throw new IllegalArgumentException("指令模板缺少参数: " + key + ", template=" + template);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
