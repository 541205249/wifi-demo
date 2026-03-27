package com.wifi.lib.command;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * 根据 App 侧预留编码导出 CSV 模板，交给设备端填写真实命令。
 */
public final class CommandTableCsvExporter {
    private static final String TAG = "CommandTableExporter";

    public void exportTemplate(@NonNull CommandCatalog catalog, @NonNull OutputStream outputStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(buildCsvHeader());
            writer.write('\n');

            for (CommandReservation reservation : catalog.getReservations()) {
                writer.write(buildCsvLine(
                        reservation.getCodeValue(),
                        reservation.getCodeExplanation(),
                        reservation.getModuleName(),
                        reservation.getSubModuleName(),
                        reservation.getActionName(),
                        reservation.getDirection().getLabel(),
                        "",
                        "",
                        CommandMatchMode.EXACT.getLabel(),
                        reservation.getDescription(),
                        "",
                        "是",
                        ""
                ));
                writer.write('\n');
            }
            writer.flush();
        }
        DLog.i(TAG, "编码表模板导出完成，count=" + catalog.getReservations().size());
    }

    @NonNull
    private String buildCsvHeader() {
        return buildCsvLine(
                "编码",
                "编号解释",
                "大模块",
                "子模块",
                "动作",
                "方向",
                "发送命令",
                "接收命令",
                "接收匹配方式",
                "描述",
                "示例",
                "启用",
                "备注"
        );
    }

    @NonNull
    private String buildCsvLine(@NonNull String... values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(escape(values[index]));
        }
        return builder.toString();
    }

    @NonNull
    private String escape(@NonNull String value) {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
