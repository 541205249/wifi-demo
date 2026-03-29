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
    private static final String[] CSV_HEADER = {"编码", "指令", "编号解释"};

    public void exportTemplate(@NonNull CommandCatalog catalog, @NonNull OutputStream outputStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writeCsvLine(writer, CSV_HEADER);

            for (CommandReservation reservation : catalog.getReservations()) {
                writeCsvLine(
                        writer,
                        reservation.getCodeValue(),
                        "",
                        reservation.getCodeExplanation()
                );
            }
            writer.flush();
        }
        DLog.i(TAG, "编码表模板导出完成，count=" + catalog.getReservations().size());
    }

    private void writeCsvLine(@NonNull Writer writer, @NonNull String... values) throws IOException {
        writer.write(buildCsvLine(values));
        writer.write('\n');
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
