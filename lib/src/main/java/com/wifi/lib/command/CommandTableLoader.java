package com.wifi.lib.command;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 当前默认支持 UTF-8 CSV 编码表。
 */
public final class CommandTableLoader {
    private static final String TAG = "CommandTableLoader";

    @NonNull
    public CommandTable loadFromUri(@NonNull Context context, @NonNull Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("无法打开编码表文档: " + uri);
            }
            String sourceName = uri.getLastPathSegment() == null ? uri.toString() : uri.getLastPathSegment();
            return load(inputStream, sourceName);
        }
    }

    @NonNull
    public CommandTable loadFromFile(@NonNull File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return load(inputStream, file.getName());
        }
    }

    @NonNull
    public CommandTable load(@NonNull InputStream inputStream, @NonNull String sourceName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            List<String> headerCells = null;
            Map<String, Integer> headerIndexMap = null;
            List<CommandDefinition> definitions = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String normalizedLine = stripBom(lineNumber, line).trim();
                if (normalizedLine.isEmpty() || normalizedLine.startsWith("#") || normalizedLine.startsWith("//")) {
                    continue;
                }

                List<String> cells = parseCsvLine(normalizedLine);
                if (headerCells == null) {
                    headerCells = cells;
                    headerIndexMap = buildHeaderIndexMap(headerCells);
                    continue;
                }

                if (headerIndexMap == null) {
                    throw new IllegalStateException("编码表表头解析失败");
                }

                CommandDefinition definition = buildDefinition(cells, headerIndexMap, lineNumber);
                definitions.add(definition);
            }

            if (headerCells == null || headerIndexMap == null) {
                throw new IllegalArgumentException("编码表为空，未找到表头");
            }

            CommandTable commandTable = new CommandTable(sourceName, System.currentTimeMillis(), definitions);
            DLog.i(TAG, "编码表加载成功，source=" + sourceName + ", size=" + commandTable.size());
            return commandTable;
        }
    }

    @NonNull
    private CommandDefinition buildDefinition(
            @NonNull List<String> cells,
            @NonNull Map<String, Integer> headerIndexMap,
            int lineNumber
    ) {
        String code = getCell(cells, headerIndexMap, "code");
        if (TextUtils.isEmpty(code)) {
            throw new IllegalArgumentException("第 " + lineNumber + " 行缺少编码");
        }

        String moduleName = getCell(cells, headerIndexMap, "moduleName");
        String subModuleName = getCell(cells, headerIndexMap, "subModuleName");
        String actionName = getCell(cells, headerIndexMap, "actionName");
        String codeExplanation = getCell(cells, headerIndexMap, "codeExplanation");
        if (TextUtils.isEmpty(codeExplanation)) {
            codeExplanation = moduleName + "/" + subModuleName + "/" + actionName;
        }
        String directionValue = getCell(cells, headerIndexMap, "direction");
        String sendCommand = getCell(cells, headerIndexMap, "sendCommand");
        String receiveCommand = getCell(cells, headerIndexMap, "receiveCommand");
        String receiveMatchModeValue = getCell(cells, headerIndexMap, "receiveMatchMode");
        String description = getCell(cells, headerIndexMap, "description");
        String example = getCell(cells, headerIndexMap, "example");
        String enabledValue = getCell(cells, headerIndexMap, "enabled");
        String remark = getCell(cells, headerIndexMap, "remark");

        CommandDirection direction = TextUtils.isEmpty(directionValue)
                ? inferDirection(sendCommand, receiveCommand)
                : CommandDirection.fromValue(directionValue);

        CommandMatchMode receiveMatchMode = TextUtils.isEmpty(receiveMatchModeValue)
                ? CommandMatchMode.EXACT
                : CommandMatchMode.fromValue(receiveMatchModeValue);

        boolean enabled = parseEnabled(enabledValue);
        return new CommandDefinition(
                code,
                moduleName,
                subModuleName,
                actionName,
                codeExplanation,
                direction,
                sendCommand,
                receiveCommand,
                receiveMatchMode,
                description,
                example,
                enabled,
                remark,
                lineNumber
        );
    }

    @NonNull
    private Map<String, Integer> buildHeaderIndexMap(@NonNull List<String> headerCells) {
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
        for (int index = 0; index < headerCells.size(); index++) {
            String key = aliasHeader(headerCells.get(index));
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            headerIndexMap.put(key, index);
        }
        if (!headerIndexMap.containsKey("code")) {
            throw new IllegalArgumentException("编码表缺少必填表头: 编码/code");
        }
        return headerIndexMap;
    }

    @NonNull
    private String getCell(
            @NonNull List<String> cells,
            @NonNull Map<String, Integer> headerIndexMap,
            @NonNull String key
    ) {
        Integer index = headerIndexMap.get(key);
        if (index == null || index < 0 || index >= cells.size()) {
            return "";
        }
        return cells.get(index).trim();
    }

    @NonNull
    private String aliasHeader(@NonNull String header) {
        String normalized = normalizeHeader(header);
        Map<String, String> aliases = new HashMap<>();
        aliases.put("code", "code");
        aliases.put("编码", "code");
        aliases.put("编号", "code");
        aliases.put("指令编码", "code");

        aliases.put("modulename", "moduleName");
        aliases.put("模块", "moduleName");
        aliases.put("大模块", "moduleName");
        aliases.put("大模块名称", "moduleName");

        aliases.put("submodulename", "subModuleName");
        aliases.put("submodule", "subModuleName");
        aliases.put("子模块", "subModuleName");
        aliases.put("子模块名称", "subModuleName");

        aliases.put("actionname", "actionName");
        aliases.put("action", "actionName");
        aliases.put("动作", "actionName");
        aliases.put("动作名称", "actionName");

        aliases.put("direction", "direction");
        aliases.put("方向", "direction");

        aliases.put("codeexplanation", "codeExplanation");
        aliases.put("编号解释", "codeExplanation");
        aliases.put("编码解释", "codeExplanation");
        aliases.put("编号说明", "codeExplanation");
        aliases.put("编码说明", "codeExplanation");

        aliases.put("sendcommand", "sendCommand");
        aliases.put("send", "sendCommand");
        aliases.put("发送命令", "sendCommand");
        aliases.put("发送指令", "sendCommand");

        aliases.put("receivecommand", "receiveCommand");
        aliases.put("receive", "receiveCommand");
        aliases.put("接收命令", "receiveCommand");
        aliases.put("接收指令", "receiveCommand");

        aliases.put("receivematchmode", "receiveMatchMode");
        aliases.put("matchmode", "receiveMatchMode");
        aliases.put("匹配方式", "receiveMatchMode");
        aliases.put("接收匹配方式", "receiveMatchMode");

        aliases.put("description", "description");
        aliases.put("描述", "description");
        aliases.put("说明", "description");

        aliases.put("example", "example");
        aliases.put("示例", "example");

        aliases.put("enabled", "enabled");
        aliases.put("启用", "enabled");
        aliases.put("是否启用", "enabled");

        aliases.put("remark", "remark");
        aliases.put("备注", "remark");

        String canonical = aliases.get(normalized);
        return canonical == null ? "" : canonical;
    }

    @NonNull
    private String normalizeHeader(@NonNull String header) {
        return header
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("\uFEFF", "")
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    @NonNull
    private List<String> parseCsvLine(@NonNull String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cellBuilder = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    cellBuilder.append('"');
                    index++;
                    continue;
                }
                inQuotes = !inQuotes;
                continue;
            }
            if (current == ',' && !inQuotes) {
                cells.add(cellBuilder.toString());
                cellBuilder.setLength(0);
                continue;
            }
            cellBuilder.append(current);
        }
        cells.add(cellBuilder.toString());
        return cells;
    }

    @NonNull
    private String stripBom(int lineNumber, @NonNull String line) {
        if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private boolean parseEnabled(@NonNull String rawValue) {
        if (TextUtils.isEmpty(rawValue)) {
            return true;
        }
        String normalized = rawValue
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace(" ", "");
        return "1".equals(normalized)
                || "true".equals(normalized)
                || "yes".equals(normalized)
                || "y".equals(normalized)
                || "是".equals(normalized)
                || "启用".equals(normalized)
                || "开启".equals(normalized);
    }

    @NonNull
    private CommandDirection inferDirection(@NonNull String sendCommand, @NonNull String receiveCommand) {
        boolean hasSend = !TextUtils.isEmpty(sendCommand);
        boolean hasReceive = !TextUtils.isEmpty(receiveCommand);
        if (hasSend && hasReceive) {
            return CommandDirection.BIDIRECTIONAL;
        }
        if (hasSend) {
            return CommandDirection.OUTBOUND;
        }
        if (hasReceive) {
            return CommandDirection.INBOUND;
        }
        return CommandDirection.BIDIRECTIONAL;
    }
}
