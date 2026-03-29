---
name: java-clean-refactor
description: 用于 Java 或 Android 项目的代码整洁检查、小步重构、坏味道识别、受保护边界判断、职责拆分和设计改进。适用于用户要求“重构”“清理代码”“优化命名”“减少重复”“先评审再决定要不要改”“判断这里该不该重构”“改善设计但尽量不改功能”这类场景。
---

# Java 小步重构

## 工作目标

在尽量不改变外部行为的前提下，提高代码可读性、可维护性和边界清晰度。

## 工作流程

1. 先判断任务属于哪一类：命名、函数拆分、类职责、重复代码、错误处理、依赖边界、模块落点、架构收敛。
2. 先判断当前代码是普通内部结构，还是受保护边界，例如生命周期、线程、socket、硬件通信、事务写入。
3. 先识别坏味道，再选择最小重构动作，不要先做大改。
4. 默认优先保持行为不变；如果实际上包含功能变更，明确指出，不要把功能修改伪装成重构。
5. 小步修改，每一步后做最小可行验证。
6. 最终说明必须覆盖：发现了什么问题、做了什么重构、哪些债务没有处理、验证了什么。

## 决策规则

- 优先处理高收益、低风险问题。
- 不为了“更优雅”而引入新框架或大规模迁移。
- 重命名必须明显提升意图表达，否则不动。
- 拆函数和拆类应围绕职责边界，而不是机械追求更短。
- 只有在重复模式或变化点已经稳定时才抽象，避免过早抽象。
- 对受保护边界，优先只动无副作用辅助逻辑，主时序、清理顺序和事务边界默认保守处理。
- 如果仓库里存在镜像实现或多模块重复代码，先确认是否应该同步修改。

## 参考资料

- 需要检查命名、函数、类职责、注释、错误处理时，读取 `references/clean-code-checklist.md`
- 需要根据坏味道选择重构动作时，读取 `references/refactoring-playbook.md`
- 需要判断模块边界、依赖方向、抽象时机时，读取 `references/architecture-boundaries.md`
- 需要看经过项目内验证的判断样例时，读取 `references/decision-patterns.md`
- 如果任务更偏 Android 工程开发、XML/ViewBinding 页面、Gradle 构建排错、生命周期和资源护栏，同时读取 `../android-java-delivery/SKILL.md`

## 知识地图

- 页面与入口页：工作台、设置表单、业务台账、程序页、报告页、设备连接页、历史详情页、Demo 首页、按钮集合页、Tab 场景页。
- 协调层与状态门面：`MainActivity`、`ClinicViewModel`、`CommandSettingsViewModel`、demo 系列 `ViewModel` 的接线骨架、小步收尾和显式场景清单。
- 设备通信边界：`TcpServerService`、`HeartbeatManager`、`DeviceManager`、`DeviceHistoryStore`、`Hc25MacDiscoveryClient` 这类线程、socket、前台服务、事务写入相关类的保守重构方法。
- 协议与命令域：`CommandEngine`、`ProtocolDispatcher / ProtocolDispatchResult`、`ProtocolGateway`、`CommandDefinition`、`CommandCode`、`CommandCatalog`、`CommandReservation / CommandTable`、`AckMessage`、`AckCodec / AckFactory`、`ProtocolInboundEvent` 一类协议对象、结果对象和分发骨架的判断方法。
- 传输与实时流：`TransferSender / TransferReceiver`、`TransferPacketCodec`、`StreamSender / StreamReceiver`、`StreamPacketCodec`、`StreamFrame`、`TransferMetadata`、`TransferChunk`、`StreamMetadata` 这类“状态机 + 校验 + 结束语义”核心的保护原则。
- 日志与导出链路：`JLog / JLogConfig`、`JLogEntry`、`DLog`、`JLogcatCollector`、`JZipDelegate`、`JLogExporter`、`DLogExporter`、`DLogZipDelegate`、`DLogNetworkInterceptor` 这类初始化、落盘、分享、预览边界的处理方式。
- 网络调用骨架：`NetworkConfig`、`NetworkServiceFactory`、`NetworkCallExecutor`、`ApiResult`、`GsonEnvelopeParser`、`EchoEnvelope` 这类默认值、字段映射、成功码、装配顺序和结果载体的保护方法。
- 基础 UI 与委托：`BaseVBActivity`、`BaseVBFragment`、`BaseVBBottomSheetDialog`、`BaseConfirmDialog`、`BaseClinicFragment`、`PermissionDelegate`、`PageTitleUIDelegate`、`ViewBindingReflector` 这类模板方法骨架、权限时序和延迟挂载委托的判断方法。
- 轻量模型与 Room 实体：`ClinicSettings`、`FunctionalTestState`、`ExamStep`、`ExamSession`、`LensMeasurement`、`PatientProfile`、`ReportRecord`、`DLogSettingsForm`、`ConnectedDeviceInfo`、`TrackedDeviceEntity / DeviceLogEntity` 这类“字段本身就是边界”的保留策略。
- demo / 教学型代码：BRVAH 示例、通信示例、网络示例、日志示例、功能面板类，重点是区分“示例清单本来就该显式”与“旁路重复可以收敛”，包括 `DemoViewModel`、`BaseBrvahScenarioFragment / BrvahLoadMoreFragment`、`BrvahDragSwipeFragment` 这类交互型示例。

