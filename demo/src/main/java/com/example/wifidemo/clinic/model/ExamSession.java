package com.example.wifidemo.clinic.model;

public class ExamSession {
    public enum DistanceMode {
        FAR,
        NEAR
    }

    public enum EyeSelection {
        RIGHT,
        LEFT,
        BOTH
    }

    public enum MeasurementField {
        SPH,
        CYL,
        AXIS,
        ADD,
        VA,
        X,
        Y,
        PD
    }

    public enum PrismMode {
        CARTESIAN,
        POLAR
    }

    private PatientProfile patient;
    private String currentProgramId;
    private int currentStepIndex;
    private String selectedChartId;
    private MeasurementField selectedField;
    private EyeSelection activeEye;
    private EyeSelection lensVisibility;
    private DistanceMode distanceMode;
    private PrismMode prismMode;
    private boolean cylMinusMode;
    private boolean cpLinked;
    private boolean lensInserted;
    private boolean shiftEnabled;
    private String note;
    private final LensMeasurement farRight = new LensMeasurement();
    private final LensMeasurement farLeft = new LensMeasurement();
    private final LensMeasurement nearRight = new LensMeasurement();
    private final LensMeasurement nearLeft = new LensMeasurement();
    private final LensMeasurement finalRight = new LensMeasurement();
    private final LensMeasurement finalLeft = new LensMeasurement();
    private final FunctionalTestState functionalTests = new FunctionalTestState();

    public PatientProfile getPatient() {
        return patient;
    }

    public void setPatient(PatientProfile patient) {
        this.patient = patient;
    }

    public String getCurrentProgramId() {
        return currentProgramId;
    }

    public void setCurrentProgramId(String currentProgramId) {
        this.currentProgramId = currentProgramId;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }

    public String getSelectedChartId() {
        return selectedChartId;
    }

    public void setSelectedChartId(String selectedChartId) {
        this.selectedChartId = selectedChartId;
    }

    public MeasurementField getSelectedField() {
        return selectedField;
    }

    public void setSelectedField(MeasurementField selectedField) {
        this.selectedField = selectedField;
    }

    public EyeSelection getActiveEye() {
        return activeEye;
    }

    public void setActiveEye(EyeSelection activeEye) {
        this.activeEye = activeEye;
    }

    public EyeSelection getLensVisibility() {
        return lensVisibility;
    }

    public void setLensVisibility(EyeSelection lensVisibility) {
        this.lensVisibility = lensVisibility;
    }

    public DistanceMode getDistanceMode() {
        return distanceMode;
    }

    public void setDistanceMode(DistanceMode distanceMode) {
        this.distanceMode = distanceMode;
    }

    public PrismMode getPrismMode() {
        return prismMode;
    }

    public void setPrismMode(PrismMode prismMode) {
        this.prismMode = prismMode;
    }

    public boolean isCylMinusMode() {
        return cylMinusMode;
    }

    public void setCylMinusMode(boolean cylMinusMode) {
        this.cylMinusMode = cylMinusMode;
    }

    public boolean isCpLinked() {
        return cpLinked;
    }

    public void setCpLinked(boolean cpLinked) {
        this.cpLinked = cpLinked;
    }

    public boolean isLensInserted() {
        return lensInserted;
    }

    public void setLensInserted(boolean lensInserted) {
        this.lensInserted = lensInserted;
    }

    public boolean isShiftEnabled() {
        return shiftEnabled;
    }

    public void setShiftEnabled(boolean shiftEnabled) {
        this.shiftEnabled = shiftEnabled;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LensMeasurement getFarRight() {
        return farRight;
    }

    public LensMeasurement getFarLeft() {
        return farLeft;
    }

    public LensMeasurement getNearRight() {
        return nearRight;
    }

    public LensMeasurement getNearLeft() {
        return nearLeft;
    }

    public LensMeasurement getFinalRight() {
        return finalRight;
    }

    public LensMeasurement getFinalLeft() {
        return finalLeft;
    }

    public FunctionalTestState getFunctionalTests() {
        return functionalTests;
    }
}
