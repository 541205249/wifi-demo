package com.wifi.optometry.domain.model;

import java.util.ArrayList;
import java.util.List;

public class ReportRecord {
    private String id;
    private String patientName;
    private String programName;
    private long createdAt;
    private String visionSummary;
    private String prescriptionSummary;
    private String qrPayload;
    private LensMeasurement finalRight;
    private LensMeasurement finalLeft;
    private final List<VisualFunctionMetric> metrics = new ArrayList<>();

    public ReportRecord(
            String id,
            String patientName,
            String programName,
            long createdAt,
            String visionSummary,
            String prescriptionSummary,
            String qrPayload,
            LensMeasurement finalRight,
            LensMeasurement finalLeft
    ) {
        this.id = id;
        this.patientName = patientName;
        this.programName = programName;
        this.createdAt = createdAt;
        this.visionSummary = visionSummary;
        this.prescriptionSummary = prescriptionSummary;
        this.qrPayload = qrPayload;
        this.finalRight = finalRight;
        this.finalLeft = finalLeft;
    }

    public String getId() {
        return id;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getProgramName() {
        return programName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getVisionSummary() {
        return visionSummary;
    }

    public String getPrescriptionSummary() {
        return prescriptionSummary;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public LensMeasurement getFinalRight() {
        return finalRight;
    }

    public LensMeasurement getFinalLeft() {
        return finalLeft;
    }

    public List<VisualFunctionMetric> getMetrics() {
        return metrics;
    }
}
