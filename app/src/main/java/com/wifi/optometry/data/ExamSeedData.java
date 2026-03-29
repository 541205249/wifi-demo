package com.wifi.optometry.data;

import com.wifi.optometry.R;
import com.wifi.optometry.domain.model.ClinicSettings;
import com.wifi.optometry.domain.model.ExamProgram;
import com.wifi.optometry.domain.model.ExamSession;
import com.wifi.optometry.domain.model.ExamStep;
import com.wifi.optometry.domain.model.FunctionalTestState;
import com.wifi.optometry.domain.model.PatientProfile;
import com.wifi.optometry.domain.model.VisionChart;

import java.util.ArrayList;
import java.util.List;

public final class ExamSeedData {
    private ExamSeedData() {
    }

    public static List<PatientProfile> createPatients() {
        List<PatientProfile> patients = new ArrayList<>();
        patients.add(new PatientProfile("P001", "张晨曦", "13800138000", "女", "1991-08-12", "上海市浦东新区", "常规复查"));
        patients.add(new PatientProfile("P002", "李承宇", "13900139000", "男", "1986-03-05", "杭州市西湖区", "近用疲劳明显"));
        patients.add(new PatientProfile("P003", "王念", "13700137000", "女", "2008-11-21", "苏州市工业园区", "立体视训练跟踪"));
        patients.add(new PatientProfile("P004", "赵沐", "13600136000", "男", "1998-02-17", "南京市鼓楼区", "初诊主观验光"));
        return patients;
    }

    public static List<VisionChart> createCharts() {
        List<VisionChart> charts = new ArrayList<>();
        charts.add(new VisionChart("phoria", "隐斜视", "远近隐斜检测", "用于水平与垂直隐斜位初筛，配合棱镜或偏振分离观察双眼对位。", R.drawable.img_chart_fixation_grid_reference));
        charts.add(new VisionChart("fixation_phoria", "带固视的隐斜视", "中心固视 + 周边分离", "在中心固视条件下观察隐斜代偿，适合近距功能筛查。", R.drawable.img_chart_fixation_grid_reference));
        charts.add(new VisionChart("stereo", "立体视", "双眼融合判断", "通过双眼视差图案观察立体感知与融合能力。", R.drawable.img_chart_stereo_pattern_reference));
        charts.add(new VisionChart("vertical", "垂直重合", "上下对齐", "用于垂直偏差与对齐能力检查，常和 AC/A 一起记录。", R.drawable.img_chart_vertical_alignment_reference));
        charts.add(new VisionChart("schober", "Schober", "立体及融合辅助图", "用于立体视觉和主观融合判断，可作为近距检查辅助视标。", R.drawable.img_chart_stereo_pattern_reference));
        charts.add(new VisionChart("worth", "Worth", "四点融像", "用于抑制、复视和双眼融合状态判断。", R.drawable.img_chart_dot_pattern_reference));
        charts.add(new VisionChart("fixation", "固视", "中心固视参考", "用于帮助被测者维持注视点，常与隐斜和垂直重合联动。", R.drawable.img_chart_fixation_grid_reference));
        charts.add(new VisionChart("red_green", "红/绿", "红绿平衡", "用于第一次和第二次红绿检测，辅助确认球镜方向。", R.drawable.img_chart_polar_balance_reference));
        charts.add(new VisionChart("polar_red_green", "偏振红/绿", "偏振分离", "用于红绿平衡与双眼分离演示，适合双眼平衡末段。", R.drawable.img_chart_polar_balance_reference));
        charts.add(new VisionChart("va", "VA 表", "视力表", "根据当前矫正视力自动切换视标范围，适配远距与近距模式。", R.drawable.img_chart_visual_acuity_reference));
        charts.add(new VisionChart("cross_grid", "交叉格栅", "散光辅助判断", "用于散光轴位和清晰方向判断。", R.drawable.img_chart_fixation_grid_reference));
        charts.add(new VisionChart("astigmatism_dial", "散光表", "钟表盘视标", "用于粗调与精调散光轴位、散光度数。", R.drawable.img_chart_fixation_grid_reference));
        charts.add(new VisionChart("dots", "斑点图", "双眼平衡辅助图", "用于双眼视觉舒适度与单字模式辅助判断。", R.drawable.img_chart_dot_pattern_reference));
        charts.add(new VisionChart("balance", "双眼平衡", "双眼平衡图", "用于双眼最佳视力后做平衡调整，确认主观舒适度与双眼协同。", R.drawable.img_chart_polar_balance_reference));
        return charts;
    }

