package com.example.wifidemo.sample.data;

import com.wifi.lib.mvvm.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DemoRepository extends BaseRepository {
    private static volatile DemoRepository instance;

    private final List<String> records = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private DemoRepository() {
        records.add(formatRecord("基础库已接入，准备演示 MVVM + Repository。"));
        records.add(formatRecord("页面可直接复用 BaseVBActivity / BaseVBFragment。"));
        records.add(formatRecord("Loading、权限、Dialog、BottomSheet 已统一下沉到 lib。"));
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
        return new ArrayList<>(records);
    }

    public synchronized void appendRecord(String message) {
        records.add(0, formatRecord(message));
    }

    public synchronized int size() {
        return records.size();
    }

    private String formatRecord(String message) {
        return "[" + timeFormat.format(new Date()) + "] " + message;
    }
}
