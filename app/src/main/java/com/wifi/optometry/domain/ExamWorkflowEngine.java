package com.wifi.optometry.domain;

import com.wifi.lib.log.DLog;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.LensMeasurement;

import java.util.List;

public final class ExamWorkflowEngine {
    private static final String TAG = "ExamWorkflow";

    private ExamWorkflowEngine() {
    }

    public static ExamStep getCurrentStep(ExamSession session, List<ExamProgram> programs) {
        ExamProgram program = findProgram(session.getCurrentProgramId(), programs);
        if (program == null || program.getSteps().isEmpty()) {
            DLog.w(TAG, "未找到当前流程步骤，programId=" + session.getCurrentProgramId());
            return null;
        }
        int index = Math.max(0, Math.min(session.getCurrentStepIndex(), program.getSteps().size() - 1));
        session.setCurrentStepIndex(index);
        ExamStep step = program.getSteps().get(index);
        DLog.d(TAG, "解析当前步骤完成，programId=" + program.getId()
                + ", index=" + index
                + ", stepId=" + step.getId());
        return step;
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
            DLog.w(TAG, "下一步流转失败，未找到流程 programId=" + session.getCurrentProgramId());
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
        DLog.d(TAG, "流程前进完成，programId=" + program.getId() + ", index=" + targetIndex);
    }

    public static void movePrevious(ExamSession session, List<ExamProgram> programs) {
        ExamProgram program = findProgram(session.getCurrentProgramId(), programs);
        if (program == null || program.getSteps().isEmpty()) {
            DLog.w(TAG, "上一步流转失败，未找到流程 programId=" + session.getCurrentProgramId());
            return;
        }

        int targetIndex = session.getCurrentStepIndex() - 1;
        while (targetIndex > 0 && shouldSkip(session, program.getSteps().get(targetIndex))) {
            targetIndex--;
        }
        session.setCurrentStepIndex(Math.max(0, targetIndex));
        DLog.d(TAG, "流程回退完成，programId=" + program.getId() + ", index=" + session.getCurrentStepIndex());
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
        boolean shouldSkip;
        switch (step.getSkipComparator()) {
            case EQ:
                shouldSkip = currentValue == target;
                break;
            case GT:
                shouldSkip = currentValue > target;
                break;
            case GTE:
                shouldSkip = currentValue >= target;
                break;
            case LT:
                shouldSkip = currentValue < target;
                break;
            case LTE:
                shouldSkip = currentValue <= target;
                break;
            default:
                shouldSkip = false;
                break;
        }
        if (shouldSkip) {
            DLog.d(TAG, "命中跳步条件，stepId=" + step.getId()
                    + ", field=" + step.getSkipField()
                    + ", current=" + currentValue
                    + ", comparator=" + step.getSkipComparator()
                    + ", threshold=" + target);
        }
        return shouldSkip;
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

