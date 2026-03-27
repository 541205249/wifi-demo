package com.example.wifidemo.sample.brvah.data;

import com.example.wifidemo.R;
import com.example.wifidemo.clinic.model.ExamProgram;
import com.example.wifidemo.clinic.model.ExamStep;
import com.example.wifidemo.clinic.model.KnownDeviceSummary;
import com.example.wifidemo.clinic.model.LensMeasurement;
import com.example.wifidemo.clinic.model.ReportRecord;
import com.example.wifidemo.clinic.model.VisionChart;
import com.example.wifidemo.clinic.model.VisualFunctionMetric;
import com.example.wifidemo.sample.brvah.model.BrvahWorkflowItem;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrvahDemoRepository extends BaseRepository {
    private static final String TAG = "BrvahDemoRepo";
    private static final int REPORT_PAGE_SIZE = 4;

    public List<KnownDeviceSummary> createKnownDevices() {
        long now = System.currentTimeMillis();
        List<KnownDeviceSummary> devices = new ArrayList<>();
        devices.add(new KnownDeviceSummary("98:DA:F0:31:2C:18", "主检模块 A", "192.168.1.40", now - 3 * 60_000L, true, 124, 38));
        devices.add(new KnownDeviceSummary("98:DA:F0:31:2C:19", "综合视标箱", "192.168.1.41", now - 16 * 60_000L, true, 89, 27));
        devices.add(new KnownDeviceSummary("98:DA:F0:31:2C:20", "雾视模块", "192.168.1.52", now - 41 * 60_000L, false, 47, 14));
        devices.add(new KnownDeviceSummary("98:DA:F0:31:2C:21", "近用补光模块", "192.168.1.63", now - 2 * 60 * 60_000L, false, 31, 11));
        devices.add(new KnownDeviceSummary("98:DA:F0:31:2C:22", "备用测试终端", "192.168.1.75", now - 26 * 60_000L, true, 56, 18));
        DLog.d(TAG, "构建已知模块示例数据，count=" + devices.size());
        return devices;
    }

    public List<VisionChart> createVisionCharts() {
        List<VisionChart> charts = new ArrayList<>();
        charts.add(new VisionChart("chart-distance", "远用视力表", "对数视标", "适合远距离裸眼与矫正视力检查。", R.drawable.chart_plate_42));
        charts.add(new VisionChart("chart-near", "近用阅读卡", "阅读训练", "用于近距离阅读舒适度与附加光确认。", R.drawable.chart_plate_25));
        charts.add(new VisionChart("chart-color", "偏振字母表", "双眼视功能", "适用于双眼平衡与主视眼辅助判断。", R.drawable.chart_polar_letters));
        charts.add(new VisionChart("chart-fixation", "注视网格", "黄斑注视", "适合筛查注视稳定性与中心暗点反馈。", R.drawable.chart_fixation_grid));
        charts.add(new VisionChart("chart-child", "儿童快速表", "单行视标", "适合儿童快速验光与复查场景。", R.drawable.chart_plate_5));
        DLog.d(TAG, "构建视标卡片示例数据，count=" + charts.size());
        return charts;
    }

    public List<BrvahWorkflowItem> createWorkflowItems() {
        List<BrvahWorkflowItem> items = new ArrayList<>();
        items.add(new BrvahWorkflowItem(
                BrvahWorkflowItem.TYPE_HEADER,
                "标准主观验光流程",
                "共 6 个阶段，支持跳步和功能检查插入",
                "适合作为 BaseMultiItemAdapter 的多布局示例。",
                "流程模板"
        ));
        items.add(new BrvahWorkflowItem(
                BrvahWorkflowItem.TYPE_STEP,
                "初始雾视",
                "双眼同时 +0.75D",
                "先稳定调节，再进入单眼精调。",
                "进行中"
        ));
        items.add(new BrvahWorkflowItem(
                BrvahWorkflowItem.TYPE_STEP,
                "单眼红绿平衡",
                "右眼优先，再切左眼",
                "根据被测者反馈微调球镜度数。",
                "待执行"
        ));
        items.add(new BrvahWorkflowItem(
                BrvahWorkflowItem.TYPE_STEP,
                "交叉圆柱精调",
                "柱镜轴位与柱镜度数",
                "结合雾视保留量与最佳矫正视力判断。",
                "待执行"
        ));
        items.add(new BrvahWorkflowItem(
                BrvahWorkflowItem.TYPE_ACTION,
                "插入视功能检查",
                "当双眼平衡差异明显时插入",
                "可在此节点挂接 Worth 四点、偏振融合或集合近点。",
                "可操作"
        ));
        DLog.d(TAG, "构建工作流示例数据，count=" + items.size());
        return items;
    }

    public List<ExamProgram> createExamPrograms() {
        List<ExamProgram> programs = new ArrayList<>();
        ExamProgram standard = new ExamProgram(
                "program-standard",
                "标准成人验光",
                "远近结合，含双眼平衡",
                "适合门店常规成人验光与复查。"
        );
        standard.getSteps().add(createStep("step-fog", "雾视准备", "建立调节缓冲，稳定主观判断。"));
        standard.getSteps().add(createStep("step-balance", "红绿平衡", "快速收敛到舒适清晰区间。"));
        standard.getSteps().add(createStep("step-final", "最终确认", "记录处方并输出结果。"));
        programs.add(standard);

        ExamProgram child = new ExamProgram(
                "program-child",
                "儿童快速筛查",
                "流程更短，优先保证配合度",
                "适合低龄儿童或随访复查。"
        );
        child.getSteps().add(createStep("step-child-1", "快速裸眼筛查", "先建立基础视力水平。"));
        child.getSteps().add(createStep("step-child-2", "单眼精调", "根据配合度选择是否完整执行。"));
        programs.add(child);

        ExamProgram binocular = new ExamProgram(
                "program-binocular",
                "双眼视功能评估",
                "用于主诉视疲劳或斜位人群",
                "可在常规处方基础上追加视功能模块。"
        );
        binocular.getSteps().add(createStep("step-vf-1", "主视眼判断", "结合偏振表与主诉确认。"));
        binocular.getSteps().add(createStep("step-vf-2", "融合范围测量", "记录近距/远距融合储备。"));
        binocular.getSteps().add(createStep("step-vf-3", "集合近点", "评估调节与集合协调。"));
        programs.add(binocular);
        DLog.d(TAG, "构建验光流程示例数据，count=" + programs.size());
        return programs;
    }

    public List<ReportRecord> createAllReports() {
        long now = System.currentTimeMillis();
        List<ReportRecord> reports = new ArrayList<>();
        reports.add(createReport("report-001", "陈晨", "标准成人验光", now - 2 * 60 * 60_000L, "双眼矫正 1.0", "R:-2.25/-0.50x180  L:-2.00/-0.75x175"));
        reports.add(createReport("report-002", "李诺", "儿童快速筛查", now - 4 * 60 * 60_000L, "右眼 0.8 左眼 0.9", "建议 3 个月后复查"));
        reports.add(createReport("report-003", "王宁", "双眼视功能评估", now - 7 * 60 * 60_000L, "近距融合偏低", "建议增加视功能训练"));
        reports.add(createReport("report-004", "周安", "标准成人验光", now - 12 * 60 * 60_000L, "双眼矫正 1.2", "R:-4.00/-1.00x10  L:-3.75/-1.25x170"));
        reports.add(createReport("report-005", "赵一", "标准成人验光", now - 26 * 60 * 60_000L, "夜间眩光主诉明显", "建议加做眩光测试"));
        reports.add(createReport("report-006", "孙乐", "儿童快速筛查", now - 36 * 60 * 60_000L, "远视储备偏低", "建议减少近距离用眼"));
        reports.add(createReport("report-007", "刘清", "双眼视功能评估", now - 48 * 60 * 60_000L, "集合近点偏远", "建议纳入训练方案"));
        reports.add(createReport("report-008", "何妍", "标准成人验光", now - 60 * 60 * 60_000L, "双眼矫正 1.0", "R:-1.50/-0.25x5  L:-1.25/-0.50x178"));
        reports.add(createReport("report-009", "杨帆", "标准成人验光", now - 72 * 60 * 60_000L, "新增近用附加需求", "ADD +1.25"));
        DLog.d(TAG, "构建报告分页示例数据，count=" + reports.size());
        return reports;
    }

    public List<ReportRecord> getReportPage(List<ReportRecord> allReports, int pageIndex) {
        int fromIndex = pageIndex * REPORT_PAGE_SIZE;
        if (fromIndex >= allReports.size()) {
            DLog.w(TAG, "请求报告分页超出范围，pageIndex=" + pageIndex);
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + REPORT_PAGE_SIZE, allReports.size());
        DLog.d(TAG, "切分报告分页，pageIndex=" + pageIndex + ", from=" + fromIndex + ", to=" + toIndex);
        return new ArrayList<>(allReports.subList(fromIndex, toIndex));
    }

    public int getReportPageSize() {
        return REPORT_PAGE_SIZE;
    }

    public String formatSeenTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private ExamStep createStep(String id, String title, String description) {
        return new ExamStep(
                id,
                title,
                description,
                "",
                ExamStep.DistanceMode.BOTH,
                ExamStep.EyeScope.BOTH,
                "",
                "",
                "",
                "",
                "",
                "",
                ExamStep.Comparator.NONE,
                0,
                ""
        );
    }

    private ReportRecord createReport(
            String id,
            String patientName,
            String programName,
            long createdAt,
            String visionSummary,
            String prescriptionSummary
    ) {
        ReportRecord record = new ReportRecord(
                id,
                patientName,
                programName,
                createdAt,
                visionSummary,
                prescriptionSummary,
                "https://demo.local/report/" + id,
                new LensMeasurement(-2.25, -0.50, 180, 0, 1.0, 31.5, 0, 0, 0, 0),
                new LensMeasurement(-2.00, -0.75, 175, 0, 1.0, 31.5, 0, 0, 0, 0)
        );
        record.getMetrics().add(new VisualFunctionMetric("远用视力", "矫正视力", "VA", "1.0", ">=1.0", "正常", "主观反馈稳定"));
        record.getMetrics().add(new VisualFunctionMetric("双眼平衡", "偏振平衡", "Balance", "通过", "通过", "稳定", "双眼主观反馈一致"));
        return record;
    }
}

