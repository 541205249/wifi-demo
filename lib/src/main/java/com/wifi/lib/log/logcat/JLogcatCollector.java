package com.wifi.lib.log.logcat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.log.JLog;
import com.wifi.lib.log.JLogConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JLogcatCollector {
    private static final String TAG = "JLogcatCollector";
    private static final String PERSISTENT_LOG_FILE = "persistent_log_buffer.dat";
    private static final String PERSISTENT_LOG_TEMP_FILE = "persistent_log_buffer.tmp";
    private static final int MAX_PERSISTENT_LOG_COUNT = 10000;
    private static final int PERSISTENT_SAVE_INTERVAL = 200;
    private static final int SPILL_FLUSH_LINE_LIMIT = 200;
    private static final long SPILL_FLUSH_BYTE_LIMIT = 128 * 1024L;
    private static final String PATTERN_CRASH_TIMESTAMP = "yyyy-MM-dd_HH-mm-ss";
    private static final String PATTERN_LOG_LINE_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String PATTERN_LOG_DATE = "yyyy-MM-dd";
    private static final String PATTERN_LOG_TIME = "HH-mm-ss";

    private final List<String> logBuffer = new ArrayList<>();
    private final List<String> spilledLogBuffer = new ArrayList<>();
    private final Object bufferLock = new Object();
    private final Object fileWriteLock = new Object();
    private final Object persistentFileLock = new Object();

    private String logDirectory;
    private File persistentLogFile;
    private long currentBufferSize;
    private long spilledBufferSize;
    private int pendingPersistentWrites;
    private boolean started;
    private boolean crashMonitorRegistered;
    private Thread.UncaughtExceptionHandler defaultHandler;

    public synchronized void start(@NonNull JLogConfig config) {
        logDirectory = config.getLogDirectoryPath();
        if (config.isSaveLogEnable()) {
            File logDir = new File(logDirectory);
            if (!logDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                logDir.mkdirs();
            }
            persistentLogFile = new File(logDir, PERSISTENT_LOG_FILE);

            if (!started) {
                loadPersistentLogs();
            }
        } else {
            persistentLogFile = null;
            clearRuntimeBuffers();
        }
        started = true;

        if (config.isMonitorCrashLog()) {
            if (!crashMonitorRegistered) {
                crashMonitorRegistered = true;
                registerCrashMonitor();
            }
        } else if (crashMonitorRegistered) {
            unregisterCrashMonitor();
        }
    }

    public void record(int priority, @NonNull String tag, @Nullable String message, @Nullable Throwable throwable) {
        JLogConfig config = JLog.get().getLogConfig();
        if (config == null || !config.isSaveLogEnable()) {
            return;
        }
        if (!started) {
            start(config);
        }

        List<String> spilledLogsToFlush = null;
        boolean shouldPersist;
        synchronized (bufferLock) {
            appendLineLocked(formatLogLine(priorityToLevel(priority), tag, message, throwable), config);
            if (shouldFlushSpilledLogsLocked()) {
                spilledLogsToFlush = drainSpilledLogsLocked();
            }
            pendingPersistentWrites++;
            shouldPersist = pendingPersistentWrites >= PERSISTENT_SAVE_INTERVAL;
            if (shouldPersist) {
                pendingPersistentWrites = 0;
            }
        }

        flushLogsToFile(spilledLogsToFlush);
        if (shouldPersist) {
            savePersistentLogs();
        }
    }

    public void saveLogsToFile() {
        JLogConfig config = JLog.get().getLogConfig();
        if (config == null || !config.isSaveLogEnable()) {
            return;
        }

        flushSpilledLogs(true);
        List<String> snapshot;
        synchronized (bufferLock) {
            snapshot = new ArrayList<>(logBuffer);
        }

        synchronized (fileWriteLock) {
            try {
                writeLogsToFile(snapshot, config.getLogDirectoryPath(), config);
                JLogcatFileUtil.manageLogCount(config.getLogDirectoryPath(), config.getMaxLogStorageDays());
            } catch (IOException e) {
                android.util.Log.e(TAG, "Failed to save logs to file", e);
            }
        }
    }

    public void stop() {
        flushSpilledLogs(true);
        savePersistentLogs();
        synchronized (this) {
            started = false;
            if (crashMonitorRegistered) {
                unregisterCrashMonitor();
            }
        }
    }

    @Nullable
    public String getLogDirectory() {
        return logDirectory;
    }

    private void registerCrashMonitor() {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            saveCrashLog(throwable);
            saveLogsToFile();
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }

    private void unregisterCrashMonitor() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
        defaultHandler = null;
        crashMonitorRegistered = false;
    }

    private void saveCrashLog(@NonNull Throwable throwable) {
        JLogConfig config = JLog.get().getLogConfig();
        if (config == null) {
            return;
        }

        try {
            File crashDir = new File(config.getLogDirectoryPath(), "crash_logs");
            if (!crashDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                crashDir.mkdirs();
            }

            String timestamp = formatNow(PATTERN_CRASH_TIMESTAMP);
            File crashFile = new File(crashDir, "crash_" + timestamp + ".txt");
            List<String> recentLogs;
            synchronized (bufferLock) {
                recentLogs = new ArrayList<>(logBuffer);
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(crashFile), StandardCharsets.UTF_8))) {
                writer.write("=== Recent Logs Before Crash ===");
                writer.newLine();
                for (String logLine : recentLogs) {
                    writer.write(logLine);
                    writer.newLine();
                }
                writer.write("=== Crash Information ===");
                writer.newLine();
                writer.write(buildThrowableText(throwable));
                writer.newLine();
            }
        } catch (Exception ignored) {
            // ignore crash logging failures
        }
    }

    private void appendLineLocked(@NonNull String line, @NonNull JLogConfig config) {
        long lineSize = getLineSize(line);
        long memoryLimit = getMemoryLimitBytes(config);
        if (currentBufferSize + lineSize > memoryLimit) {
            long excessBytes = (currentBufferSize + lineSize) - memoryLimit;
            long removedBytes = 0;
            while (removedBytes < excessBytes && !logBuffer.isEmpty()) {
                String oldestLog = logBuffer.remove(0);
                long oldestLogSize = getLineSize(oldestLog);
                currentBufferSize -= oldestLogSize;
                removedBytes += oldestLogSize;
                spilledLogBuffer.add(oldestLog);
                spilledBufferSize += oldestLogSize;
            }
        }

        logBuffer.add(line);
        currentBufferSize += lineSize;
    }

    private void flushSpilledLogs(boolean force) {
        List<String> logsToFlush;
        synchronized (bufferLock) {
            if (!force && !shouldFlushSpilledLogsLocked()) {
                return;
            }
            logsToFlush = drainSpilledLogsLocked();
        }
        flushLogsToFile(logsToFlush);
    }

    private void flushLogsToFile(@Nullable List<String> logLines) {
        JLogConfig config = JLog.get().getLogConfig();
        if (config == null || !config.isSaveLogEnable() || logLines == null || logLines.isEmpty()) {
            return;
        }

        synchronized (fileWriteLock) {
            try {
                writeLogsToFile(logLines, config.getLogDirectoryPath(), config);
                JLogcatFileUtil.manageLogCount(config.getLogDirectoryPath(), config.getMaxLogStorageDays());
            } catch (IOException e) {
                restoreSpilledLogs(logLines);
                android.util.Log.e(TAG, "Failed to flush spilled logs", e);
            }
        }
    }

    private void savePersistentLogs() {
        if (persistentLogFile == null) {
            return;
        }

        List<String> logsToPersist;
        long persistedBufferSize = 0;

        synchronized (bufferLock) {
            int actualSaveCount = Math.min(logBuffer.size(), MAX_PERSISTENT_LOG_COUNT);
            int startIndex = Math.max(0, logBuffer.size() - actualSaveCount);
            logsToPersist = new ArrayList<>(logBuffer.subList(startIndex, logBuffer.size()));
            for (String logLine : logsToPersist) {
                persistedBufferSize += getLineSize(logLine);
            }
        }

        synchronized (persistentFileLock) {
            File tempFile = new File(persistentLogFile.getParentFile(), PERSISTENT_LOG_TEMP_FILE);
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
                writer.write(Integer.toString(logsToPersist.size()));
                writer.newLine();
                writer.write(Long.toString(persistedBufferSize));
                writer.newLine();
                for (String logLine : logsToPersist) {
                    writer.write(logLine);
                    writer.newLine();
                }
                writer.flush();
            } catch (IOException e) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                android.util.Log.e(TAG, "Failed to save persistent logs", e);
                return;
            }

            try {
                replacePersistentFile(tempFile, persistentLogFile);
            } catch (IOException e) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
                android.util.Log.e(TAG, "Failed to replace persistent log file", e);
            }
        }
    }

    private void loadPersistentLogs() {
        if (persistentLogFile == null || !persistentLogFile.exists()) {
            return;
        }

        List<String> savedLogs = new ArrayList<>();
        long calculatedBufferSize = 0;
        synchronized (persistentFileLock) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(persistentLogFile), StandardCharsets.UTF_8))) {
                String countLine = reader.readLine();
                String sizeLine = reader.readLine();
                if (countLine == null || sizeLine == null) {
                    throw new IOException("Persistent log file is truncated");
                }

                int expectedCount = Integer.parseInt(countLine.trim());
                if (expectedCount < 0 || expectedCount > MAX_PERSISTENT_LOG_COUNT) {
                    throw new IOException("Persistent log count is invalid");
                }
                Long.parseLong(sizeLine.trim());

                for (int i = 0; i < expectedCount; i++) {
                    String logLine = reader.readLine();
                    if (logLine == null) {
                        throw new IOException("Persistent log file ended early");
                    }
                    savedLogs.add(logLine);
                    calculatedBufferSize += getLineSize(logLine);
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to load persistent logs", e);
                //noinspection ResultOfMethodCallIgnored
                persistentLogFile.delete();
                return;
            }
        }

        synchronized (bufferLock) {
            logBuffer.clear();
            logBuffer.addAll(savedLogs);
            currentBufferSize = calculatedBufferSize;
        }
    }

    private boolean shouldFlushSpilledLogsLocked() {
        return spilledLogBuffer.size() >= SPILL_FLUSH_LINE_LIMIT || spilledBufferSize >= SPILL_FLUSH_BYTE_LIMIT;
    }

    @NonNull
    private List<String> drainSpilledLogsLocked() {
        if (spilledLogBuffer.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> drainedLogs = new ArrayList<>(spilledLogBuffer);
        spilledLogBuffer.clear();
        spilledBufferSize = 0;
        return drainedLogs;
    }

    private void restoreSpilledLogs(@NonNull List<String> logLines) {
        synchronized (bufferLock) {
            spilledLogBuffer.addAll(0, logLines);
            for (String line : logLines) {
                spilledBufferSize += getLineSize(line);
            }
        }
    }

    private void clearRuntimeBuffers() {
        synchronized (bufferLock) {
            logBuffer.clear();
            spilledLogBuffer.clear();
            currentBufferSize = 0L;
            spilledBufferSize = 0L;
            pendingPersistentWrites = 0;
        }
    }

    @NonNull
    private static String formatLogLine(
            @NonNull String level,
            @NonNull String tag,
            @Nullable String message,
            @Nullable Throwable throwable
    ) {
        String timestamp = formatNow(PATTERN_LOG_LINE_TIMESTAMP);
        StringBuilder builder = new StringBuilder()
                .append(timestamp)
                .append(" ")
                .append(level)
                .append("/")
                .append(tag)
                .append(": ")
                .append(message == null ? "" : message);
        if (throwable != null) {
            builder.append('\n').append(buildThrowableText(throwable));
        }
        return builder.toString();
    }

    @NonNull
    private static String buildThrowableText(@NonNull Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }

    private static long getLineSize(@NonNull String line) {
        return (line + "\n").getBytes(StandardCharsets.UTF_8).length;
    }

    @NonNull
    private static String formatNow(@NonNull String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date());
    }

    private static long getMemoryLimitBytes(@NonNull JLogConfig config) {
        return Math.max(1L, (long) (config.getMemoryBufferLimit() * 1024 * 1024));
    }

    private static void writeLogsToFile(
            @Nullable List<String> logLines,
            @NonNull String logDirectory,
            @NonNull JLogConfig config
    ) throws IOException {
        if (logLines == null || logLines.isEmpty()) {
            return;
        }

        long singleFileSizeLimit = Math.max(1L, (long) (config.getSingleFileSizeLimit() * 1024 * 1024));
        int dailyFilesLimit = Math.max(1, config.getDailyFilesLimit());

        String dateStr = formatNow(PATTERN_LOG_DATE);
        File dateDir = new File(logDirectory, dateStr);
        if (!dateDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dateDir.mkdirs();
        }

        File[] existingFiles = dateDir.listFiles((dir, name) -> name.startsWith("log_") && name.endsWith(".txt"));
        List<File> logFiles = existingFiles != null ? new ArrayList<>(Arrays.asList(existingFiles)) : new ArrayList<>();
        Collections.sort(logFiles, (file1, file2) -> file1.getName().compareTo(file2.getName()));

        String timeStr = formatNow(PATTERN_LOG_TIME);
        String baseFileName = "log_" + timeStr;
        int fileIndex = 0;
        File logFile = createNextLogFile(dateDir, baseFileName, fileIndex, logFiles, dailyFilesLimit);
        long currentFileSize = 0;
        int writtenLines = 0;
        BufferedWriter currentWriter = null;

        try {
            currentWriter = createWriter(logFile);
            for (String logLine : logLines) {
                String lineWithNewLine = logLine + "\n";
                byte[] lineBytes = lineWithNewLine.getBytes(StandardCharsets.UTF_8);
                if (currentFileSize + lineBytes.length > singleFileSizeLimit && writtenLines > 0) {
                    closeQuietly(currentWriter);
                    fileIndex++;
                    logFile = createNextLogFile(dateDir, baseFileName, fileIndex, logFiles, dailyFilesLimit);
                    currentWriter = createWriter(logFile);
                    currentFileSize = 0;
                    writtenLines = 0;
                }
                currentWriter.write(lineWithNewLine);
                currentFileSize += lineBytes.length;
                writtenLines++;
            }
            if (currentWriter != null) {
                currentWriter.flush();
            }
        } finally {
            closeQuietly(currentWriter);
        }
    }

    @NonNull
    private static File createNextLogFile(
            @NonNull File dateDir,
            @NonNull String baseFileName,
            int fileIndex,
            @NonNull List<File> existingLogFiles,
            int dailyFilesLimit
    ) {
        while (existingLogFiles.size() >= dailyFilesLimit) {
            File oldestFile = existingLogFiles.remove(0);
            if (!oldestFile.delete()) {
                android.util.Log.w(TAG, "Failed to delete oldest log file: " + oldestFile.getAbsolutePath());
                break;
            }
        }

        File logFile = buildLogFile(dateDir, baseFileName, fileIndex);
        int actualIndex = fileIndex;
        while (logFile.exists()) {
            actualIndex++;
            logFile = buildLogFile(dateDir, baseFileName, actualIndex);
        }
        existingLogFiles.add(logFile);
        Collections.sort(existingLogFiles, (file1, file2) -> file1.getName().compareTo(file2.getName()));
        return logFile;
    }

    @NonNull
    private static File buildLogFile(@NonNull File dateDir, @NonNull String baseFileName, int fileIndex) {
        if (fileIndex <= 0) {
            return new File(dateDir, baseFileName + ".txt");
        }
        return new File(dateDir, baseFileName + "_" + fileIndex + ".txt");
    }

    @NonNull
    private static BufferedWriter createWriter(@NonNull File file) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
    }

    private static void replacePersistentFile(@NonNull File sourceFile, @NonNull File targetFile) throws IOException {
        if (targetFile.exists() && !targetFile.delete()) {
            throw new IOException("Failed to replace old persistent log file");
        }
        if (!sourceFile.renameTo(targetFile)) {
            throw new IOException("Failed to replace persistent log file");
        }
    }

    private static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // ignore
        }
    }

    @NonNull
    private static String priorityToLevel(int priority) {
        switch (priority) {
            case android.util.Log.DEBUG:
                return "D";
            case android.util.Log.INFO:
                return "I";
            case android.util.Log.WARN:
                return "W";
            case android.util.Log.ERROR:
                return "E";
            default:
                return "V";
        }
    }
}
