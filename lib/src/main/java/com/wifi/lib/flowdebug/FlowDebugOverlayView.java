package com.wifi.lib.flowdebug;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wifi.lib.databinding.ViewFlowDebugOverlayBinding;
import com.wifi.lib.log.JLogEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FlowDebugOverlayView extends FrameLayout {
    public interface Callback {
        void onCollapsedChanged(boolean collapsed);

        void onPositionChanged(float x, float y);
    }

    private final ViewFlowDebugOverlayBinding binding;
    private final FlowLogCenter flowLogCenter = FlowLogCenter.getInstance();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private final int touchSlop;

    @Nullable
    private FlowLogCenter.Listener logListener;
    @Nullable
    private Callback callback;
    private boolean collapsed;

    public FlowDebugOverlayView(@NonNull Context context) {
        this(context, null);
    }

    public FlowDebugOverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = ViewFlowDebugOverlayBinding.inflate(LayoutInflater.from(context), this, true);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        setClipChildren(false);
        setClipToPadding(false);

        binding.btnOverlayClear.setOnClickListener(v -> flowLogCenter.clear());
        binding.btnOverlayCollapse.setOnClickListener(v -> updateCollapsedState(true, true));

        binding.collapsedCard.setOnTouchListener(new DragTouchListener(() -> updateCollapsedState(false, true)));
        binding.headerDragArea.setOnTouchListener(new DragTouchListener(null));
    }

    public void bind(boolean initialCollapsed, float initialX, float initialY, @Nullable Callback callback) {
        this.callback = callback;
        this.collapsed = initialCollapsed;
        applyCollapsedState();
        if (logListener == null) {
            logListener = this::renderLogs;
            flowLogCenter.addListener(logListener);
        }
        post(() -> applyInitialPosition(initialX, initialY));
    }

    public void release() {
        if (logListener != null) {
            flowLogCenter.removeListener(logListener);
            logListener = null;
        }
        callback = null;
    }

    private void renderLogs(@NonNull List<JLogEntry> entries) {
        binding.tvOverlayCount.setText(String.valueOf(entries.size()));
        binding.tvCollapsedCount.setText(String.valueOf(entries.size()));

        if (entries.isEmpty()) {
            binding.tvLatestLogSummary.setText("等待 DLog 日志");
            binding.tvLogs.setText("暂时还没有 DLog 数据流日志。");
            return;
        }

        JLogEntry latest = entries.get(0);
        binding.tvLatestLogSummary.setText(buildSummary(latest));

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append(formatEntry(entries.get(index)));
        }
        binding.tvLogs.setText(builder.toString());
        binding.scrollLogs.post(() -> binding.scrollLogs.scrollTo(0, 0));
    }

    private String buildSummary(@NonNull JLogEntry entry) {
        String message = entry.getDisplayMessage().replace('\n', ' ').trim();
        if (message.length() > 40) {
            message = message.substring(0, 40) + "...";
        }
        if (TextUtils.isEmpty(message)) {
            return "最新一条日志来自 " + entry.getThreadName();
        }
        return entry.getPriorityLabel() + " | " + message;
    }

    @NonNull
    private String formatEntry(@NonNull JLogEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append("[")
                .append(timeFormat.format(new Date(entry.getTimestamp())))
                .append("] ")
                .append(entry.getPriorityLabel())
                .append(" | ")
                .append(entry.getThreadName());
        String message = entry.getDisplayMessage();
        if (!TextUtils.isEmpty(message)) {
            builder.append("\n").append(message);
        }
        return builder.toString();
    }

    private void updateCollapsedState(boolean collapsed, boolean notifyCallback) {
        this.collapsed = collapsed;
        applyCollapsedState();
        post(() -> {
            clampIntoParent(getX(), getY(), notifyCallback);
            if (notifyCallback && callback != null) {
                callback.onCollapsedChanged(collapsed);
            }
        });
    }

    private void applyCollapsedState() {
        binding.expandedCard.setVisibility(collapsed ? GONE : VISIBLE);
        binding.collapsedCard.setVisibility(collapsed ? VISIBLE : GONE);
    }

    private void applyInitialPosition(float initialX, float initialY) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null || parent.getWidth() == 0 || getWidth() == 0) {
            post(() -> applyInitialPosition(initialX, initialY));
            return;
        }

        if (Float.isNaN(initialX) || Float.isNaN(initialY)) {
            int margin = dp(12);
            float targetX = Math.max(0, parent.getWidth() - getWidth() - margin);
            float targetY = dp(72);
            clampIntoParent(targetX, targetY, false);
            return;
        }
        clampIntoParent(initialX, initialY, false);
    }

    private void clampIntoParent(float targetX, float targetY, boolean notifyCallback) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            setX(targetX);
            setY(targetY);
            return;
        }

        float maxX = Math.max(0, parent.getWidth() - getWidth());
        float maxY = Math.max(0, parent.getHeight() - getHeight());
        float clampedX = Math.max(0, Math.min(targetX, maxX));
        float clampedY = Math.max(0, Math.min(targetY, maxY));
        setX(clampedX);
        setY(clampedY);
        if (notifyCallback && callback != null) {
            callback.onPositionChanged(clampedX, clampedY);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private final class DragTouchListener implements OnTouchListener {
        @Nullable
        private final Runnable clickAction;
        private float downRawX;
        private float downRawY;
        private float startX;
        private float startY;
        private boolean dragging;

        private DragTouchListener(@Nullable Runnable clickAction) {
            this.clickAction = clickAction;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = getX();
                    startY = getY();
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - downRawX;
                    float deltaY = event.getRawY() - downRawY;
                    if (!dragging && (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop)) {
                        dragging = true;
                    }
                    if (dragging) {
                        clampIntoParent(startX + deltaX, startY + deltaY, true);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragging && clickAction != null) {
                        v.performClick();
                        clickAction.run();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                default:
                    return false;
            }
        }
    }
}
