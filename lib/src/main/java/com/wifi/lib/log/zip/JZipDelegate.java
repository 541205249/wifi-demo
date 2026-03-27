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

import com.wifi.lib.log.JLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JZipDelegate {
    public interface Callback {
        void onComplete(@NonNull String resultMessage);

        void onError(@NonNull String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Callback callback;
    private final ActivityResultLauncher<Intent> directoryPickerLauncher;

    private JZipDelegate(
            @NonNull Context context,
            ActivityResultLauncher<Intent> directoryPickerLauncher
    ) {
        this.context = context;
        this.directoryPickerLauncher = directoryPickerLauncher;
    }

    @NonNull
    public static JZipDelegate withDirectoryPicker(@NonNull ComponentActivity activity) {
        final JZipDelegate[] holder = new JZipDelegate[1];
        ActivityResultLauncher<Intent> launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    JZipDelegate delegate = holder[0];
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
        holder[0] = new JZipDelegate(activity, launcher);
        return holder[0];
    }

    @NonNull
    public static JZipDelegate withoutDirectoryPicker(@NonNull Context context) {
        return new JZipDelegate(context, null);
    }

    public void exportToLocalDirectory(@NonNull Callback callback) {
        this.callback = callback;
        if (directoryPickerLauncher == null) {
            notifyError("日志导出组件尚未完成目录选择器注册");
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
            try {
                if (JLog.get().getLogConfig() == null || JLog.get().getLogcatCollector().getLogDirectory() == null) {
                    notifyError("JLog 尚未初始化");
                    return;
                }

                JLog.saveLogsToFile();
                String logPath = JLog.get().getLogcatCollector().getLogDirectory();
                File sourceDir = new File(logPath);
                if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                    notifyError("日志目录不存在");
                    return;
                }

                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                        .format(new Date());
                String zipFileName = "JLog_" + timestamp + ".zip";
                File zipFile = new File(logPath, zipFileName);

                JZipEncrypter.encryptZipFile(sourceDir, zipFile, JLog.get().getLogConfig().getUnzipCode());

                if (destinationUri == null) {
                    Uri shareUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".jlog.fileProvider",
                            zipFile
                    );
                    mainHandler.post(() -> shareFile(shareUri));
                    notifySuccess("已调起社交应用分享面板");
                    return;
                }

                Uri resultUri = saveFileToSelectedDirectory(zipFile, destinationUri, zipFileName);
                //noinspection ResultOfMethodCallIgnored
                zipFile.delete();
                if (resultUri == null) {
                    notifyError("保存压缩文件失败");
                    return;
                }
                notifySuccess("日志已导出到本地: " + resultUri);
            } catch (Exception e) {
                notifyError("导出日志失败: " + e.getMessage());
            }
        }).start();
    }

    private void shareFile(@NonNull Uri fileUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.setType("application/zip");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "分享日志文件"));
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
            JLog.e("JZipDelegate", "保存文件失败", e);
            return null;
        }
    }
}
