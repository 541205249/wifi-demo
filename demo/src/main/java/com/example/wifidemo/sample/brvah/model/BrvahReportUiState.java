package com.example.wifidemo.sample.brvah.model;

import com.example.wifidemo.clinic.model.ReportRecord;

import java.util.ArrayList;
import java.util.List;

public class BrvahReportUiState {
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_LOADING = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_COMPLETE = 3;

    private final List<ReportRecord> records;
    private final String summary;
    private final String footerTip;
    private final int status;
    private final boolean hasMore;
    private final String errorMessage;

    public BrvahReportUiState(
            List<ReportRecord> records,
            String summary,
            String footerTip,
            int status,
            boolean hasMore,
            String errorMessage
    ) {
        this.records = new ArrayList<>(records);
        this.summary = summary;
        this.footerTip = footerTip;
        this.status = status;
        this.hasMore = hasMore;
        this.errorMessage = errorMessage;
    }

    public List<ReportRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public String getSummary() {
        return summary;
    }

    public String getFooterTip() {
        return footerTip;
    }

    public int getStatus() {
        return status;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
