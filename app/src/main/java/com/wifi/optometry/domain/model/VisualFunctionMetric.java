package com.wifi.optometry.domain.model;

public class VisualFunctionMetric {
    private String groupTitle;
    private String itemTitle;
    private String measureLabel;
    private String resultValue;
    private String referenceValue;
    private String status;
    private String note;

    public VisualFunctionMetric(
            String groupTitle,
            String itemTitle,
            String measureLabel,
            String resultValue,
            String referenceValue,
            String status,
            String note
    ) {
        this.groupTitle = groupTitle;
        this.itemTitle = itemTitle;
        this.measureLabel = measureLabel;
        this.resultValue = resultValue;
        this.referenceValue = referenceValue;
        this.status = status;
        this.note = note;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public String getMeasureLabel() {
        return measureLabel;
    }

    public String getResultValue() {
        return resultValue;
    }

    public String getReferenceValue() {
        return referenceValue;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
