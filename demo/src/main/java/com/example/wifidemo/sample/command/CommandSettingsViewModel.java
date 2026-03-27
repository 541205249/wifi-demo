package com.example.wifidemo.sample.command;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wifi.lib.command.CommandCatalog;
import com.wifi.lib.command.CommandDefinition;
import com.wifi.lib.command.CommandEngine;
import com.wifi.lib.command.CommandSettingsRepository;
import com.wifi.lib.command.CommandTable;
import com.wifi.lib.command.InboundCommand;
import com.wifi.lib.command.OutboundCommand;
import com.wifi.lib.command.profile.OptometryCommandCodes;
import com.wifi.lib.command.profile.OptometryCommandProfile;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseViewModel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CommandSettingsViewModel extends BaseViewModel {
    private static final String TAG = "CommandSettingsVM";
    private static final int MAX_CONSOLE_LINE_COUNT = 80;

    private final CommandSettingsRepository repository;
    private final CommandEngine commandEngine;
    private final ArrayDeque<String> consoleLines = new ArrayDeque<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final MutableLiveData<String> loadedFileLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> tableSummaryLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> validationLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> consoleLiveData = new MutableLiveData<>();

    public CommandSettingsViewModel(@NonNull Application application) {
        super(application);
        repository = CommandSettingsRepository.getInstance(application, OptometryCommandProfile.getInstance());
        commandEngine = repository.getCommandEngine();
        registerInboundHandlers();
        renderSnapshot(repository.snapshot());
        appendConsole("命令框架已初始化，等待加载编码表");
    }

    public LiveData<String> getLoadedFileLiveData() {
        return loadedFileLiveData;
    }

    public LiveData<String> getTableSummaryLiveData() {
        return tableSummaryLiveData;
    }

    public LiveData<String> getValidationLiveData() {
        return validationLiveData;
    }

    public LiveData<String> getConsoleLiveData() {
        return consoleLiveData;
    }

    @Nullable
    public Uri getLastLoadedUri() {
        return repository.getLastLoadedUri();
    }

    public void reloadLastLoadedTable() {
        try {
            CommandSettingsRepository.LoadResult loadResult = repository.reloadLast();
            if (loadResult == null) {
                dispatchMessage("还没有加载过编码表文档");
                appendConsole("未找到上次加载的编码表");
                return;
            }
            renderLoadResult(loadResult);
            dispatchMessage("已重新加载上次编码表");
        } catch (IOException exception) {
            dispatchMessage("重新加载编码表失败: " + safeMessage(exception));
            appendConsole("重新加载编码表失败: " + safeMessage(exception));
            DLog.e(TAG, "重新加载编码表失败", exception);
        }
    }

    public void loadBuiltInSample() {
        try {
            CommandSettingsRepository.LoadResult loadResult = repository.loadBuiltInSample();
            renderLoadResult(loadResult);
            dispatchMessage("已加载内置示例编码表");
        } catch (IOException exception) {
            dispatchMessage("加载内置示例失败: " + safeMessage(exception));
            appendConsole("加载内置示例失败: " + safeMessage(exception));
            DLog.e(TAG, "加载内置示例编码表失败", exception);
        }
    }

    public void loadCommandTable(@NonNull Uri uri) {
        try {
            CommandSettingsRepository.LoadResult loadResult = repository.loadFromUri(uri);
            renderLoadResult(loadResult);
            dispatchMessage("编码表加载完成");
        } catch (IOException exception) {
            dispatchMessage("加载编码表失败: " + safeMessage(exception));
            appendConsole("加载编码表失败: " + safeMessage(exception));
            DLog.e(TAG, "加载编码表失败", exception);
        } catch (IllegalArgumentException exception) {
            dispatchMessage("编码表格式错误: " + safeMessage(exception));
            appendConsole("编码表格式错误: " + safeMessage(exception));
            DLog.e(TAG, "编码表格式错误", exception);
        }
    }

    @Nullable
    public OutboundCommand prepareCommand(@NonNull String code, @Nullable Map<String, String> arguments) {
        try {
            OutboundCommand outboundCommand = commandEngine.prepareOutbound(code, arguments);
            appendConsole("已按编码 " + code + " 解析发送命令: " + outboundCommand.getRawMessage());
            return outboundCommand;
        } catch (Exception exception) {
            dispatchMessage("编码 " + code + " 发送失败: " + safeMessage(exception));
            appendConsole("编码 " + code + " 发送失败: " + safeMessage(exception));
            DLog.e(TAG, "按编码发送失败，code=" + code, exception);
            return null;
        }
    }

    public void onCommandSent(@NonNull String targetLabel, @NonNull OutboundCommand outboundCommand) {
        appendConsole("已发送到 " + targetLabel + "，编码 " + outboundCommand.getCode()
                + " -> " + outboundCommand.getRawMessage());
        DLog.i(TAG, "命令发送成功，code=" + outboundCommand.getCode() + ", target=" + targetLabel);
    }

    public void onIncomingMessage(@NonNull String clientId, @Nullable String rawMessage) {
        if (TextUtils.isEmpty(rawMessage)) {
            appendConsole("收到空消息，来源=" + clientId);
            return;
        }

        InboundCommand inboundCommand = commandEngine.resolveInbound(rawMessage);
        if (inboundCommand == null) {
            appendConsole("收到未匹配消息 [" + clientId + "]: " + rawMessage);
            return;
        }

        boolean handled = commandEngine.dispatchInbound(rawMessage);
        if (!handled) {
            appendConsole("收到编码 " + inboundCommand.getCode() + "，但未注册业务处理器");
        }
    }

    @NonNull
    public Map<String, String> createModeArgument(@NonNull String mode) {
        Map<String, String> arguments = new LinkedHashMap<>();
        arguments.put("mode", mode);
        return arguments;
    }

    private void registerInboundHandlers() {
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_REPORT_MODULE_INFO,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理模块信息上报: " + command.getRawMessage()));
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_CONFIRM_AUTO_MODE,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理自动模式切换确认: " + command.getRawMessage()));
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_CONFIRM_MANUAL_MODE,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理手动模式切换确认: " + command.getRawMessage()));
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_CONFIRM_START_OPTOMETRY,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理开始验光确认: " + command.getRawMessage()));
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_CONFIRM_STOP_OPTOMETRY,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理停止验光确认: " + command.getRawMessage()));
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_REPORT_DEVICE_STATUS,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理设备状态上报: " + command.getRawMessage()));
        commandEngine.registerInboundHandler(OptometryCommandCodes.CODE_REPORT_OPTOMETRY_RESULT,
                command -> appendConsole("已按编码 " + command.getCode() + " 处理验光结果上报: " + command.getRawMessage()));
    }

    private void renderLoadResult(@NonNull CommandSettingsRepository.LoadResult loadResult) {
        loadedFileLiveData.setValue("当前编码表: " + loadResult.getSourceLabel());
        tableSummaryLiveData.setValue(buildTableSummary(loadResult.getCommandTable()));
        validationLiveData.setValue(buildValidationSummary(loadResult.getValidationResult()));
        appendConsole("编码表加载成功，source=" + loadResult.getSourceLabel() + ", count=" + loadResult.getCommandTable().size());
        DLog.i(TAG, "命令编码表加载成功，source=" + loadResult.getSourceLabel());
    }

    private void renderSnapshot(@NonNull CommandSettingsRepository.Snapshot snapshot) {
        Uri sourceUri = snapshot.getSourceUri();
        loadedFileLiveData.setValue(sourceUri == null ? "当前编码表: 未加载" : "当前编码表: " + sourceUri);
        tableSummaryLiveData.setValue(buildTableSummary(snapshot.getCommandTable()));
        validationLiveData.setValue(buildValidationSummary(snapshot.getValidationResult()));
    }

    @NonNull
    private String buildTableSummary(@NonNull CommandTable commandTable) {
        if (commandTable.isEmpty()) {
            return "编码表状态: 未加载\n预留编码数: " + repository.getCatalog().getReservations().size();
        }

        int outboundCount = 0;
        int inboundCount = 0;
        for (CommandDefinition definition : commandTable.getDefinitions()) {
            if (definition.getDirection().supportsOutbound()) {
                outboundCount++;
            }
            if (definition.getDirection().supportsInbound()) {
                inboundCount++;
            }
        }
        return "编码表状态: 已加载"
                + "\n来源: " + commandTable.getSourceName()
                + "\n映射条数: " + commandTable.size()
                + "\n发送编码: " + outboundCount
                + "\n接收编码: " + inboundCount
                + "\n预留编码数: " + repository.getCatalog().getReservations().size();
    }

    @NonNull
    private String buildValidationSummary(@Nullable CommandCatalog.ValidationResult validationResult) {
        if (validationResult == null) {
            return "校验结果: 尚未校验";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("校验结果摘要: ").append(validationResult.buildSummary());

        if (!validationResult.getMissingCodes().isEmpty()) {
            builder.append("\n缺失编码: ").append(join(validationResult.getMissingCodes()));
        }
        if (!validationResult.getUnexpectedCodes().isEmpty()) {
            builder.append("\n多余编码: ").append(join(validationResult.getUnexpectedCodes()));
        }
        if (!validationResult.getDirectionMismatches().isEmpty()) {
            builder.append("\n方向不一致: ").append(join(validationResult.getDirectionMismatches()));
        }
        if (!validationResult.getUnconfiguredCodes().isEmpty()) {
            builder.append("\n未填命令: ").append(join(validationResult.getUnconfiguredCodes()));
        }
        if (!validationResult.hasIssues() && !validationResult.hasUnconfiguredCodes()) {
            builder.append("\n编码表结构完整，可直接联调");
        }
        return builder.toString();
    }

    private void appendConsole(@NonNull String message) {
        String line = "[" + timeFormat.format(new Date()) + "] " + message;
        consoleLines.addFirst(line);
        while (consoleLines.size() > MAX_CONSOLE_LINE_COUNT) {
            consoleLines.removeLast();
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String consoleLine : consoleLines) {
            if (!first) {
                builder.append('\n').append('\n');
            }
            builder.append(consoleLine);
            first = false;
        }
        consoleLiveData.setValue(builder.toString());
    }

    @NonNull
    private String join(@NonNull java.util.List<String> values) {
        return TextUtils.join(" | ", values);
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage()) ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
