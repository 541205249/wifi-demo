package com.wifi.lib.log.logcat;

import java.io.File;

public final class JLogcatFileUtil {
    private JLogcatFileUtil() {
    }

    public static void manageLogCount(String logDirectory, int daysToKeep) {
        File dir = new File(logDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        deleteExpiredFiles(dir, cutoffTime);
        deleteEmptyDirectories(dir, false);
    }

    private static void deleteExpiredFiles(File directory, long cutoffTime) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                deleteExpiredFiles(file, cutoffTime);
            } else if (file.lastModified() < cutoffTime) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    private static boolean deleteEmptyDirectories(File directory, boolean deleteSelf) {
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }

        boolean hasChildren = false;
        for (File file : files) {
            if (file.isDirectory()) {
                boolean deleted = deleteEmptyDirectories(file, true);
                if (!deleted) {
                    hasChildren = true;
                }
            } else {
                hasChildren = true;
            }
        }

        if (deleteSelf && !hasChildren) {
            return directory.delete();
        }
        return false;
    }
}
