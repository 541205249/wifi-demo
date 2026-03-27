package com.wifi.lib.log.zip;

import androidx.annotation.NonNull;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;

public final class JZipEncrypter {
    private JZipEncrypter() {
    }

    public static void encryptZipFile(@NonNull File sourceDir, @NonNull File zipFile, @NonNull String password)
            throws IOException {
        try {
            if (password.isEmpty()) {
                throw new IOException("压缩密码不能为空");
            }
            deleteExtraZipFile(sourceDir);

            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);
            parameters.setEncryptionMethod(EncryptionMethod.AES);
            parameters.setEncryptFiles(true);

            ZipFile encryptedZipFile = new ZipFile(zipFile, password.toCharArray());
            if (sourceDir.isFile()) {
                encryptedZipFile.addFile(sourceDir, parameters);
            } else {
                encryptedZipFile.addFolder(sourceDir, parameters);
            }
        } catch (Exception e) {
            throw new IOException("创建加密 ZIP 文件失败: " + e.getMessage(), e);
        }
    }

    private static void deleteExtraZipFile(@NonNull File sourceDir) {
        File[] zipFiles = sourceDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (zipFiles == null) {
            return;
        }
        for (File file : zipFiles) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
