package com.wifi.optometry.domain.model;

public class ClinicSettings {
    private String companyName;
    private boolean cloudEnabled;
    private String cloudUrl;
    private String cloudAccount;
    private String cloudPassword;
    private String language;
    private boolean showDisplayDuration;
    private String dateUnit;
    private String timeUnit;
    private double sphStep;
    private double sphShiftStep;
    private double cylStep;
    private double cylShiftStep;
    private double axisStep;
    private double axisShiftStep;
    private double prismStep;
    private double prismShiftStep;
    private double pdStep;
    private double pdShiftStep;

    public ClinicSettings copy() {
        ClinicSettings copy = new ClinicSettings();
        copy.companyName = companyName;
        copy.cloudEnabled = cloudEnabled;
        copy.cloudUrl = cloudUrl;
        copy.cloudAccount = cloudAccount;
        copy.cloudPassword = cloudPassword;
        copy.language = language;
        copy.showDisplayDuration = showDisplayDuration;
        copy.dateUnit = dateUnit;
        copy.timeUnit = timeUnit;
        copy.sphStep = sphStep;
        copy.sphShiftStep = sphShiftStep;
        copy.cylStep = cylStep;
        copy.cylShiftStep = cylShiftStep;
        copy.axisStep = axisStep;
        copy.axisShiftStep = axisShiftStep;
        copy.prismStep = prismStep;
        copy.prismShiftStep = prismShiftStep;
        copy.pdStep = pdStep;
        copy.pdShiftStep = pdShiftStep;
        return copy;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public boolean isCloudEnabled() {
        return cloudEnabled;
    }

    public void setCloudEnabled(boolean cloudEnabled) {
        this.cloudEnabled = cloudEnabled;
    }

    public String getCloudUrl() {
        return cloudUrl;
    }

    public void setCloudUrl(String cloudUrl) {
        this.cloudUrl = cloudUrl;
    }

    public String getCloudAccount() {
        return cloudAccount;
    }

    public void setCloudAccount(String cloudAccount) {
        this.cloudAccount = cloudAccount;
    }

    public String getCloudPassword() {
        return cloudPassword;
    }

    public void setCloudPassword(String cloudPassword) {
        this.cloudPassword = cloudPassword;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isShowDisplayDuration() {
        return showDisplayDuration;
    }

    public void setShowDisplayDuration(boolean showDisplayDuration) {
        this.showDisplayDuration = showDisplayDuration;
    }

    public String getDateUnit() {
        return dateUnit;
    }

    public void setDateUnit(String dateUnit) {
        this.dateUnit = dateUnit;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public double getSphStep() {
        return sphStep;
    }

    public void setSphStep(double sphStep) {
        this.sphStep = sphStep;
    }

    public double getSphShiftStep() {
        return sphShiftStep;
    }

    public void setSphShiftStep(double sphShiftStep) {
        this.sphShiftStep = sphShiftStep;
    }

    public double getCylStep() {
        return cylStep;
    }

    public void setCylStep(double cylStep) {
        this.cylStep = cylStep;
    }

    public double getCylShiftStep() {
        return cylShiftStep;
    }

    public void setCylShiftStep(double cylShiftStep) {
        this.cylShiftStep = cylShiftStep;
    }

    public double getAxisStep() {
        return axisStep;
    }

    public void setAxisStep(double axisStep) {
        this.axisStep = axisStep;
    }

    public double getAxisShiftStep() {
        return axisShiftStep;
    }

    public void setAxisShiftStep(double axisShiftStep) {
        this.axisShiftStep = axisShiftStep;
    }

    public double getPrismStep() {
        return prismStep;
    }

    public void setPrismStep(double prismStep) {
        this.prismStep = prismStep;
    }

    public double getPrismShiftStep() {
        return prismShiftStep;
    }

    public void setPrismShiftStep(double prismShiftStep) {
        this.prismShiftStep = prismShiftStep;
    }

    public double getPdStep() {
        return pdStep;
    }

    public void setPdStep(double pdStep) {
        this.pdStep = pdStep;
    }

    public double getPdShiftStep() {
        return pdShiftStep;
    }

    public void setPdShiftStep(double pdShiftStep) {
        this.pdShiftStep = pdShiftStep;
    }
}
