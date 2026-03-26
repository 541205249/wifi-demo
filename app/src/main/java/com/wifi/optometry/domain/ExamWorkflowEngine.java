package com.wifi.optometry.domain;

import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.LensMeasurement;

import java.util.List;

public final class ExamWorkflowEngine {
    private ExamWorkflowEngine() {
    }

    public static ExamStep getCurrentStep(ExamSession session, List<ExamProgram> programs) {
        ExamProgram program = findProgram(session.getCurrentProgramId(), programs);
        if (program == null || program.getSteps().isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(session.getCurrentStepIndex(), program.getSteps().size() - 1));
        session.setCurrentStepIndex(index);
        return program.getSteps().get(index);
    }

    public static int getProgressPercent(ExamSession session, List<ExamProgram> programs) {
        ExamProgram program = findProgram(session.getCurrentProgramId(), programs);
        if (program == null || program.getSteps().isEmpty()) {
            return 0;
        }
        return (int) (((session.getCurrentStepIndex() + 1f) / program.getSteps().size()) * 100f);
    }

    public static void moveNext(ExamSession session, List<ExamProgram> programs) {
        ExamProgram program = findProgram(session.getCurrentProgramId(), programs);
        if (program == null || program.getSteps().isEmpty()) {
            return;
        }

        int targetIndex = session.getCurrentStepIndex() + 1;
        while (targetIndex < program.getSteps().size() && shouldSkip(session, program.getSteps().get(targetIndex))) {
            targetIndex++;
        }
        if (targetIndex >= program.getSteps().size()) {
            targetIndex = program.getSteps().size() - 1;
        }
        session.setCurrentStepIndex(targetIndex);
    }

    public static void movePrevious(ExamSession session, List<ExamProgram> programs) {
        ExamProgram program = findProgram(session.getCurrentProgramId(), programs);
        if (program == null || program.getSteps().isEmpty()) {
            return;
        }

        int targetIndex = session.getCurrentStepIndex() - 1;
        while (targetIndex > 0 && shouldSkip(session, program.getSteps().get(targetIndex))) {
            targetIndex--;
        }
        session.setCurrentStepIndex(Math.max(0, targetIndex));
    }

    private static boolean shouldSkip(ExamSession session, ExamStep step) {
        if (step == null || step.getSkipComparator() == null || step.getSkipComparator() == ExamStep.Comparator.NONE) {
            return false;
        }
        double currentValue = resolveField(session, step);
        if (Double.isNaN(currentValue)) {
            return false;
        }
        double target = step.getSkipThreshold();
        switch (step.getSkipComparator()) {
            case EQ:
                return currentValue == target;
            case GT:
                return currentValue > target;
            case GTE:
                return currentValue >= target;
            case LT:
                return currentValue < target;
            case LTE:
                return currentValue <= target;
            default:
                return false;
        }
    }

    private static double resolveField(ExamSession session, ExamStep step) {
        LensMeasurement measurement = pickMeasurement(session, step.getEyeScope(), step.getDistanceMode());
        if (measurement == null || step.getSkipField() == null) {
            return Double.NaN;
        }
        String field = step.getSkipField().trim().toUpperCase();
        switch (field) {
            case "SPH":
                return measurement.getSph();
            case "CYL":
                return measurement.getCyl();
            case "AXIS":
            case "AXS":
                return measurement.getAxis();
            case "ADD":
                return measurement.getAdd();
            case "VA":
                return measurement.getVa();
            case "X":
                return measurement.getPrismX();
            case "Y":
                return measurement.getPrismY();
            case "PD":
                return measurement.getPd();
            default:
                return Double.NaN;
        }
    }

    private static LensMeasurement pickMeasurement(
            ExamSession session,
            ExamStep.EyeScope eyeScope,
            ExamStep.DistanceMode distanceMode
    ) {
        boolean useNear = distanceMode == ExamStep.DistanceMode.NEAR;
        if (eyeScope == ExamStep.EyeScope.LEFT) {
            return useNear ? session.getNearLeft() : session.getFarLeft();
        }
        return useNear ? session.getNearRight() : session.getFarRight();
    }

    private static ExamProgram findProgram(String programId, List<ExamProgram> programs) {
        if (programs == null) {
            return null;
        }
        for (ExamProgram program : programs) {
            if (program.getId().equals(programId)) {
                return program;
            }
        }
        return null;
    }
}