## 快速导航

- 如果当前是页面类，优先去 `references/decision-patterns.md` 对照最接近的页面类型：工作台/容器页、设置表单页、业务台账页、程序页、报告页、设备连接页、历史详情页、目录入口页、按钮式场景集合页、Tab 场景页。
- 如果当前是设备通信或高风险基础设施，优先对照这些样本：`TcpServerService`、`HeartbeatManager`、`DeviceManager`、`DeviceHistoryStore`、`ProtocolDispatcher / ProtocolDispatchResult`、`ProtocolGateway`、`CommandEngine`、`AckCodec / AckFactory`、`TransferSender / TransferReceiver`、`TransferPacketCodec`、`StreamSender / StreamReceiver`、`StreamPacketCodec / StreamFrame`、`Hc25MacDiscoveryClient`、`DLogZipDelegate`、`JZipDelegate`、`JLogExporter`、`DLog / DLogExporter`、`JLogcatCollector`、`JLog / JLogConfig`、`JLogEntry`、`DLogNetworkInterceptor`、`NetworkConfig / NetworkCallExecutor / NetworkServiceFactory`、`GsonEnvelopeParser`。
- 如果当前是标题栏/权限委托、薄网关接口、基础页面基类或轻量数据载体，优先去 `references/decision-patterns.md` 对照 `PageTitleUIDelegate`、`PermissionDelegate`、`DeviceServiceGateway`、`BaseClinicFragment`、`BaseVBActivity`、`BaseVBFragment / ViewBindingReflector`、`BaseVBBottomSheetDialog`、`BaseConfirmDialog`、`ConnectedDeviceInfo / KnownDeviceSummary`、`TrackedDeviceEntity / DeviceLogEntity`、`ClinicSettings`、`FunctionalTestState`、`ExamStep`、`LensMeasurement / PatientProfile`、`ReportRecord`、`DLogSettingsForm`、`CommandCode`、`AckMessage / TransferMetadata`、`ProtocolInboundEvent / TransferChunk`、`StreamMetadata`、`ApiResult / EchoEnvelope`，先判断是不是其实“不该过度重构”。
- 如果当前是日志门面、日志配置、导出代理或网络调用骨架，优先去 `references/decision-patterns.md` 对照 `JLog / JLogConfig`、`JLogEntry`、`DLog / DLogExporter`、`JZipDelegate`、`NetworkConfig / NetworkCallExecutor / NetworkServiceFactory`、`ApiResult / EchoEnvelope`、`GsonEnvelopeParser`，先保护初始化、默认值、字段映射和成功/失败回调顺序。
- 如果当前是命令解析引擎、协议分发或发送门面，优先去 `references/decision-patterns.md` 对照 `CommandEngine`、`CommandReservation / CommandTable`、`ProtocolDispatcher / ProtocolDispatchResult`、`ProtocolGateway`、`AckCodec / AckFactory`、`TransferSender / TransferReceiver`，先保护匹配、校验、分发和传输语义。
- 如果当前是流式传输核心、实时流元数据或结束帧处理，优先去 `references/decision-patterns.md` 对照 `StreamMetadata`、`StreamSender / StreamReceiver`、`TransferSender / TransferReceiver`、`TransferPacketCodec`、`StreamPacketCodec / StreamFrame`，先保护序号、状态推进和结束语义。
- 如果当前是日志导出入口或隐藏触发器，优先去 `references/decision-patterns.md` 对照 `JLogExporter`、`JZipDelegate`、`DLogZipDelegate`，先保护注册时机、点击阈值和导出/分享分支。
- 如果当前是 demo / 教学型交互示例，尤其是拖拽、侧滑、分页、加载更多这类 BRVAH 场景，优先去 `references/decision-patterns.md` 对照 `BrvahDemoActivity`、`BaseBrvahScenarioFragment / BrvahLoadMoreFragment`、`BrvahDragSwipeFragment`、`BrvahDemoViewModel`、`BrvahDemoRepository`，先判断示例监听器和场景清单是不是本来就该显式保留。
- 如果当前是领域会话模型、配置快照或功能检查状态，优先去 `references/decision-patterns.md` 对照 `ClinicSettings`、`FunctionalTestState`、`ExamSession`，先判断这些集中字段是不是边界本身。
- 如果当前是自定义调试 View、悬浮浮层或带拖拽的诊断工具组件，优先去 `references/decision-patterns.md` 对照 `FlowDebugOverlayView`、`FlowDebugOverlay / FlowLogCenter`，先保护交互骨架、拖拽边界和监听时序。
- 如果当前是 demo / 教学型 Repository，优先去 `references/decision-patterns.md` 对照 `NetworkDemoRepository`、`BrvahDemoRepository`，先判断显式样例清单是不是本来就该保留。
- 如果当前是 demo / 教学型 ViewModel，优先去 `references/decision-patterns.md` 对照 `DemoViewModel`、`CommunicationDemoViewModel`、`NetworkDemoViewModel`、`BrvahDemoViewModel`，先判断显式场景入口是不是本来就该保留。
- 如果关键词已经比较明确，但 reference 太长不想手翻，优先运行 `scripts/find_refactor_rules.py <关键词...>`；如果已经知道要看的 Java 文件，也可以直接运行 `scripts/find_refactor_rules.py --file <文件路径>`，先缩小到最相关的文档和标题，再展开阅读。
- 如果当前类没有直接对应样本，先在 `references/decision-patterns.md` 找最接近的边界类型，再回到另外三份 references 选择重构动作，不要直接生造抽象。
- 如果本轮验证出了新的稳定模式，优先同步回写四份参考文档：`clean-code-checklist.md`、`refactoring-playbook.md`、`architecture-boundaries.md`、`decision-patterns.md`。

