package com.wifi.optometry.util;

import com.wifi.optometry.domain.model.LensMeasurement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ClinicFormatters {
    private ClinicFormatters() {
    }

    public static String formatSigned(double value) {
        return String.format(Locale.getDefault(), "%+.2f", value);
    }

    public static String formatUnsigned(double value) {
        return String.format(Locale.getDefault(), "%.2f", value);
    }

    public static String formatAxis(double value) {
        return String.format(Locale.getDefault(), "%.0f", value);
    }

    public static String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    public static String buildLensSummary(LensMeasurement measurement) {
        if (measurement == null) {
            return "--";
        }
        return "S " + formatSigned(measurement.getSph())
                + " / C " + formatSigned(measurement.getCyl())
                + " / A " + formatAxis(measurement.getAxis())
                + " / VA " + formatUnsigned(measurement.getVa());
    }

    public static String buildPrescriptionSummary(LensMeasurement right, LensMeasurement left) {
        return "R[" + buildLensSummary(right) + "]  L[" + buildLensSummary(left) + "]";
    }
}
