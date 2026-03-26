package com.example.wifidemo.clinic.model;

public class VisionChart {
    private String id;
    private String title;
    private String subtitle;
    private String description;
    private int imageResId;

    public VisionChart(String id, String title, String subtitle, String description, int imageResId) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.imageResId = imageResId;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public int getImageResId() {
        return imageResId;
    }
}