## 使用路线

- 如果任务是“做代码评审 / 看看哪里别扭 / 这段代码有没有坏味道”，优先读 `references/clean-code-checklist.md` 和 `references/decision-patterns.md`，先判断问题是不是成立，再决定是否动代码。
- 如果任务是“直接重构 / 减少重复 / 优化命名 / 提炼 helper”，优先先在 `references/decision-patterns.md` 找最近样本，再去 `references/refactoring-playbook.md` 选最小动作。
- 如果任务涉及生命周期、线程、socket、前台 Service、导出分享、持久化时序或设备通信，先读 `references/architecture-boundaries.md`，确认是不是受保护边界，再决定本轮只动旁路辅助逻辑还是保持不动。
- 如果任务同时涉及 `app` 和 `demo` 的镜像实现，先检查两个模块是否都需要同步调整；不要默认只改一边。
- 如果任务最后发现“不该重构”，也要明确输出原因，例如风险太高、收益太低、验证面不足，或者当前显式写法本身就是页面/领域边界的一部分。

## 典型触发

- “帮我重构这段 Java 代码，但尽量别改行为”
- “看看这个 Activity / Fragment 哪些地方可以小步整理”
- “这个 ViewModel / Repository 有没有坏味道，先评审再决定要不要改”
- “减少重复，但不要搞大抽象”
- “帮我判断这里是不是受保护边界，能不能安全重构”

## 脚本工具

- `scripts/find_refactor_rules.py <关键词...>`：按关键词快速定位最相关的 reference 标题和样例，适合文档变长后先做缩小范围。
- `scripts/find_refactor_rules.py --file <Java 文件路径>`：直接从类名和路径自动提取关键词，再优先命中这个文件对应的项目样本，然后补充最相关的规则和样例。
- `scripts/verify_find_refactor_rules.py`：回归检查一组真实类样本是否仍然先命中各自的项目样本，适合在新增 decision-patterns 或调整排序后跑一遍。
- `scripts/find_uncovered_refactor_candidates.py`：列出还没进入 `decision-patterns.md` 的候选 Java 类，适合继续补项目内判断样本时先选题。
- 示例：`python scripts/find_refactor_rules.py 设备 连接页`
- 示例：`python scripts/find_refactor_rules.py 报告 边界`
- 示例：`python scripts/find_refactor_rules.py --file app/src/main/java/com/wifi/optometry/ui/main/DeviceFragment.java`
- 示例：`python scripts/verify_find_refactor_rules.py --verbose`
- 示例：`python scripts/find_uncovered_refactor_candidates.py --limit 20`

## 输出要求

- 先说明判断结果，再说明改动动作。
- 如果判断为受保护边界，明确说明本轮只动了什么旁路辅助逻辑，或为什么选择不改。
- 如果选择不重构，明确说明原因，例如风险高、验证不足、收益过低或会引入过早抽象。
- 如果任务更适合做评审而不是直接修改，先列出主要问题和风险，再给建议方案。
