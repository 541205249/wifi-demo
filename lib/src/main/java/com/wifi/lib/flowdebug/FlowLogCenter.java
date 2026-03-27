package com.wifi.lib.flowdebug;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.wifi.lib.log.DLog;
import com.wifi.lib.log.JLog;
import com.wifi.lib.log.JLogEntry;
import com.wifi.lib.log.JLogObserver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class FlowLogCenter {
    private static final int MAX_BUFFER_SIZE = 200;
    private static final FlowLogCenter INSTANCE = new FlowLogCenter();

    public interface Listener {
        void onLogsChanged(@NonNull List<JLogEntry> entries);
    }

    private final Object lock = new Object();
    private final ArrayDeque<JLogEntry> buffer = new ArrayDeque<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final JLogObserver observer = entry -> {
        if (TextUtils.equals(DLog.TAG, entry.getTag())) {
            append(entry);
        }
    };

    private volatile boolean started;

    private FlowLogCenter() {
    }

    public static FlowLogCenter getInstance() {
        return INSTANCE;
    }

    public void start() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            JLog.addObserver(observer);
            started = true;
        }
    }

    public void addListener(@NonNull Listener listener) {
        listeners.addIfAbsent(listener);
        notifyListener(listener, snapshot());
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void clear() {
        synchronized (lock) {
            buffer.clear();
        }
        notifyListeners();
    }

    @NonNull
    public List<JLogEntry> snapshot() {
        synchronized (lock) {
            return new ArrayList<>(buffer);
        }
    }

    private void append(@NonNull JLogEntry entry) {
        synchronized (lock) {
            buffer.addFirst(entry);
            while (buffer.size() > MAX_BUFFER_SIZE) {
                buffer.removeLast();
            }
        }
        notifyListeners();
    }

    private void notifyListeners() {
        List<JLogEntry> snapshot = snapshot();
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onLogsChanged(snapshot);
            }
        });
    }

    private void notifyListener(@NonNull Listener listener, @NonNull List<JLogEntry> snapshot) {
        mainHandler.post(() -> listener.onLogsChanged(snapshot));
    }
}
