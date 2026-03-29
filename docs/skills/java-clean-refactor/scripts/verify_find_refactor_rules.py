from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path

from find_refactor_rules import (
    build_file_query,
    derive_keywords_from_file,
    load_sections,
    rank_sections,
    unique_keywords,
)


@dataclass(frozen=True)
class VerifyCase:
    file_path: str
    expected_heading: str
    keywords: tuple[str, ...] = ()


CASES = [
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/ui/MainActivity.java",
        expected_heading="MainActivity: 导航与回调收尾",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/MainActivity.java",
        expected_heading="demo MainActivity: 传统 Demo 入口页保留线性流程，服务回调桥接小步收敛",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/communication/device/DeviceManager.java",
        expected_heading="DeviceManager: 并发通信边界受保护",
        keywords=("边界",),
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/communication/device/Hc25MacDiscoveryClient.java",
        expected_heading="Hc25MacDiscoveryClient: 协议解析优先纯化",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/device/Hc25MacDiscoveryClient.java",
        expected_heading="Hc25MacDiscoveryClient: 协议解析优先纯化",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/communication/TcpServerService.java",
        expected_heading="TcpServerService: 前台 Service 属于受保护边界",
        keywords=("边界",),
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/TcpServerService.java",
        expected_heading="TcpServerService: 前台 Service 属于受保护边界",
        keywords=("边界",),
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/communication/HeartbeatManager.java",
        expected_heading="HeartbeatManager: 定时保活与清理顺序属于受保护边界",
        keywords=("边界",),
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/HeartbeatManager.java",
        expected_heading="HeartbeatManager: 定时保活与清理顺序属于受保护边界",
        keywords=("边界",),
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/logcat/JLogcatCollector.java",
        expected_heading="JLogcatCollector: 日志主时序保护，纯格式化可抽",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/zip/JZipDelegate.java",
        expected_heading="JZipDelegate: 导出代理先保护目录选择、分享与回调时序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/JLog.java",
        expected_heading="JLog / JLogConfig: 日志门面先保护启停语义，配置对象保持 builder 默认值",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/JLogConfig.java",
        expected_heading="JLog / JLogConfig: 日志门面先保护启停语义，配置对象保持 builder 默认值",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/JLogEntry.java",
        expected_heading="JLogEntry: 日志记录值对象保留展示拼装与优先级映射语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/JLogExporter.java",
        expected_heading="JLogExporter: 导出入口门面先保护注册时机与五击触发语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/DLog.java",
        expected_heading="DLog / DLogExporter: 日志包装门面与导出入口保持薄封装和触发语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/log/DLogExporter.java",
        expected_heading="DLog / DLogExporter: 日志包装门面与导出入口保持薄封装和触发语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/network/DLogNetworkInterceptor.java",
        expected_heading="DLogNetworkInterceptor: 网络日志拦截器先保护预览边界与记录顺序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/network/NetworkConfig.java",
        expected_heading="NetworkConfig / NetworkCallExecutor: 网络配置值对象与执行骨架先保护默认值和回调顺序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/network/NetworkCallExecutor.java",
        expected_heading="NetworkConfig / NetworkCallExecutor: 网络配置值对象与执行骨架先保护默认值和回调顺序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/network/NetworkServiceFactory.java",
        expected_heading="NetworkConfig / NetworkCallExecutor: 网络配置值对象与执行骨架先保护默认值和回调顺序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/network/ApiResult.java",
        expected_heading="ApiResult / EchoEnvelope: 网络返回载体保留静态工厂和直接字段结构",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/network/gson/GsonEnvelopeParser.java",
        expected_heading="GsonEnvelopeParser: JSON 包装解析器先保护字段映射与成功码语义",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/ui/main/SettingsFragment.java",
        expected_heading="SettingsFragment: 设置表单页的显式字段映射优先保留",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/ui/DemoFeatureFragment.java",
        expected_heading="DemoFeatureFragment: 演示动作面板保留显式入口，局部交互 helper 可抽",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/ui/DemoViewModel.java",
        expected_heading="DemoViewModel: 演示页 ViewModel 保留显式动作，状态同步 helper 留在本类",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/BaseVBActivity.java",
        expected_heading="BaseVBActivity: 轻量 ViewBinding 基类先保持模板方法骨架",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/BaseVBFragment.java",
        expected_heading="BaseVBFragment / ViewBindingReflector: Fragment 模板骨架与反射 inflate 边界先保护",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/internal/ViewBindingReflector.java",
        expected_heading="BaseVBFragment / ViewBindingReflector: Fragment 模板骨架与反射 inflate 边界先保护",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/BaseVBBottomSheetDialog.java",
        expected_heading="BaseVBBottomSheetDialog: BottomSheet 模板基类先保护初始化与展示位置骨架",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/BaseConfirmDialog.java",
        expected_heading="BaseConfirmDialog: 基础确认弹框保留显式按钮入口与文案设置",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/delegate/PageTitleUIDelegate.java",
        expected_heading="PageTitleUIDelegate: 标题栏委托保留延迟挂载骨架，显式入口比统一配置更重要",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/baseui/delegate/PermissionDelegate.java",
        expected_heading="PermissionDelegate: 权限闸门委托优先抽共用骨架，不改申请时序",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/ConnectedDeviceInfo.java",
        expected_heading="ConnectedDeviceInfo / KnownDeviceSummary: 轻量摘要模型保持直接字段结构",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/db/TrackedDeviceEntity.java",
        expected_heading="TrackedDeviceEntity / DeviceLogEntity: Room 实体优先保持字段直达与空值归一化",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/db/DeviceLogEntity.java",
        expected_heading="TrackedDeviceEntity / DeviceLogEntity: Room 实体优先保持字段直达与空值归一化",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandViewHelper.java",
        expected_heading="CommandViewHelper / OptometryCommandCodes: 显式命令码清单保留，纯提示文案小步提炼",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/gateway/ProtocolGateway.java",
        expected_heading="ProtocolGateway: 协议门面保留显式分支，直达发送入口不必再套壳",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/dispatcher/ProtocolDispatchResult.java",
        expected_heading="ProtocolDispatcher: 分发核心先保护语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandEngine.java",
        expected_heading="CommandEngine: 命令解析引擎先保护匹配、分发与模板替换语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandSettingsRepository.java",
        expected_heading="CommandSettingsRepository: 编码表仓库先保护加载与替换顺序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandDefinition.java",
        expected_heading="CommandDefinition: 协议定义值对象保留字段与匹配语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandReservation.java",
        expected_heading="CommandReservation / CommandTable: 命令预留位与已加载编码表保留显式索引和匹配语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandTable.java",
        expected_heading="CommandReservation / CommandTable: 命令预留位与已加载编码表保留显式索引和匹配语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/CommandCode.java",
        expected_heading="CommandCode: 协议编码值对象保留归一化、分段与比较语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/ack/AckCodec.java",
        expected_heading="AckCodec / AckFactory: ACK 文本协议先保护前缀、字段约定与渠道语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/ack/AckFactory.java",
        expected_heading="AckCodec / AckFactory: ACK 文本协议先保护前缀、字段约定与渠道语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/ack/AckMessage.java",
        expected_heading="AckMessage / TransferMetadata: 协议载荷值对象保留 builder 与局部派生语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/gateway/ProtocolInboundEvent.java",
        expected_heading="ProtocolInboundEvent / TransferChunk: 协议入站封装与分块值对象保留工厂入口和校验语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/transfer/TransferMetadata.java",
        expected_heading="AckMessage / TransferMetadata: 协议载荷值对象保留 builder 与局部派生语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/transfer/TransferChunk.java",
        expected_heading="ProtocolInboundEvent / TransferChunk: 协议入站封装与分块值对象保留工厂入口和校验语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/stream/StreamMetadata.java",
        expected_heading="StreamMetadata: 实时流元数据值对象保留 builder 默认值与校验语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/transfer/TransferPacketCodec.java",
        expected_heading="TransferPacketCodec / StreamPacketCodec / StreamFrame: 文本帧编解码与流帧值对象先保护字段顺序、长度校验和结束语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/stream/StreamPacketCodec.java",
        expected_heading="TransferPacketCodec / StreamPacketCodec / StreamFrame: 文本帧编解码与流帧值对象先保护字段顺序、长度校验和结束语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/stream/StreamFrame.java",
        expected_heading="TransferPacketCodec / StreamPacketCodec / StreamFrame: 文本帧编解码与流帧值对象先保护字段顺序、长度校验和结束语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/transfer/TransferSender.java",
        expected_heading="TransferSender / TransferReceiver: 传输链路核心先保护分块与校验语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/transfer/TransferReceiver.java",
        expected_heading="TransferSender / TransferReceiver: 传输链路核心先保护分块与校验语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/stream/StreamSender.java",
        expected_heading="StreamSender / StreamReceiver: 实时流核心先保护序号、状态与收发边界",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/command/stream/StreamReceiver.java",
        expected_heading="StreamSender / StreamReceiver: 实时流核心先保护序号、状态与收发边界",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/ui/main/ReportFragment.java",
        expected_heading="ReportFragment: 报告页保留显式汇总骨架，摘要与卡片 helper 可抽",
        keywords=("边界",),
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/network/data/NetworkDemoRepository.java",
        expected_heading="NetworkDemoRepository: 演示请求清单保留显式，响应预览 helper 可收敛",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahDemoViewModel.java",
        expected_heading="BrvahDemoViewModel: BRVAH 场景清单保留显式，分页状态骨架留在本类更清楚",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/brvah/data/BrvahDemoRepository.java",
        expected_heading="BrvahDemoRepository: 演示素材仓库保留显式样例清单，分页切片 helper 保持直接",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahDragSwipeFragment.java",
        expected_heading="BrvahDragSwipeFragment: 拖拽侧滑示例保留显式监听器骨架，状态提示与持久化收尾可抽",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BaseBrvahScenarioFragment.java",
        expected_heading="BaseBrvahScenarioFragment / BrvahLoadMoreFragment: BRVAH 场景基类与加载更多示例保留模板骨架和尾部状态映射",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahLoadMoreFragment.java",
        expected_heading="BaseBrvahScenarioFragment / BrvahLoadMoreFragment: BRVAH 场景基类与加载更多示例保留模板骨架和尾部状态映射",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/log/model/DLogSettingsForm.java",
        expected_heading="DLogSettingsForm: 日志表单快照保留字符串字段与 fromConfig 语义",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/sample/network/model/EchoEnvelope.java",
        expected_heading="ApiResult / EchoEnvelope: 网络返回载体保留静态工厂和直接字段结构",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/ClinicSettings.java",
        expected_heading="ClinicSettings: 镜像配置模型保持直接字段与 copy 语义",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/ClinicSettings.java",
        expected_heading="ClinicSettings: 镜像配置模型保持直接字段与 copy 语义",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/FunctionalTestState.java",
        expected_heading="FunctionalTestState: 镜像功能检查状态模型保持直接字段与 copy 语义",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/FunctionalTestState.java",
        expected_heading="FunctionalTestState: 镜像功能检查状态模型保持直接字段与 copy 语义",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/ExamStep.java",
        expected_heading="ExamStep: 镜像流程步骤描述保持枚举与跳步字段集中",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/ExamStep.java",
        expected_heading="ExamStep: 镜像流程步骤描述保持枚举与跳步字段集中",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/ExamSession.java",
        expected_heading="ExamSession: 镜像领域会话模型保持集中状态字段",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/ExamSession.java",
        expected_heading="ExamSession: 镜像领域会话模型保持集中状态字段",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/LensMeasurement.java",
        expected_heading="LensMeasurement / PatientProfile: 镜像轻量领域模型保持 copy 与少量展示 helper",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/LensMeasurement.java",
        expected_heading="LensMeasurement / PatientProfile: 镜像轻量领域模型保持 copy 与少量展示 helper",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/PatientProfile.java",
        expected_heading="LensMeasurement / PatientProfile: 镜像轻量领域模型保持 copy 与少量展示 helper",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/PatientProfile.java",
        expected_heading="LensMeasurement / PatientProfile: 镜像轻量领域模型保持 copy 与少量展示 helper",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/domain/model/ReportRecord.java",
        expected_heading="ReportRecord: 镜像报告模型保持直接字段聚合与列表容器语义",
    ),
    VerifyCase(
        file_path="demo/src/main/java/com/example/wifidemo/clinic/model/ReportRecord.java",
        expected_heading="ReportRecord: 镜像报告模型保持直接字段聚合与列表容器语义",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/flowdebug/FlowDebugOverlayView.java",
        expected_heading="FlowDebugOverlayView: 调试浮层保留交互骨架，拖拽与日志订阅先别打散",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/flowdebug/FlowDebugOverlay.java",
        expected_heading="FlowDebugOverlay / FlowLogCenter: 浮层宿主与日志中心先保护挂载和订阅时序",
    ),
    VerifyCase(
        file_path="lib/src/main/java/com/wifi/lib/flowdebug/FlowLogCenter.java",
        expected_heading="FlowDebugOverlay / FlowLogCenter: 浮层宿主与日志中心先保护挂载和订阅时序",
    ),
    VerifyCase(
        file_path="app/src/main/java/com/wifi/optometry/ui/state/DeviceServiceGateway.java",
        expected_heading="DeviceServiceGateway: 薄网关接口保持直接，不要为了层次继续套壳",
    ),
]


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="回归检查 find_refactor_rules.py 是否仍能优先命中正确项目样本。")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parent.parent,
        help="skill 根目录，默认取脚本所在 skill 目录",
    )
    parser.add_argument("--verbose", action="store_true", help="输出每个样本的前三条结果，便于排查排序变化。")
    return parser