    public static List<ExamProgram> createPrograms() {
        List<ExamProgram> programs = new ArrayList<>();
        programs.add(createRegularProgram());
        programs.add(createQuickProgram());
        programs.add(createFullVisualProgram());
        return programs;
    }

    private static ExamProgram createRegularProgram() {
        ExamProgram routine = new ExamProgram(
                "routine",
                "常规验光",
                "标准主观验光流程",
                "覆盖雾视、散光粗精调、红绿检测、双眼平衡、MPMVA 和试戴。"
        );
        routine.getSteps().add(step("rt_01", "雾视", "先建立雾视量，保证调节放松。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "SubJ", "SPH", "+0.75", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_02", "去雾视（右眼）", "右眼逐步减雾并观察最佳视力。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "SPH", "+0.25", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_03", "粗调散光轴位（右眼）", "结合散光表完成粗调。", "astigmatism_dial", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "AXIS", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_04", "粗调散光度数（右眼）", "以柱镜步长推进，找到清晰方向。", "cross_grid", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "CYL", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_05", "首次 MPMVA（右眼）", "最正最高视力第一次确认。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_06", "首次红绿检测（右眼）", "利用红绿平衡确认球镜方向。", "red_green", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_07", "精调散光轴位（右眼）", "在更细步长下精调轴位。", "astigmatism_dial", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "AXIS", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_08", "精调散光度数（右眼）", "再次确认右眼柱镜度数。", "cross_grid", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "CYL", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_09", "二次红绿检测（右眼）", "右眼第二次红绿平衡。", "red_green", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_10", "单眼 MPMVA（右眼）", "右眼单眼最正最高视力复核。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "Final", "VA", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_11", "去雾视（左眼）", "左眼逐步减雾并观察最佳视力。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "SPH", "+0.25", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_12", "粗调散光轴位（左眼）", "左眼轴位粗调。", "astigmatism_dial", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "AXIS", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_13", "粗调散光度数（左眼）", "左眼柱镜粗调。", "cross_grid", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "CYL", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_14", "首次 MPMVA（左眼）", "左眼第一次最正最高视力确认。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_15", "首次红绿检测（左眼）", "左眼第一次红绿平衡。", "red_green", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_16", "精调散光轴位（左眼）", "左眼轴位精调。", "astigmatism_dial", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "AXIS", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_17", "精调散光度数（左眼）", "左眼柱镜精调。", "cross_grid", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "CYL", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_18", "二次红绿检测（左眼）", "左眼第二次红绿检测。", "red_green", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_19", "单眼 MPMVA（左眼）", "左眼单眼最正最高视力复核。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "Final", "VA", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_20", "双眼最佳视力检测", "合并双眼后确认最佳视力。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_21", "双眼平衡测试", "调整双眼舒适平衡。", "balance", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_22", "双眼 MPMVA", "最正最高视力双眼复核。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        routine.getSteps().add(step("rt_23", "试戴", "确认远近舒适度与处方接受度。", "polar_red_green", ExamStep.DistanceMode.BOTH, ExamStep.EyeScope.BOTH, "Final", "VA", "", "开", "", "", ExamStep.Comparator.NONE, 0));
        return routine;
    }

    private static ExamProgram createQuickProgram() {
        ExamProgram quick = new ExamProgram(
                "quick_screen",
                "视功能快速筛查",
                "五项快速筛查",
                "适合门店快速初筛：双眼融合、NRA/PRA、远近眼位。"
        );
        quick.getSteps().add(step("qs_01", "双眼融合测试", "先做 Worth 融合判断。", "worth", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "关", "Worth", "", ExamStep.Comparator.NONE, 0));
        quick.getSteps().add(step("qs_02", "NRA", "在近距模式下推进正镜直到模糊。", "polar_red_green", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "NRA", "", ExamStep.Comparator.NONE, 0));
        quick.getSteps().add(step("qs_03", "PRA", "在近距模式下推进负镜直到模糊。", "polar_red_green", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "PRA", "", ExamStep.Comparator.NONE, 0));
        quick.getSteps().add(step("qs_04", "远眼位", "记录远距隐斜和水平融像。", "phoria", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        quick.getSteps().add(step("qs_05", "近眼位", "记录近距隐斜和集合能力。", "fixation_phoria", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "", "", ExamStep.Comparator.NONE, 0));
        return quick;
    }

    private static ExamProgram createFullVisualProgram() {
        ExamProgram full = new ExamProgram(
                "full_visual",
                "全套视功能检查",
                "全流程双眼视功能检查",
                "覆盖双眼融合、立体视、远近眼位、AC/A、NRA/PRA、BCC、AMP 和水平融像。"
        );
        full.getSteps().add(step("fv_01", "双眼融合测试", "从 Worth 开始确认基础融合。", "worth", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "关", "Worth", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_02", "立体视测试", "记录立体视觉级别。", "stereo", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "开", "", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_03", "远眼位", "远距隐斜与融像发散/集合。", "phoria", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "关", "", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_04", "近眼位", "近距隐斜与集合能力。", "fixation_phoria", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_05", "AC/A（梯度法）", "记录 BI 与目标偏移。", "vertical", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "AC/A", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_06", "NRA", "记录模糊与恢复。", "polar_red_green", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "NRA", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_07", "BCC 检查", "检查双眼交叉柱镜近附加光度。", "cross_grid", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_08", "PRA", "记录模糊与恢复。", "polar_red_green", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "PRA", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_09", "AMP 检查", "分别记录 R/L 调节幅度。", "va", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "AMP", "", ExamStep.Comparator.NONE, 0));
        full.getSteps().add(step("fv_10", "远近水平融像", "完成最终功能汇总。", "balance", ExamStep.DistanceMode.BOTH, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "", "", ExamStep.Comparator.NONE, 0));
        return full;
    }

    private static ExamStep step(
            String id,
            String title,
            String description,
            String chartId,
            ExamStep.DistanceMode distanceMode,
            ExamStep.EyeScope eyeScope,
            String subjectSource,
            String targetField,
            String fogOption,
            String nearLightOption,
            String functionLabel,
            String skipField,
            ExamStep.Comparator comparator,
            double skipThreshold
    ) {
        return new ExamStep(
                id,
                title,
                description,
                chartId,
                distanceMode,
                eyeScope,
                subjectSource,
                targetField,
                fogOption,
                nearLightOption,
                functionLabel,
                skipField,
                comparator,
                skipThreshold,
                ""
        );
    }

    public static ClinicSettings createSettings() {
        ClinicSettings settings = new ClinicSettings();
        settings.setCompanyName("星图验光中心");
        settings.setCloudEnabled(true);
        settings.setCloudUrl("https://cloud.optometry.local/report");
        settings.setCloudAccount("demo@clinic.local");
        settings.setCloudPassword("123456");
        settings.setLanguage("中文");
        settings.setShowDisplayDuration(true);
        settings.setDateUnit("年/月/日");
        settings.setTimeUnit("时/分/秒");
        settings.setChartDeviceMode("跟随主设备屏");
        settings.setChartPageMode("自动切换");
        settings.setDefaultChartId("va");
        settings.setClockYear(2026);
        settings.setClockMonth(3);
        settings.setClockDay(29);
        settings.setClockHour(18);
        settings.setClockMinute(10);
        settings.setClockSecond(59);
        settings.setSphStep(0.25);
        settings.setSphShiftStep(1.00);
        settings.setCylStep(0.25);
        settings.setCylShiftStep(1.00);
        settings.setAxisStep(5);
        settings.setAxisShiftStep(15);
        settings.setPrismStep(0.5);
        settings.setPrismShiftStep(1.0);
        settings.setPdStep(0.5);
        settings.setPdShiftStep(1.0);
        return settings;
    }

    public static ExamSession createInitialSession(PatientProfile patient) {
        ExamSession session = new ExamSession();
        session.setPatient(patient != null ? patient.copy() : null);
        session.setCurrentProgramId("routine");
        session.setCurrentStepIndex(0);
        session.setSelectedChartId("va");
        session.setSelectedField(ExamSession.MeasurementField.SPH);
        session.setActiveEye(ExamSession.EyeSelection.BOTH);
        session.setLensVisibility(ExamSession.EyeSelection.BOTH);
        session.setDistanceMode(ExamSession.DistanceMode.FAR);
        session.setPrismMode(ExamSession.PrismMode.CARTESIAN);
        session.setLensDataSource(ExamSession.LensDataSource.SUBJECTIVE);
        session.setActiveTool(ExamSession.ToolType.NONE);
        session.setCylMinusMode(true);
        session.setCpLinked(false);
        session.setLensInserted(false);
        session.setShiftEnabled(false);
        session.setUnsavedChanges(false);
        session.setNote("已载入平板验光工作台。");

        session.getFarRight().setSph(-2.25);
        session.getFarRight().setCyl(-0.75);
        session.getFarRight().setAxis(175);
        session.getFarRight().setVa(0.8);
        session.getFarRight().setPd(32);
        session.getFarRight().setPrismX(0.0);
        session.getFarRight().setPrismY(0.0);

        session.getFarLeft().setSph(-2.00);
        session.getFarLeft().setCyl(-0.50);
        session.getFarLeft().setAxis(5);
        session.getFarLeft().setVa(0.9);
        session.getFarLeft().setPd(32);
        session.getFarLeft().setPrismX(0.0);
        session.getFarLeft().setPrismY(0.0);

        session.getNearRight().setAdd(1.25);
        session.getNearRight().setVa(0.9);
        session.getNearRight().setPd(32);
        session.getNearLeft().setAdd(1.25);
        session.getNearLeft().setVa(1.0);
        session.getNearLeft().setPd(32);

        session.getFinalRight().setSph(-2.00);
        session.getFinalRight().setCyl(-0.75);
        session.getFinalRight().setAxis(175);
        session.getFinalRight().setVa(1.0);
        session.getFinalRight().setPd(32);

        session.getFinalLeft().setSph(-1.75);
        session.getFinalLeft().setCyl(-0.50);
        session.getFinalLeft().setAxis(5);
        session.getFinalLeft().setVa(1.0);
        session.getFinalLeft().setPd(32);

        FunctionalTestState testState = session.getFunctionalTests();
        testState.setNpc(6);
        testState.setNpa(11);
        testState.setNra(2.00);
        testState.setPra(-2.25);
        testState.setAcaBi(2.0);
        testState.setAcaTarget(4.0);
        testState.setAmpRight(10);
        testState.setAmpLeft(9);
        testState.setNearLampOn(false);
        testState.setNraNote("Bin 模糊 +2.00D");
        testState.setPraNote("Bin 模糊 -2.25D");
        testState.setAcaNote("首次对齐完成");
        testState.setAmpNote("右眼主导");
        return session;
    }
}
