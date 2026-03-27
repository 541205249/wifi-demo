package com.example.wifidemo.sample.brvah.model;

public class BrvahWorkflowItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_STEP = 1;
    public static final int TYPE_ACTION = 2;

    private final int viewType;
    private final String title;
    private final String subtitle;
    private final String detail;
    private final String statusLabel;

    public BrvahWorkflowItem(int viewType, String title, String subtitle, String detail, String statusLabel) {
        this.viewType = viewType;
        this.title = title;
        this.subtitle = subtitle;
        this.detail = detail;
        this.statusLabel = statusLabel;
    }

    public int getViewType() {
        return viewType;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDetail() {
        return detail;
    }

    public String getStatusLabel() {
        return statusLabel;
    }
}
