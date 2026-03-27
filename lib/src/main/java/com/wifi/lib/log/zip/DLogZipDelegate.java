package com.wifi.lib.log.zip;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;
import com.wifi.lib.log.JLogConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DLogZipDelegate {
    public interface Callback {
        void onComplete(@NonNull String resultMessage);

        void onError(@NonNull String errorMessage);
    }

    private static final Pattern LOG_HEADER_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} [VDIWE]/([^:]+):.*$"
    );

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private final ActivityResultLauncher<Intent> directoryPickerLauncher;

    private DLogZipDelegate(
            @NonNull Context context,
            ActivityResultLauncher<Intent> directoryPickerLauncher
    ) {
        this.context = context;
        this.directoryPickerLauncher = directoryPickerLauncher;
    }

    @NonNull
    public static DLogZipDelegate withDirectoryPicker(@NonNull ComponentActivity activity) {
        final DLogZipDelegate[] holder = new DLogZipDelegate[1];
        ActivityResultLauncher<Intent> launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    DLogZipDelegate delegate = holder[0];
                    if (delegate == null) {
                        return;
                    }
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedUri = result.getData().getData();
                        delegate.performCompression(selectedUri);
                    } else if (delegate.callback != null) {
                        delegate.notifyError("未选择导出目录");
                    }
                }
        );
        holder[0] = new DLogZipDelegate(activity, launcher);
        return holder[0];
    }

    @NonNull
    public static DLogZipDelegate withoutDirectoryPicker(@NonNull Context context) {
        return new DLogZipDelegate(context, null);
    }

    public void exportToLocalDirectory(@NonNull Callback callback) {
        this.callback = callback;
        if (directoryPickerLauncher == null) {
            notifyError("DLog 导出组件尚未完成目录选择器注册");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        directoryPickerLauncher.launch(intent);
    }

    public void shareToSocialApp(@NonNull Callback callback) {
        this.callback = callback;
        performCompression(null);
    }

    private void performCompression(Uri destinationUri) {
        new Thread(() -> {
            File workingDir = null;
            File zipFile = null;
            try {
                JLogConfig logConfig = DLog.getConfig();
                if (logConfig == null || JLog.get().getLogcatCollector().getLogDirectory() == null) {
                    notifyError("DLog 尚未初始化");
                    return;
                }

                DLog.saveLogsToFile();
                File sourceDir = new File(JLog.get().getLogcatCollector().getLogDirectory());
                if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                    notifyError("日志目录不存在");
                    return;
                }

                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                        .format(new Date());
                workingDir = new File(context.getCacheDir(), "dlog_export_" + timestamp);
                if (!workingDir.exists() && !workingDir.mkdirs()) {
                    notifyError("创建 DLog 导出目录失败");
                    return;
                }

                File filteredFile = new File(workingDir, "DLog_" + timestamp + ".txt");
                writeFilteredLogs(sourceDir, filteredFile, timestamp);

                cleanupOldZipFiles(context.getCacheDir());
                zipFile = new File(context.getCacheDir(), "DLog_" + timestamp + ".zip");
                JZipEncrypter.encryptZipFile(workingDir, zipFile, logConfig.getUnzipCode());

                if (destinationUri == null) {
                    Uri shareUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".jlog.fileProvider",
                            zipFile
                    );
                    mainHandler.post(() -> shareFile(shareUri));
                    notifySuccess("已调起 DLog 分享面板");
                    return;
                }

                Uri resultUri = saveFileToSelectedDirectory(zipFile, destinationUri, zipFile.getName());
                if (resultUri == null) {
                    notifyError("保存 DLog 压缩文件失败");
                    return;
                }
                notifySuccess("DLog 已导出到本地: " + resultUri);
            } catch (Exception e) {
                notifyError("导出 DLog 失败: " + e.getMessage());
            } finally {
                deleteQuietly(workingDir);
                if (destinationUri != null) {
                    deleteQuietly(zipFile);
                }
            }
        }).start();
    }

    private void writeFilteredLogs(@NonNull File sourceDir, @NonNull File outputFile, @NonNull String timestamp)
            throws IOException {
        List<File> textFiles = collectTextFiles(sourceDir);
        boolean hasAnyDLog = false;
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            writer.write("# DLog Export");
            writer.newLine();
            writer.write("# generatedAt=" + timestamp);
            writer.newLine();
            writer.write("# tag=" + DLog.TAG);
            writer.newLine();
            writer.newLine();

            for (File textFile : textFiles) {
                List<String> filteredLines = extractDLogLines(textFile);
                if (filteredLines.isEmpty()) {
                    continue;
                }
                hasAnyDLog = true;
                writer.write("=== source: " + textFile.getAbsolutePath() + " ===");
                writer.newLine();
                for (String line : filteredLines) {
                    writer.write(line);
                    writer.newLine();
                }
                writer.newLine();
            }

            if (!hasAnyDLog) {
                writer.write("暂无 DLog 日志。");
                writer.newLine();
            }
        }
    }

    @NonNull
    private List<File> collectTextFiles(@NonNull File directory) {
        List<File> files = new ArrayList<>();
        collectTextFiles(directory, files);
        files.sort(Comparator.comparingLong(File::lastModified).thenComparing(File::getAbsolutePath));
        return files;
    }

    private void collectTextFiles(@NonNull File directory, @NonNull List<File> result) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collectTextFiles(file, result);
            } else if (file.getName().toLowerCase(Locale.US).endsWith(".txt")) {
                result.add(file);
            }
        }
    }

    @NonNull
    private List<String> extractDLogLines(@NonNull File textFile) throws IOException {
        List<String> result = new ArrayList<>();
        boolean capture = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(textFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LOG_HEADER_PATTERN.matcher(line);
                if (matcher.matches()) {
                    capture = DLog.TAG.equals(matcher.group(1));
                    if (capture) {
                        result.add(line);
                    }
                    continue;
                }
                if (capture) {
                    result.add(line);
                }
            }
        }
        return result;
    }

    private void cleanupOldZipFiles(@NonNull File cacheDir) {
        File[] oldZipFiles = cacheDir.listFiles((dir, name) ->
                name.startsWith("DLog_") && name.toLowerCase(Locale.US).endsWith(".zip"));
        if (oldZipFiles == null) {
            return;
        }
        List<File> files = new ArrayList<>();
        Collections.addAll(files, oldZipFiles);
        files.sort(Comparator.comparingLong(File::lastModified).reversed());
        for (int index = 3; index < files.size(); index++) {
            deleteQuietly(files.get(index));
        }
    }

    private void shareFile(@NonNull Uri fileUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.setType("application/zip");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "分享 DLog 文件"));
    }

    private void notifySuccess(@NonNull String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onComplete(message));
        }
    }

    private void notifyError(@NonNull String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }

    private Uri saveFileToSelectedDirectory(
            @NonNull File file,
            @NonNull Uri destinationUri,
            @NonNull String fileName
    ) {
        try {
            DocumentFile documentDir = DocumentFile.fromTreeUri(context, destinationUri);
            if (documentDir == null || !documentDir.exists() || !documentDir.canWrite()) {
                return null;
            }

            DocumentFile documentFile = documentDir.createFile("application/zip", fileName);
            if (documentFile == null) {
                return null;
            }

            try (FileInputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = context.getContentResolver().openOutputStream(documentFile.getUri())) {
                if (outputStream == null) {
                    return null;
                }
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return documentFile.getUri();
        } catch (Exception e) {
            JLog.e("DLogZipDelegate", "保存 DLog 文件失败", e);
            return null;
        }
    }

    private void deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteQuietly(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
