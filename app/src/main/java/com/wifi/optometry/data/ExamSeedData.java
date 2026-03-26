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
        return patients;
    }

    public static List<VisionChart> createCharts() {
        List<VisionChart> charts = new ArrayList<>();
        charts.add(new VisionChart("phoria", "隐斜视", "远近隐斜检测", "用于水平与垂直隐斜位初筛，配合棱镜或偏振分离观察双眼对位。", R.drawable.chart_fixation_grid));
        charts.add(new VisionChart("fixation_phoria", "带固视隐斜", "中心固视 + 周边分离", "适合在维持中心固视条件下观察隐斜位补偿情况。", R.drawable.chart_fixation_grid));
        charts.add(new VisionChart("stereo", "立体视", "双眼融合判断", "通过双眼视差图案观察立体感知与融合能力。", R.drawable.chart_plate_42));
        charts.add(new VisionChart("vertical", "垂直重合", "上下对齐", "用于垂直偏差与对齐能力检查，常和 AC/A、融像功能一起记录。", R.drawable.guide_lens_step));
        charts.add(new VisionChart("worth", "Worth 4", "四点融像", "用于抑制、复视和双眼融合状态判断，可作为视功能程序的基础筛查。", R.drawable.chart_plate_25));
        charts.add(new VisionChart("polar", "偏振红绿", "偏振分离", "用于红绿平衡与视标分离演示，适合主观验光末段的平衡确认。", R.drawable.chart_polar_letters));
        charts.add(new VisionChart("va", "VA 表", "视力表", "根据当前矫正视力自动切换视标范围，适配远距与近距模式。", R.drawable.chart_plate_5));
        charts.add(new VisionChart("balance", "双眼平衡", "双眼平衡图", "用于双眼最佳视力后做平衡调整，确认主观舒适度与双眼协同。", R.drawable.chart_polar_letters));
        return charts;
    }

    public static List<ExamProgram> createPrograms() {
        List<ExamProgram> programs = new ArrayList<>();

        ExamProgram routine = new ExamProgram(
                "routine",
                "常规验光",
                "主观验光主线流程",
                "覆盖雾视、去雾视、散光轴位/度数调整、红绿检测、双眼平衡和试戴。"
        );
        routine.getSteps().add(new ExamStep("rt_01", "雾视", "先建立雾视量，保证调节放松。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "SubJ", "SPH", "+0.75", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_02", "去雾视（右眼）", "右眼逐步减雾并观察最佳视力。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "SPH", "+0.25", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_03", "粗调散光轴位（右眼）", "结合散光表或交叉格栅完成粗调。", "phoria", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "AXIS", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_04", "粗调散光度数（右眼）", "以柱镜步长推进，找到清晰方向。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "CYL", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_05", "首次红绿检测（右眼）", "利用红绿平衡确认球镜方向。", "polar", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.RIGHT, "SubJ", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_06", "去雾视（左眼）", "左眼逐步减雾并观察最佳视力。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "SPH", "+0.25", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_07", "粗调散光轴位（左眼）", "左眼轴位粗调。", "phoria", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "AXIS", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_08", "粗调散光度数（左眼）", "左眼柱镜粗调。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.LEFT, "SubJ", "CYL", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_09", "双眼最佳视力检测", "合并双眼后确认最佳视力。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_10", "双眼平衡测试", "调整双眼舒适平衡。", "balance", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_11", "双眼 MPMVA", "最正最高视力复核。", "va", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "SPH", "", "关", "", "", ExamStep.Comparator.NONE, 0, ""));
        routine.getSteps().add(new ExamStep("rt_12", "试戴", "确认远近舒适度与处方接受度。", "polar", ExamStep.DistanceMode.BOTH, ExamStep.EyeScope.BOTH, "Final", "VA", "", "开", "试戴", "", ExamStep.Comparator.NONE, 0, ""));
        programs.add(routine);

        ExamProgram quick = new ExamProgram("quick_screen", "视功能快速筛查", "五项快速筛查", "适合门店快速初筛：双眼融合、NRA/PRA、远近眼位。");
        quick.getSteps().add(new ExamStep("qs_01", "双眼融合测试", "先做 Worth4 融合判断。", "worth", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "关", "WORTH 4", "", ExamStep.Comparator.NONE, 0, ""));
        quick.getSteps().add(new ExamStep("qs_02", "NRA 负相对调节", "在近距模式下推进正镜直到模糊。", "polar", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "NRA", "", ExamStep.Comparator.NONE, 0, ""));
        quick.getSteps().add(new ExamStep("qs_03", "PRA 正相对调节", "在近距模式下推进负镜直到模糊。", "polar", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "PRA", "", ExamStep.Comparator.NONE, 0, ""));
        quick.getSteps().add(new ExamStep("qs_04", "远眼位", "记录远距隐斜和水平融像。", "phoria", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "关", "远眼位", "", ExamStep.Comparator.NONE, 0, ""));
        quick.getSteps().add(new ExamStep("qs_05", "近眼位", "记录近距隐斜和集合能力。", "fixation_phoria", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "近眼位", "", ExamStep.Comparator.NONE, 0, ""));
        programs.add(quick);

        ExamProgram full = new ExamProgram("full_visual", "全套视功能检查", "九项全流程", "覆盖双眼融合、立体视、远近眼位、AC/A、NRA/PRA、BCC、AMP 与水平融像。");
        full.getSteps().add(new ExamStep("fv_01", "双眼融合检测", "从 Worth4 开始确认基础融合。", "worth", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "关", "双眼融合", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_02", "立体视检测", "记录立体视觉级别。", "stereo", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "VA", "", "开", "立体视", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_03", "远眼位", "远距隐斜与融像发散/集合。", "phoria", ExamStep.DistanceMode.FAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "关", "远眼位", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_04", "近眼位", "近距隐斜与集合能力。", "fixation_phoria", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "近眼位", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_05", "AC/A（梯度法）", "在近距模式记录 BI 与目标偏移。", "vertical", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "AC/A", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_06", "NRA 负相对调节", "记录模糊与恢复。", "polar", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "NRA", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_07", "PRA 正相对调节", "记录模糊与恢复。", "polar", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "PRA", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_08", "AMP 检查", "分别记录 R/L 调节幅度。", "va", ExamStep.DistanceMode.NEAR, ExamStep.EyeScope.BOTH, "Final", "ADD", "", "开", "AMP", "", ExamStep.Comparator.NONE, 0, ""));
        full.getSteps().add(new ExamStep("fv_09", "远近水平融像", "完成最终功能汇总。", "balance", ExamStep.DistanceMode.BOTH, ExamStep.EyeScope.BOTH, "Final", "X", "", "开", "水平融像", "", ExamStep.Comparator.NONE, 0, ""));
        programs.add(full);

        return programs;
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
        settings.setSphStep(0.25);
        settings.setSphShiftStep(1.00);
        settings.setCylStep(0.25);
        settings.setCylShiftStep(1.00);
        settings.setAxisStep(5);
        settings.setAxisShiftStep(30);
        settings.setPrismStep(0.5);
        settings.setPrismShiftStep(2.0);
        settings.setPdStep(0.5);
        settings.setPdShiftStep(3.0);
        return settings;
    }

    public static ExamSession createInitialSession(PatientProfile patient) {
        ExamSession session = new ExamSession();
        session.setPatient(patient != null ? patient.copy() : null);
        session.setCurrentProgramId("routine");
        session.setCurrentStepIndex(0);
        session.setSelectedChartId("va");
        session.setSelectedField(ExamSession.MeasurementField.SPH);
        session.setActiveEye(ExamSession.EyeSelection.RIGHT);
        session.setLensVisibility(ExamSession.EyeSelection.BOTH);
        session.setDistanceMode(ExamSession.DistanceMode.FAR);
        session.setPrismMode(ExamSession.PrismMode.CARTESIAN);
        session.setCylMinusMode(true);
        session.setCpLinked(false);
        session.setLensInserted(false);
        session.setShiftEnabled(false);
        session.setNote("按常规验光流程开始。");

        session.getFarRight().setSph(-2.25);
        session.getFarRight().setCyl(-0.75);
        session.getFarRight().setAxis(175);
        session.getFarRight().setVa(0.8);
        session.getFarRight().setPd(31);

        session.getFarLeft().setSph(-2.00);
        session.getFarLeft().setCyl(-0.50);
        session.getFarLeft().setAxis(5);
        session.getFarLeft().setVa(0.9);
        session.getFarLeft().setPd(31);

        session.getNearRight().setAdd(1.25);
        session.getNearRight().setVa(0.9);
        session.getNearLeft().setAdd(1.25);
        session.getNearLeft().setVa(1.0);

        session.getFinalRight().setSph(-2.00);
        session.getFinalRight().setCyl(-0.75);
        session.getFinalRight().setAxis(175);
        session.getFinalRight().setVa(1.0);
        session.getFinalRight().setPd(31);

        session.getFinalLeft().setSph(-1.75);
        session.getFinalLeft().setCyl(-0.50);
        session.getFinalLeft().setAxis(5);
        session.getFinalLeft().setVa(1.0);
        session.getFinalLeft().setPd(31);

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
