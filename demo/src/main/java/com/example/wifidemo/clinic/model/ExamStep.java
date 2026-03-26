package com.example.wifidemo.clinic.model;

public class ExamStep {
    public enum DistanceMode {
        FAR,
        NEAR,
        BOTH
    }

    public enum EyeScope {
        RIGHT,
        LEFT,
        BOTH
    }

    public enum Comparator {
        NONE,
        EQ,
        GT,
        GTE,
        LT,
        LTE
    }

    private String id;
    private String title;
    private String description;
    private String chartId;
    private DistanceMode distanceMode;
    private EyeScope eyeScope;
    private String subjectSource;
    private String targetField;
    private String fogOption;
    private String nearLightOption;
    private String functionLabel;
    private String skipField;
    private Comparator skipComparator;
    private double skipThreshold;
    private String note;

    public ExamStep(
            String id,
            String title,
            String description,
            String chartId,
            DistanceMode distanceMode,
            EyeScope eyeScope,
            String subjectSource,
            String targetField,
            String fogOption,
            String nearLightOption,
            String functionLabel,
            String skipField,
            Comparator skipComparator,
            double skipThreshold,
            String note
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.chartId = chartId;
        this.distanceMode = distanceMode;
        this.eyeScope = eyeScope;
        this.subjectSource = subjectSource;
        this.targetField = targetField;
        this.fogOption = fogOption;
        this.nearLightOption = nearLightOption;
        this.functionLabel = functionLabel;
        this.skipField = skipField;
        this.skipComparator = skipComparator;
        this.skipThreshold = skipThreshold;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getChartId() {
        return chartId;
    }

    public DistanceMode getDistanceMode() {
        return distanceMode;
    }

    public EyeScope getEyeScope() {
        return eyeScope;
    }

    public String getSubjectSource() {
        return subjectSource;
    }

    public String getTargetField() {
        return targetField;
    }

    public String getFogOption() {
        return fogOption;
    }

    public String getNearLightOption() {
        return nearLightOption;
    }

    public String getFunctionLabel() {
        return functionLabel;
    }

    public String getSkipField() {
        return skipField;
    }

    public Comparator getSkipComparator() {
        return skipComparator;
    }

    public double getSkipThreshold() {
        return skipThreshold;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