def resolve_repo_root(skill_root: Path) -> Path:
    return skill_root.parents[2]


def main() -> int:
    args = build_parser().parse_args()
    skill_root = args.root.resolve()
    repo_root = resolve_repo_root(skill_root)
    sections = load_sections(skill_root / "references")

    failures: list[str] = []
    for case in CASES:
        file_path = repo_root / case.file_path
        keywords = unique_keywords(derive_keywords_from_file(file_path) + list(case.keywords))
        ranked = rank_sections(sections, keywords, build_file_query(file_path))
        if not ranked:
            failures.append(f"{case.file_path} -> 没有匹配结果")
            print(f"[失败] {case.file_path}")
            print("  没有匹配结果")
            continue

        top = ranked[0]
        matched = top.section.heading == case.expected_heading
        status = "通过" if matched else "失败"
        print(f"[{status}] {case.file_path}")
        print(f"  顶条: {top.section.heading}")
        if case.keywords:
            print(f"  附加关键词: {' / '.join(case.keywords)}")
        if args.verbose or not matched:
            for index, result in enumerate(ranked[:3], start=1):
                print(f"  {index}. {result.section.heading} (分数={result.score}, 匹配词={result.matched_keywords})")
        if not matched:
            failures.append(
                f"{case.file_path} -> 期望 `{case.expected_heading}`，实际 `{top.section.heading}`"
            )

    print("")
    if failures:
        print(f"回归失败：{len(failures)} 条")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(f"回归通过：{len(CASES)} 条样本全部命中预期顶条。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
