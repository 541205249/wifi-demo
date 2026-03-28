package com.wifi.lib.command.transfer;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.zip.CRC32;

public final class TransferChecksums {
    private static final int BUFFER_SIZE = 8 * 1024;

    private TransferChecksums() {
    }

    @NonNull
    public static String md5Hex(@NonNull byte[] payload) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(payload);
            return toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 MD5", exception);
        }
    }

    @NonNull
    public static String md5Hex(@NonNull File file) throws IOException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return md5Hex(inputStream);
        }
    }

    @NonNull
    public static String crc32Hex(@NonNull byte[] payload) {
        CRC32 crc32 = new CRC32();
        crc32.update(payload);
        return Long.toHexString(crc32.getValue()).toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static String md5Hex(@NonNull InputStream inputStream) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int readCount;
            while ((readCount = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, readCount);
            }
            return toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前环境不支持 MD5", exception);
        }
    }

    @NonNull
    private static String toHex(@NonNull byte[] digest) {
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }
}
