package com.example.wifidemo.sample.data;

import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DemoRepository extends BaseRepository {
    private static final String TAG = "DemoRepository";
    private static volatile DemoRepository instance;

    private final List<String> records = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private DemoRepository() {
        seedInitialRecords();
        DLog.i(TAG, "演示仓库初始化完成，records=" + records.size());
    }

    public static DemoRepository getInstance() {
        if (instance == null) {
            synchronized (DemoRepository.class) {
                if (instance == null) {
                    instance = new DemoRepository();
                }
            }
        }
        return instance;
    }

    public synchronized List<String> snapshot() {
        DLog.d(TAG, "读取演示记录快照，count=" + records.size());
        return new ArrayList<>(records);
    }

    public synchronized void appendRecord(String message) {
        String normalizedMessage = normalizeMessage(message);
        records.add(0, formatRecord(normalizedMessage));
        DLog.i(TAG, "写入演示记录，message=" + normalizedMessage);
    }

    public synchronized int size() {
        return records.size();
    }

    private String formatRecord(String message) {
        return "[" + timeFormat.format(new Date()) + "] " + message;
    }

    private void seedInitialRecords() {
        addSeedRecord("基础库已接入，准备演示 MVVM + Repository。");
        addSeedRecord("页面可直接复用 BaseVBActivity / BaseVBFragment。");
        addSeedRecord("Loading、权限、Dialog、BottomSheet 已统一下沉到 lib。");
    }

    private void addSeedRecord(String message) {
        records.add(formatRecord(message));
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.trim();
    }
}

