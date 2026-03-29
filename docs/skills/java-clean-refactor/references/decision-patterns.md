# 项目内判断样例

这份文件记录了已经在当前仓库里验证过的重构判断模式，用于帮助后续类似场景快速决策。

## 目录

- 协调层与状态门面：`MainActivity`、`ClinicViewModel`、`ClinicRepository`、`CommandSettingsViewModel`、`CommandSettingsRepository`、`DLogSettingsRepository`、`DemoViewModel`
- 设备通信与高风险基础设施：`Hc25MacDiscoveryClient`、`DeviceManager`、`DeviceHistoryStore`、`TcpServerService`、`HeartbeatManager`、`ProtocolDispatcher / ProtocolDispatchResult`、`ProtocolGateway`、`CommandEngine`、`AckCodec / AckFactory`、`TransferSender / TransferReceiver`、`TransferPacketCodec / StreamPacketCodec / StreamFrame`、`StreamSender / StreamReceiver`、`DLogZipDelegate`、`JZipDelegate`、`JLogExporter`、`DLog / DLogExporter`、`JLogcatCollector`、`JLog / JLogConfig`、`JLogEntry`、`DLogNetworkInterceptor`、`NetworkConfig / NetworkCallExecutor / NetworkServiceFactory`、`GsonEnvelopeParser`
- 业务规则与配置素材：`ExamWorkflowEngine`、`ExamSeedData`、`CommandCatalog / OptometryCommandCatalogs`、`CommandDefinition`、`CommandReservation / CommandTable`、`ClinicFormatters`
- 业务页面与入口页：`WorkbenchFragment`、`SettingsFragment`、`DeviceHistoryActivity`、`PatientFragment`、`ProgramFragment`、`ReportFragment`、`DeviceFragment`、`CommandSettingsActivity`、`BaseClinicFragment`、`BaseVBActivity`
- 基础 UI 委托、薄接口与轻量数据结构：`PageTitleUIDelegate`、`BaseVBFragment / ViewBindingReflector`、`BaseVBBottomSheetDialog`、`BaseConfirmDialog`、`DeviceServiceGateway`、`ConnectedDeviceInfo / KnownDeviceSummary`、`TrackedDeviceEntity / DeviceLogEntity`、`ClinicSettings`、`FunctionalTestState`、`ExamStep`、`ExamSession`、`LensMeasurement / PatientProfile`、`ReportRecord`、`DLogSettingsForm`、`CommandCode`、`AckMessage / TransferMetadata`、`ProtocolInboundEvent / TransferChunk`、`StreamMetadata`、`ApiResult / EchoEnvelope`
- demo / 教学型页面与工具：`demo MainActivity`、`CommunicationDemoViewModel`、`NetworkDemoViewModel`、`NetworkDemoRepository`、`DemoFeatureFragment`、`DemoHomeActivity`、`DemoMvvmActivity`、`CommunicationDemoActivity`、`BrvahDemoActivity`、`BrvahDragSwipeFragment`、`BaseBrvahScenarioFragment / BrvahLoadMoreFragment`、`BrvahDemoViewModel`、`BrvahDemoRepository`、`LogSettingsActivity / LogSettingsViewModel`、`FlowDebugOverlayView`、`FlowDebugOverlay / FlowLogCenter`、`PermissionDelegate`

## MainActivity: 导航与回调收尾

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/MainActivity.java`

判断：
- `openDestination(...)` 属于协调层路由分发，适合把“路由决策”和“执行切换”拆开。
- 服务回调中的“追加控制台 + 刷新状态”属于稳定收尾动作，适合提炼公共辅助函数。

采取动作：
- 保留入口类编排职责。
- 提炼 `NavigationTarget` 和 `resolveNavigationTarget(...)`。
- 提炼 `appendConsoleAndRefresh(...)`。

没有做的事：
- 没有把导航分发挪到独立 Router 类。
- 没有把所有服务回调折叠成通用事件总线。

原因：
- 当前规则数量小、生命周期清晰，继续保持可见编排更容易维护。

## ClinicViewModel: 载荷解析与服务协调

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/state/ClinicViewModel.java`

判断：
- `importPatientFromCode(...)` 同时承担命令入口和载荷解析，适合先分离解析。
- `startServer/stopServer/broadcast/send` 的网关检查和收尾动作高度重复，适合提炼辅助函数。

采取动作：
- 提炼 `parseImportedPatientProfile(...)` 和 `applyImportedPatientParts(...)`。
- 提炼 `ensureDeviceServiceGateway(...)` 和 `appendConsoleAndRefreshDeviceState(...)`。

没有做的事：
- 没有把设备服务协调整体挪出 ViewModel。

原因：
- 当前 ViewModel 仍然是稳定门面，先收敛重复副作用比直接拆层更稳。

## ClinicRepository: 状态模板与不要过度表驱动

对应代码：
- `app/src/main/java/com/wifi/optometry/data/ClinicRepository.java`

判断：
- `DeviceUiState` 相关方法重复的是“取状态 -> 变更 -> 发布 -> 可选日志”，适合模板化。
- `adjustFunctionalValue(...)` 的分支虽然相似，但步长、边界和符号语义并不统一，不适合立即表驱动。
- `buildVisualMetrics(...)` 本质上是在显式列业务行，强行表驱动会削弱业务可读性。

采取动作：
- 提炼 `updateDeviceUiState(...)` 模板。
- 提炼 `FUNCTION_KEY_*` 常量，降低字符串分叉风险。

没有做的事：
- 没有把视功能调整规则改成配置表。
- 没有把 `buildVisualMetrics(...)` 改成数据驱动结构。

原因：
- 表面重复不足以支撑统一抽象，当前保持显式更清楚。

## Hc25MacDiscoveryClient: 协议解析优先纯化

对应代码：
- `app/src/main/java/com/wifi/optometry/communication/device/Hc25MacDiscoveryClient.java`
- `demo/src/main/java/com/example/wifidemo/device/Hc25MacDiscoveryClient.java`

判断：
- 重复发生在响应解析，而不是 UDP 发送、超时或重试流程。
- `app` 和 `demo` 都有同类实现，这类优化默认应同步评估两边，避免一个模块已经纯化解析、另一个模块仍然保留两段近似正则分支。

采取动作：
- 提炼 `findMacAddress(...)`。
- 同步把 `app` 和 `demo` 的 SEARCH 响应解析收敛到同样的纯 helper。
- 保持重试次数、超时、发送流程和返回语义不变。

没有做的事：
- 没有调整查询重试逻辑。
- 没有改变 UDP 收发时序。
- 没有只修改其中一个模块的解析骨架。

原因：
- 协议边界优先保护行为，先从纯解析函数下手风险最低。

## DeviceManager: 并发通信边界受保护

对应代码：
- `app/src/main/java/com/wifi/optometry/communication/device/DeviceManager.java`
- `demo/src/main/java/com/example/wifidemo/device/DeviceManager.java`

判断：
- 资源清理、断连通知、线程调度和 socket 生命周期构成受保护边界。
- 最安全的切入点是无副作用的解码 fallback 辅助逻辑。

采取动作：
- 提炼 `tryDecodeWithFallback(...)`。
- 同步修改 `app` 和 `demo` 的镜像实现。

没有做的事：
- 没有重排 `cleanup(...)`。
- 没有改变监听器通知顺序。
- 没有调整锁或线程模型。

原因：
- 并发通信类每次只改一个维度，否则验证成本和回归风险都会明显上升。

## DeviceHistoryStore: 事务骨架可抽，业务差异保留显式

对应代码：
- `app/src/main/java/com/wifi/optometry/communication/device/DeviceHistoryStore.java`
- `demo/src/main/java/com/example/wifidemo/device/DeviceHistoryStore.java`

判断：
- `recordConnectionAt(...)` 和 `recordCommunicationAt(...)` 重复的是事务内骨架，不是完整业务规则。
- 连接状态、计数增长和日志类别仍然是可读性很强的业务差异，不适合被一个高度参数化模板吞掉。
- 该类在 `app` 和 `demo` 中存在镜像实现，应同步调整。

采取动作：
- 提炼 `prepareTrackedDevice(...)`，收敛“取或建实体 + 更新摘要”的重复。
- 提炼 `insertDeviceLog(...)` 和 `toNullablePort(...)`，收敛日志插入与端口归一化的重复。
- 保留连接事件和通信事件各自的显式字段修改与 trace 语义。

没有做的事：
- 没有把两个写方法合并成通用执行器。
- 没有改变 `runBlocking(...)`、`runInTransaction(...)` 的边界。

原因：
- 事务类更适合抽稳定骨架，不适合把业务差异折叠到难读的回调配置里。

## TcpServerService: 前台 Service 属于受保护边界

对应代码：
- `app/src/main/java/com/wifi/optometry/communication/TcpServerService.java`
- `demo/src/main/java/com/example/wifidemo/TcpServerService.java`

判断：
- 该类同时承载前台通知、生命周期、socket 监听、锁资源、设备归档、监听器通知和 MAC 解析调度。
- 主流程时序敏感，属于高风险受保护边界。
- `app` 和 `demo` 中都存在镜像实现，后续如果真要调整监听分发、缓存回放或连接主流程，不能默认只改一边。

采取动作：
- 这一轮不做大规模结构调整。
- 仅把它作为 skill 的反例样本，沉淀“先动旁路辅助逻辑、不要重排主时序”的规则。

没有做的事：
- 没有拆分 `startServer(...)` / `stopServer(...)` 主流程。
- 没有改动 accept 循环、清理顺序、缓存回放或监听器通知顺序。
- 没有只修改其中一个模块的主流程分支。

原因：
- 当前收益主要来自判断边界，而不是硬拆结构。

## HeartbeatManager: 定时保活与清理顺序属于受保护边界

对应代码：
- `app/src/main/java/com/wifi/optometry/communication/HeartbeatManager.java`
- `demo/src/main/java/com/example/wifidemo/HeartbeatManager.java`

判断：
- 该类同时承载客户端心跳状态表、定时检查、最长保活时间、异步发送和销毁清理。
- 风险点不在“代码能不能再短”，而在 `scheduleWithFixedDelay(...)` 的触发节奏、`lastMessageTime` 更新时机、手动触发语义和线程池关闭顺序。
- `app` 和 `demo` 中是镜像实现，后续若真改调度策略，应默认同步评估两边。

采取动作：
- 这一轮不拆线程模型或调度骨架。
- 只把时间判断、日志文案或纯辅助判断视为可安全提炼的旁路 helper。
- 把它作为“定时保活类先保护时序和清理语义”的样本写回 skill。

没有做的事：
- 没有改 `HEARTBEAT_INTERVAL`、`MAX_KEEPALIVE_TIME` 或 `HEARTBEAT_MESSAGE`。
- 没有改 `scheduleWithFixedDelay(...)` 的触发频率、`lastMessageTime` 的更新时间点或 `destroy()` 的关闭顺序。
- 没有把它拆成额外 scheduler / strategy / worker 类。

原因：
- 对定时保活类来说，最重要的是保活间隔、发送时机和清理顺序的稳定性；当前收益主要来自先判断边界，不是先追求结构更漂亮。

## ExamWorkflowEngine: 显式状态机优先保留

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/ExamWorkflowEngine.java`

判断：
- `moveNext(...)`、`movePrevious(...)`、`shouldSkip(...)`、`resolveField(...)` 直接承载验光流程推进规则。
- 分支虽然多，但业务语义直观，当前更适合保留显式写法。

采取动作：
- 这一轮不把它改成规则表、策略映射或解释器结构。
- 把它作为“显式状态机比过早抽象更稳”的样本写回 skill。

没有做的事：
- 没有把字段解析改成 Map 配置。
- 没有把比较符号和跳步规则抽成通用规则引擎。

原因：
- 领域流程问题排查依赖直接可读的步骤语义，过早配置化会增加理解成本。

## CommandSettingsViewModel: 注册清单保留显式，重复接线骨架可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/command/CommandSettingsViewModel.java`
- `demo/src/main/java/com/example/wifidemo/sample/command/CommandSettingsViewModel.java`

判断：
- `registerProtocolUseCases(...)` 里的注册项本身就是页面职责清单，适合继续显式保留。
- 重复发生在“注册命令处理器”“注册 ACK 处理器”和“刷新编码表状态”这些稳定骨架上。
- 该类在 `app` 和 `demo` 中是镜像实现，应同步修改。

采取动作：
- 提炼 `registerCommandConsoleUseCase(...)` 和 `registerCommandAckConsoleUseCase(...)`，收敛重复的接线 lambda 外壳。
- 提炼 `updateCommandTableState(...)`，统一 `renderLoadResult(...)` 和 `renderSnapshot(...)` 的状态刷新流程。
- 保留各编码项、各回执项的显式注册列表。

没有做的事：
- 没有把注册项改成外部配置表。
- 没有把页面专属文案下沉到通用注册框架。

原因：
- 对接线型页面来说，可直接阅读“注册了哪些项”比减少几行重复更重要；最稳的做法是只抽骨架，不抽语义清单。

## CommandTableLoader: 纯解析字典优先静态收敛

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandTableLoader.java`

判断：
- 风险最低的重复点不是 CSV 读取主流程，而是 `aliasHeader(...)` 中反复构造同一份表头别名字典。
- 这类类属于纯解析基础设施，适合优先收敛稳定字典和规则映射。

采取动作：
- 提炼静态 `HEADER_ALIASES` 常量。
- 用 `createHeaderAliases()` 统一构造并冻结字典。

没有做的事：
- 没有重排 `load(...)` 的主解析流程。
- 没有改动表头缺失、字段回填和异常文案语义。

原因：
- 对纯解析器来说，先收敛数据字典比改主流程更稳，也更容易验证行为不变。

## CommandSettingsRepository: 编码表仓库先保护加载与替换顺序

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandSettingsRepository.java`

判断：
- 该仓库同时处理实例缓存、上次来源恢复、编码表加载、catalog 校验、`CommandEngine` 替换和最近来源持久化。
- 真正敏感的是“load -> validate -> replace -> 更新 snapshot / uri -> 持久化”的副作用顺序，以及按 `packageName + profileId` 区分实例的边界。
- `loadFromUri(...)` 和 `loadBuiltInSample()` 虽然看起来有重复，但两者在来源语义、持久化行为和错误处理上并不完全相同，不适合为了统一强行折叠。

采取动作：
- 这一轮不直接改它的主流程。
- 把它作为“命令表配置仓库先保护加载与替换顺序”的样本写回 skill。

没有做的事：
- 没有把 `loadFromUri(...)` 和 `loadBuiltInSample()` 合并成高度参数化的统一执行器。
- 没有改动 `SharedPreferences` key、实例缓存 key 或最近来源恢复逻辑。
- 没有改动 `CommandEngine.replaceCommandTable(...)` 的调用时机。

原因：
- 对配置仓库来说，更重要的是保护来源恢复、校验和运行时替换顺序，而不是消除表面上的赋值重复。

## CommunicationDemoViewModel: 叙事脚本保留显式，公共收尾可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/communication/ui/CommunicationDemoViewModel.java`

判断：
- `showActionControl(...)`、`showFieldSetting(...)`、`runTransferExample(...)` 这些方法本质上是在讲解通信场景，不适合为了统一而改成外部脚本配置。
- 重复主要集中在 `CommunicationDemoUiState` 落盘和失败收尾，而不是示例脚本本身。

采取动作：
- 提炼 `updateScenarioState(...)`，统一成功状态落盘。
- 提炼 `renderFailureScenario(...)`，统一失败日志、错误展示和 `dispatchMessage(...)` 收尾。
- 保留每个示例方法里的场景文案、步骤和示例数据。

没有做的事：
- 没有把所有示例改成配置表驱动。
- 没有把每个场景的业务讲解挪到通用模板引擎。

原因：
- demo 类的价值在于显式讲解；最稳的做法是只抽公共收尾，不抽场景叙事。

## ProtocolDispatcher: 分发核心先保护语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/dispatcher/ProtocolDispatcher.java`
- `lib/src/main/java/com/wifi/lib/command/dispatcher/ProtocolDispatchResult.java`

判断：
- 该类直接承载 command / ack / transfer / stream 的分发顺序、fallback 策略、session 生命周期和 auto-remove 语义。
- `ProtocolDispatchResult` 通过 `handled(...) / unhandled(...) / failed(...)` 三个静态工厂，把分发结果、routeKey、detail 和 auto-remove 标记集中表达出来，本身就是分发契约的一部分。
- 看起来有局部重复，但属于底层受保护核心，回归验证面大。

采取动作：
- 这一轮不直接改它的分发主流程。
- 仅把它们作为“不要为了统一模板而模糊分发语义与结果语义”的反例样本写进 skill。

没有做的事：
- 没有合并 `dispatchTransfer(...)` 和 `dispatchStream(...)`。
- 没有修改 route key、fallback、异常返回或 auto-remove 规则。
- 没有把 `ProtocolDispatchResult` 改成模糊的布尔返回值或通用 `Map` 载荷。

原因：
- 当前更重要的是保护底层分发语义，而不是消除表面重复。

## ProtocolGateway: 协议门面保留显式分支，直达发送入口不必再套壳

对应代码：
- `lib/src/main/java/com/wifi/lib/command/gateway/ProtocolGateway.java`

判断：
- 该类的主要职责是恢复命令表、解析 inbound 消息类型，以及提供 command / ack / transfer / stream 的直达发送入口。
- `resolveInbound(...)` 里的 ACK / transfer / stream / command 分支本身就是协议清单，适合继续显式保留。
- `sendCommand(...)`、`sendAck(...)`、`sendTransferBytes(...)`、`finishStream(...)` 这些方法大多是薄转发，真正敏感的是命令表恢复顺序和各编码器语义，不是公开方法数量。

采取动作：
- 这一轮不直接改它的门面结构。
- 把它作为“协议门面先保护显式分支和发送语义”的样本写回 skill。

没有做的事：
- 没有把 inbound 解析分支改成注册表、策略映射或统一 handler 容器。
- 没有给 `sendXxx(...)` 再套一层 facade / adapter / use case。
- 没有改动 `ensureCommandTableLoaded()` 的恢复顺序和 fallback 语义。

原因：
- 对协议门面来说，一眼看见支持哪些消息类型、恢复链路怎样回退，比再抽一层更重要；当前没有足够收益支撑结构继续加深。

## CommandEngine: 命令解析引擎先保护匹配、分发与模板替换语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandEngine.java`

判断：
- 该类是收发两端共用的命令解析引擎，既负责发送前的编码查找、方向校验、启用态校验和模板替换，也负责接收后的匹配、处理器分发和日志输出。
- `prepareOutbound(...)`、`resolveInbound(...)`、`dispatchInbound(...)` 虽然都围绕“命令”展开，但它们分别保护发送检查、接收匹配和处理器调度，不适合为了统一硬折叠成单一 pipeline。
- 风险最低的切入点是纯日志文案或旁路 helper，而不是重排 guard clause、模板替换异常或 `ConcurrentHashMap` 处理器表的语义。

采取动作：
- 这一轮不直接修改它。
- 把它作为“命令解析引擎先保护匹配、分发与模板替换语义”的样本写回 skill。

没有做的事：
- 没有改动 `replaceCommandTable(...)` 的替换时机或 `prepareOutbound(...)` 的校验顺序。
- 没有改动 `dispatchInbound(...)` 中“先匹配 -> 再找 handler -> 再分发”的顺序。
- 没有把发送、接收和分发流程抽成更重的责任链、策略表或统一执行器。

原因：
- 对命令解析引擎来说，最重要的是编码检查、模板替换和分发路径一眼可查；当前显式结构比追求更“统一”的抽象更稳。

## ExamSeedData: 默认业务素材优先显式保留

对应代码：
- `app/src/main/java/com/wifi/optometry/data/ExamSeedData.java`

判断：
- 这个类主要承载默认病人、默认图表、默认流程和默认会话样例，内容本身就是业务素材。
- 长列表不是主要问题，可读性重点在于“默认内容到底是什么”，而不是“能不能再少几行”。

采取动作：
- 这一轮不把它改成配置表或工厂拼装结构。
- 仅把它作为“显式 seed data 优先保留”的样本写回 skill。

没有做的事：
- 没有把 `createPrograms()` 改成表驱动。
- 没有把默认会话字段拆成多层 builder 或通用模板。

原因：
- 对 seed data 来说，直接看见默认素材比抽象统一更重要。

## DLogZipDelegate: 导出链路属于资源边界

对应代码：
- `lib/src/main/java/com/wifi/lib/log/zip/DLogZipDelegate.java`

判断：
- 该类同时处理目录选择、日志过滤、压缩、分享、缓存删除和主线程回调。
- 导出与分享链路依赖资源时序，属于受保护边界。

采取动作：
- 这一轮不重排导出和分享主流程。
- 仅把它作为“日志导出辅助类先保护资源边界”的反例样本写回 skill。

没有做的事：
- 没有调整 `performCompression(...)` 的主链路。
- 没有改变分享、保存、回调和清理顺序。

原因：
- 当前更适合先保护资源边界，再考虑局部纯 helper 提炼。

## JZipDelegate: 导出代理先保护目录选择、分享与回调时序

对应代码：
- `lib/src/main/java/com/wifi/lib/log/zip/JZipDelegate.java`

判断：
- 该类同时处理目录选择器注册、压缩线程启动、`JLog` 初始化检查、分享与本地导出分流，以及主线程结果回调。
- `exportToLocalDirectory(...)` 和 `shareToSocialApp(...)` 虽然入口相近，但它们依赖 `ActivityResultLauncher`、`destinationUri` 和临时 zip 文件流转，属于受保护资源边界。
- 最安全的切入点是纯文案、`Intent` 构造或文件复制 helper，不是重排 `performCompression(...)` 主链路。

采取动作：
- 这一轮不直接修改它。
- 把它作为“导出代理先保护目录选择、分享与回调时序”的样本写回 skill。

没有做的事：
- 没有改动 `withDirectoryPicker(...)` 的注册方式或 `withoutDirectoryPicker(...)` 的降级语义。
- 没有重排 `JLog.saveLogsToFile()`、压缩、分享/保存和 `notifySuccess(...)` / `notifyError(...)` 的先后关系。
- 没有改动未选择目录、未注册目录选择器和日志未初始化时的错误返回语义。

原因：
- 对导出代理来说，最重要的是资源可用性、回调线程和分享/保存分支稳定；当前收益主要来自先识别边界，而不是继续打散结构。

## JLogcatCollector: 日志主时序保护，纯格式化可抽

对应代码：
- `lib/src/main/java/com/wifi/lib/log/logcat/JLogcatCollector.java`

判断：
- `record(...)`、spill、持久化恢复和 crash 监控构成受保护日志主时序。
- 风险最低的重复点在时间格式和命名格式，不在 buffer / 文件锁 / flush 条件本身。

采取动作：
- 提炼时间格式常量和 `formatNow(...)`。
- 保持 crash 文件命名、日志行时间戳、按日期目录和按时间文件名的现有语义不变。

没有做的事：
- 没有改动 spill 触发条件。
- 没有改动 `savePersistentLogs()`、`loadPersistentLogs()`、`replacePersistentFile(...)` 的时序。

原因：
- 对日志基础设施来说，先从无副作用格式化 helper 下手最稳。

## JLog / JLogConfig: 日志门面先保护启停语义，配置对象保持 builder 默认值

对应代码：
- `lib/src/main/java/com/wifi/lib/log/JLog.java`
- `lib/src/main/java/com/wifi/lib/log/JLogConfig.java`

判断：
- `JLog` 是全局日志门面，`init(...)`、`stop()` 和 `log(...)` 会同时影响 collector 启停、默认 tag、observer 分发和是否落盘。
- `JLogConfig` 虽然字段不少，但本质上仍是配置快照；`Builder` 里的默认目录、默认 tag、解压密码和存储限制本身就是对外语义的一部分。
- 最安全的切入点是补充只读派生或旁路 helper，而不是改 singleton、observer 分发顺序或 builder 默认值来源。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“日志门面保护启停语义，配置对象保护默认值”的样本写回 skill。

没有做的事：
- 没有改动 `init(...)` 中“必要时先 stop -> 替换配置 -> 更新默认 tag -> start(...)”的启停顺序。
- 没有改动 `log(...)` 中 Android Log 输出、可选 `record(...)` 和 observer `dispatch(...)` 的先后关系。
- 没有改动 `Builder` 里默认目录选择、默认 `unzipCode`、日志保留天数和文件大小限制。

原因：
- 对日志门面和配置对象来说，最重要的是初始化语义、默认值和分发顺序可预期；强行再抽象一层只会增加调用端和问题定位成本。

## JLogExporter: 导出入口门面先保护注册时机与五击触发语义

对应代码：
- `lib/src/main/java/com/wifi/lib/log/JLogExporter.java`

判断：
- 该类同时负责页面初始化时机校验、`JZipDelegate` 缓存、导出/分享入口分流，以及五击触发阈值控制。
- 敏感点不在“能不能再提几个 helper”，而在注册必须早于 `Lifecycle.State.STARTED`、`WeakHashMap` 缓存语义，以及五击触发窗口和次数本身。
- 最安全的切入点是回调适配层或轻量 trigger helper，而不是改生命周期判断和导出委托创建顺序。

采取动作：
- 这一轮不直接修改它。
- 把它作为“导出入口门面先保护注册时机与五击触发语义”的样本写回 skill。

没有做的事：
- 没有改动 `getOrCreateExportDelegate(...)` 中生命周期检查、缓存命中和创建 `JZipDelegate` 的顺序。
- 没有改动 `CONTINUOUS_CLICK_INTERVAL_MS`、`REQUIRED_CLICK_COUNT` 或五击后重置计数的语义。
- 没有把导出/分享入口改成统一配置对象或更重的 manager 链路。

原因：
- 对导出入口门面来说，最重要的是防误用、注册时机和触发手势可预期；当前结构已经足够直观，不值得为了形式继续折腾。

## DLog / DLogExporter: 日志包装门面与导出入口保持薄封装和触发语义

对应代码：
- `lib/src/main/java/com/wifi/lib/log/DLog.java`
- `lib/src/main/java/com/wifi/lib/log/DLogExporter.java`

判断：
- `DLog` 本质上是基于 `JLog` 的薄门面，核心价值在于统一 `TAG`、标准化 `[source] message` 格式，以及把导出入口收敛到更业务化的静态方法。
- `DLogExporter` 和 `JLogExporter` 一样，真正敏感的是注册时机、导出委托缓存和五击触发阈值，而不是“还能不能再抽几个 helper”。
- 这两类代码更适合保持薄包装和显式入口，不适合为了层次感再拆 facade、service 或策略对象。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“日志包装门面与导出入口保持薄封装和触发语义”的样本写回 skill。

没有做的事：
- 没有改动 `DLog` 对 `JLog` / `DLogExporter` 的转发关系。
- 没有改动 `buildMessage(...)` 的消息拼装语义。
- 没有改动 `DLogExporter` 的生命周期检查、五击触发或导出/分享分支。

原因：
- 对日志包装门面来说，最重要的是入口稳定、调用直接、行为可预期；当前结构已经足够轻，不需要为了形式继续分层。

## DLogNetworkInterceptor: 网络日志拦截器先保护预览边界与记录顺序

对应代码：
- `lib/src/main/java/com/wifi/lib/network/DLogNetworkInterceptor.java`

判断：
- 该拦截器同时负责请求日志、响应日志、失败日志、文本/二进制体预览和最大预览长度控制。
- 敏感点不在“还能不能再拆几个 helper”，而在 `intercept(...)` 的记录顺序、`peekBody(...)` 的使用方式，以及 `isPlainText(...)` 对不同内容类型的判定边界。
- `requestBodyPreview(...)`、`responseBodyPreview(...)`、`truncate(...)` 这些 helper 已经紧贴主流程，再往外拆收益有限。

采取动作：
- 这一轮不直接修改它。
- 把它作为“网络日志拦截器先保护预览边界和记录顺序”的样本写回 skill。

没有做的事：
- 没有改动请求/响应/失败日志的输出顺序。
- 没有改动 `MAX_PREVIEW_BYTES`、文本判定规则或二进制提示语义。
- 没有把请求预览、响应预览和错误分支拆到额外 logging pipeline。

原因：
- 对网络日志拦截器来说，最重要的是日志顺序、体预览边界和异常场景稳定；当前直接结构比再抽象一层更利于排查问题。

## NetworkConfig / NetworkCallExecutor: 网络配置值对象与执行骨架先保护默认值和回调顺序

对应代码：
- `lib/src/main/java/com/wifi/lib/network/NetworkConfig.java`
- `lib/src/main/java/com/wifi/lib/network/NetworkCallExecutor.java`
- `lib/src/main/java/com/wifi/lib/network/NetworkServiceFactory.java`

判断：
- `NetworkConfig` 本质上是 Retrofit / OkHttp 的配置快照，`normalizeBaseUrl(...)`、超时默认值、header/interceptor 收集和不可变封装都是对外语义的一部分。
- `NetworkCallExecutor` 的敏感点不在“还能不能再拆几个 helper”，而在 `enqueue(...) -> onResponse(...) / onFailure(...) -> parse(...) -> success / failure` 的回调顺序、错误文案和日志时机。
- `NetworkServiceFactory` 保护的是 `Retrofit -> OkHttpClient -> HeaderInterceptor` 这条装配顺序，以及静态 header、动态 header、可选 `DLogNetworkInterceptor` 和自定义 interceptor 的插入顺序。
- `handleParsedSuccess(...)`、`resolveHttpErrorMessage(...)`、`truncate(...)` 这些 helper 已经贴着主流程存在，再继续拆分的收益有限。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“网络配置保护 builder 默认值，执行骨架和装配工厂保护成功/失败/解析/拦截器顺序”的样本写回 skill。

没有做的事：
- 没有改动 `build()` 后的 baseUrl 归一化、header / interceptor 不可变封装或超时正数校验。
- 没有改动 `onResponse(...)` 中“HTTP 成功 -> 解析 -> 业务成功/失败”和“HTTP 失败 -> 读取错误体 -> 包装失败结果”的顺序。
- 没有改动 `onFailure(...)` 的取消请求判定、默认错误码 `-1` 或默认错误文案语义。
- 没有改动 `createOkHttpClient(...)` 中 header 拦截器、网络日志拦截器和额外 interceptor 的注册顺序。

原因：
- 对网络基础设施来说，最重要的是默认配置稳定、成功/失败回调语义直接；当前结构已经比再套一层 pipeline 更利于排查接口问题。

## GsonEnvelopeParser: JSON 包装解析器先保护字段映射与成功码语义

对应代码：
- `lib/src/main/java/com/wifi/lib/network/gson/GsonEnvelopeParser.java`

判断：
- 该类直接承载 `code / message / data` 包装响应的读取、成功码判断、空体处理和 Gson 反序列化。
- 真正敏感的不是“还能不能再拆几个 helper”，而是 `ResponseBody` 只能消费一次、根节点必须是 `JsonObject`、字段缺失时怎样回落到 `RESULT_INVALID_ENVELOPE`，以及 `successCode` 的默认语义。
- `readCode(...)`、`readMessage(...)`、`safeMessage(...)` 已经是贴着主流程的局部 helper，再往外抽收益有限。

采取动作：
- 这一轮不直接修改它。
- 把它作为“JSON 包装解析器先保护字段映射与成功码语义”的样本写回 skill。

没有做的事：
- 没有改动 `parse(...)` 中“读 body -> 判空 -> 判根节点 -> 判业务码 -> 反序列化 data”的顺序。
- 没有改动默认字段名 `code / message / data`、默认成功码 `0` 或错误码常量语义。
- 没有把解析流程拆成额外 envelope reader、message resolver 或 data mapper 链路。

原因：
- 对包装解析器来说，最重要的是字段映射、成功码和异常回退行为稳定；当前结构比继续拆层更利于排查接口响应问题。

## WorkbenchFragment: 页面骨架保留显式，稳定 key 与动作壳可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/WorkbenchFragment.java`

判断：
- 该类主要职责是组织工作台页面结构、功能行、图表区和交互入口，显式页面骨架本身有价值。
- `buildFunctionRows()` 里的重复主要来自稳定 key 和重复动作壳，不在页面结构本身。

采取动作：
- 提炼功能 key 常量。
- 提炼 `createAdjustFunctionListener(...)`、`createAdjustFunctionAction(...)`、`createFunctionEventAction(...)`。
- 保留功能行列表和页面区域的显式写法。

没有做的事：
- 没有把工作台结构改成配置表驱动。
- 没有拆散 `renderAll()` 和页面区域组织关系。

原因：
- 对容器页来说，显式页面结构比进一步抽象更重要；最稳的是只抽稳定 key 和动作壳。

## CommandCatalog / OptometryCommandCatalogs: 显式目录保留，纯默认说明可抽

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandCatalog.java`
- `lib/src/main/java/com/wifi/lib/command/profile/OptometryCommandCatalogs.java`
- `lib/src/main/java/com/wifi/lib/command/profile/OptometryCommandProfile.java`

判断：
- `OptometryCommandCatalogs` 里的预留编码清单本身就是协议目录素材，应继续显式保留。
- `CommandCatalog.Builder` 中默认说明字符串拼装属于纯骨架，可安全提炼。
- `OptometryCommandProfile` 这种简单 profile 壳类保持直接即可，不值得再套额外抽象。

采取动作：
- 在 `CommandCatalog.Builder` 提炼 `buildDefaultCodeExplanation(...)`。
- 保留 `OptometryCommandCatalogs` 的显式 builder 清单和 `OptometryCommandProfile` 的简单返回结构。

没有做的事：
- 没有把预留编码清单改成外部 DSL 或配置层。
- 没有为了统一 profile 结构引入额外包装。

原因：
- 对命令目录类来说，直接看见支持哪些编码比减少少量字符串拼装更重要。

## CommandDefinition: 协议定义值对象保留字段与匹配语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandDefinition.java`

判断：
- 该类同时承载命令码、模块名、发送命令、接收命令、匹配模式、说明文案和顺序信息，本质上是“协议定义 + 轻量匹配行为”的值对象。
- 构造函数里的 `normalize(...)`、`buildReceivePattern(...)` 和 `compiledReceivePattern` 预编译，构成了对象一致性边界。
- `matchIncoming(...)` 直接表达 EXACT / PREFIX / CONTAINS / REGEX 的语义差异，本身比外提到额外 matcher 更清楚。

采取动作：
- 这一轮不直接修改它。
- 把它作为“轻量协议定义值对象不必拆成 builder + matcher + dto”的样本写回 skill。

没有做的事：
- 没有把 `matchIncoming(...)` 拆成多套策略类。
- 没有把 `normalize(...)`、`buildReceivePattern(...)` 或显示名称拼装挪到外部 helper。
- 没有为了“更规范”引入 builder、mapper 或额外包装对象。

原因：
- 对协议定义值对象来说，最重要的是一眼看见字段含义与匹配语义的对应关系；当前直接结构比再加层更稳。

## CommandCode: 协议编码值对象保留归一化、分段与比较语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandCode.java`

判断：
- 该类直接承载编码归一化、方向前缀校验、模块/子模块/动作分段，以及比较顺序语义。
- `of(...)`、`isValid(...)`、`getModuleCode()`、`toSegmentDisplay()` 这些方法虽然都不长，但它们共同定义了“一个合法命令码长什么样”，不适合再拆成 validator / formatter / parser 三件套。
- 风险最低的切入点是补充说明性 helper，而不是改 `normalize(...)`、`compareTo(...)` 或前缀解析规则。

采取动作：
- 这一轮不直接修改它。
- 把它作为“协议编码值对象保留归一化、分段与比较语义”的样本写回 skill。

没有做的事：
- 没有把编码再拆成额外 segment 对象、枚举组合或多层 parser。
- 没有改动 `trim + lowerCase(Locale.ROOT)` 的归一化语义。
- 没有改动 `compareTo(...)` 中“先业务码、再方向前缀”的排序规则。

原因：
- 对协议编码值对象来说，最重要的是合法性、分段结果和排序规则直接可见；当前结构已经足够紧凑清楚。

## AckMessage / TransferMetadata: 协议载荷值对象保留 builder 与局部派生语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/ack/AckMessage.java`
- `lib/src/main/java/com/wifi/lib/command/transfer/TransferMetadata.java`

判断：
- 这两个类本质上都是协议载荷值对象，主要职责是承载稳定字段、做构造期校验，并提供少量局部派生语义。
- `AckMessage.Builder.build()`、`TransferMetadata.Builder.build()`、`TransferMetadata.fromFile(...)` 保护的是对象完整性和默认值语义，不是“还可以再拆几层”的信号。
- `isSuccess()`、`hasErrorCode()`、`getTotalChunks()` 这类小型派生 helper 直接贴着字段定义，通常比额外的 calculator / formatter 更容易理解。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“协议载荷值对象优先保留 builder 与局部派生 helper”的样本写回 skill。

没有做的事：
- 没有把 builder、校验、默认值和派生 helper 拆到多个 validator / factory / calculator。
- 没有把 `extras` 包装成额外上下文对象。
- 没有改动 ACK 校验条件、chunk 计算或 `fromFile(...)` 的默认行为。

原因：
- 对协议载荷值对象来说，最重要的是直接看见字段、默认值和校验条件；当前结构已经足够紧凑清楚。

## ProtocolInboundEvent / TransferChunk: 协议入站封装与分块值对象保留工厂入口和校验语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/gateway/ProtocolInboundEvent.java`
- `lib/src/main/java/com/wifi/lib/command/transfer/TransferChunk.java`

判断：
- `ProtocolInboundEvent` 通过一组静态工厂明确区分 `COMMAND / ACK / TRANSFER / STREAM / UNKNOWN / INVALID`，这些工厂本身就是入站语义清单。
- `TransferChunk` 保护的是 `sessionId / index / totalChunks / offset / payload / crc32` 的构造期校验和 CRC 默认值语义，不适合为了“更面向对象”再拆 builder 或 validator。
- 这两类协议对象最重要的是语义直接和不变量稳定，不在于继续切分成更多小类。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“协议入站封装与分块值对象保留工厂入口和校验语义”的样本写回 skill。

没有做的事：
- 没有把 `ProtocolInboundEvent` 的静态工厂改成注册表或多层工厂链。
- 没有改动 `TransferChunk` 的 CRC 默认值、索引范围和 offset 校验。
- 没有把 `payload` 复制、防御性返回或 `isStructured()` / `isInvalid()` 语义拆走。

原因：
- 对协议小对象来说，最重要的是工厂入口、校验条件和状态语义直接可见；当前直接结构比继续抽象更稳。

## StreamMetadata: 实时流元数据值对象保留 builder 默认值与校验语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/stream/StreamMetadata.java`

判断：
- 该类集中承载 `sessionId`、方向、流类型、采样率、帧大小、校验开关和扩展字段，本质上是实时流会话的元数据快照。
- `Builder` 里的默认 `frameSize=256`、`checksumEnabled=true`、`putExtra(...)` 的裁剪规则，以及 `build()` 的必填项校验，本身就是对象语义的一部分。
- 最安全的切入点是增加只读派生 helper，而不是改 builder 默认值、extras 不可变封装或必填校验。

采取动作：
- 这一轮不直接修改它。
- 把它作为“实时流元数据值对象保留 builder 默认值与校验语义”的样本写回 skill。

没有做的事：
- 没有把 metadata 再拆成 session / format / extras 多个对象。
- 没有改动默认 frameSize、默认 checksum 开关或 `createSessionId()` 的生成语义。
- 没有把 `extras` 改成可变暴露或额外上下文包装。

原因：
- 对实时流元数据来说，最重要的是默认值、必填项和扩展字段语义稳定；当前结构已经是比较合适的值对象形态。

## demo MainActivity: 传统 Demo 入口页保留线性流程，服务回调桥接小步收敛

对应代码：
- `demo/src/main/java/com/example/wifidemo/MainActivity.java`

判断：
- 这是演示应用的传统入口页，直接持有 View、Spinner、Service 绑定和日志面板，本身就承担“演示台”职责。
- 重复主要集中在服务回调里的 `mainHandler.post + trace/appendLog + refresh`，以及发送目标、历史目标的选中项解析。

采取动作：
- 提炼字段级 `serviceMessageListener`，把匿名监听器从 `setupMessageListener()` 中抽出。
- 提炼 `handleServiceMessageReceived(...)`、`handleServiceClientEvent(...)`、`handleServiceIdentityResolved(...)`、`handleServiceError(...)`、`handleServerStarted(...)`。
- 提炼 `resolveSelectedClientId()`、`resolveSelectedHistoryDeviceId()`，收敛选中项校验。
- 提炼 `EMPTY_CLIENT_COUNT_TEXT`，并在 `onDestroy()`、`onServiceDisconnected()` 中显式清理监听器。

没有做的事：
- 没有把页面拆成多个 manager / delegate。
- 没有改动服务绑定、解绑、UI 刷新主流程。
- 没有改变发送消息和打开历史页的页面级交互路径。

原因：
- 这个页面的价值就在于直观呈现演示入口；最稳的做法是只收敛回调桥接和目标解析，不打散入口骨架。

## SettingsFragment: 设置表单页的显式字段映射优先保留

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/SettingsFragment.java`

判断：
- `bindSettings(...)` 和 `readSettingsFromForm()` 虽然都很长，但它们本质上是在逐项表达“表单控件 <-> 配置字段”的对应关系。
- 当前类已经把局部重复收敛到 `bindViews()`、`readText(...)`、`readDouble(...)`、`formatNumber(...)`，继续抽象很容易把字段语义藏进反射或配置表。

采取动作：
- 这一轮不直接改它的字段映射主流程。
- 把它作为“设置表单页优先保留显式映射”的样本写回 skill。

没有做的事：
- 没有把字段绑定改成反射、配置表或通用表单引擎。
- 没有把 `bindSettings(...)`、`readSettingsFromForm()` 拆成多层映射器。

原因：
- 对设置页来说，最重要的是一眼看见哪个控件对应哪个业务字段；当前写法已经在显式性和局部去重之间取得了比较好的平衡。

## PermissionDelegate: 权限闸门委托优先抽共用骨架，不改申请时序

对应代码：
- `lib/src/main/java/com/wifi/lib/baseui/delegate/PermissionDelegate.java`

判断：
- 这是 `lib` 里的基础 UI 委托，主职责是组织权限提示、申请入口、结果回调和可选跳设置页。
- `Activity` 和 `Fragment` 版本的申请流程高度重复，但真正敏感的是申请时序和回调语义，不是入口载体本身。

采取动作：
- 提炼 `requestPermissionsInternal(...)`，收敛 `Activity` / `Fragment` 共用的申请骨架。
- 提炼 `collectDeniedPermissions(...)`，把拒绝权限收集收敛成纯 helper。
- 保留 `requestInternal(...)`、`gotoPermissionSettings()`、requestCode 和 callback 的现有语义。

没有做的事：
- 没有改变弹框展示时机。
- 没有改变拒绝后是否跳系统设置页的语义。
- 没有把权限流程拆成多层策略接口或额外 manager。

原因：
- 对权限委托来说，最重要的是保护“检查 -> 弹框 -> 申请 -> 回调 -> 跳设置页”的时序；当前最稳的收益来自抽共用骨架，而不是重写权限模型。

## LogSettingsActivity / LogSettingsViewModel: 显式导出入口保留，回调桥接与状态发布可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/log/ui/LogSettingsActivity.java`
- `demo/src/main/java/com/example/wifidemo/sample/log/ui/LogSettingsViewModel.java`

判断：
- `LogSettingsActivity` 直接承担“应用配置、恢复默认、本地导出、平台分享”四类入口，显式功能清单本身就是页面价值的一部分。
- 真正稳定的重复点在导出成功/失败回调桥接，以及 ViewModel 里“发布表单 + 发布摘要”的状态更新骨架，不在页面入口本身。

采取动作：
- 在 Activity 中提炼统一的 `createExportResultCallback(...)`。
- 在 ViewModel 中提炼 `publishAppliedForm(...)`，统一表单和摘要的发布动作。
- 保留 4 个导出入口的显式 hook 清单和现有按钮接线。

没有做的事：
- 没有把导出入口折叠成动态配置表。
- 没有改动导出 hook 的注册时机。
- 没有改动页面表单字段、导出 API 或消息文案语义。

原因：
- 对日志设置页来说，最重要的是一眼看见页面支持哪些动作；最稳的做法是只抽回调桥接和状态发布 helper。

## DLogSettingsForm: 日志表单快照保留字符串字段与 fromConfig 语义

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/log/model/DLogSettingsForm.java`

判断：
- 该类不是底层日志配置本身，而是“给页面表单直接使用的快照”，所以字符串字段和 `overlayVisible` 这类 UI 语义本身就是边界的一部分。
- `fromConfig(...)` 里的格式化、布尔开关映射和 `formatDecimal(...)` 的字符串化规则，都是页面回显和输入框初始值的一部分，不适合为了“更纯”改回数字字段。
- 这类表单快照更适合保持直接、稳定、可回显，而不是再拆 mapper / formatter / input model。

采取动作：
- 这一轮不直接修改它。
- 把它作为“日志表单快照保留字符串字段与 fromConfig 语义”的样本写回 skill。

没有做的事：
- 没有把表单字段改回 `int / float` 再交给页面自己格式化。
- 没有把 `fromConfig(...)` 拆成额外 mapper / formatter。
- 没有改动 `formatDecimal(...)` 的去尾零语义。

原因：
- 对页面表单快照来说，最重要的是页面字段直达、回显稳定；当前结构已经很好地贴合了 UI 使用方式。

## DLogSettingsRepository: 配置仓库先保护落盘与运行时应用时序

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/log/data/DLogSettingsRepository.java`
- `demo/src/main/java/com/example/wifidemo/DemoApplication.java`

判断：
- 该仓库同时处理 SharedPreferences、`JLogConfig` 构建、参数校验、`DLog.init(...)` 和 `FlowDebugOverlay` 可见性应用，属于配置应用边界。
- 当前类已经把 `requireText(...)`、`parsePositiveInt(...)`、`parsePositiveFloat(...)`、`trimNumber(...)` 这类纯 helper 收敛好了，再往下大拆会直接碰到运行时副作用顺序。

采取动作：
- 这一轮不直接改它的主流程。
- 把它作为“配置仓库先保护落盘与应用时序”的样本写回 skill。

没有做的事：
- 没有拆散 SharedPreferences、Config Builder 和运行时应用流程。
- 没有改动默认值来源、持久化 key 和应用顺序。

原因：
- 对配置仓库来说，最重要的不是分层更漂亮，而是保护“读取 / 校验 / 持久化 / 应用”的副作用顺序。

## FlowDebugOverlayView: 调试浮层保留交互骨架，拖拽与日志订阅先别打散

对应代码：
- `lib/src/main/java/com/wifi/lib/flowdebug/FlowDebugOverlayView.java`

判断：
- 该自定义 View 同时承担日志订阅、展开/收起切换、初始定位、拖拽吸附和日志文本渲染。
- 真正敏感的是 `bind(...) / release()` 的监听器生命周期、`DragTouchListener` 的 touch slop 判定、`clampIntoParent(...)` 的位置约束和 callback 触发时机。
- `buildSummary(...)`、`formatEntry(...)` 虽然是纯格式化 helper，但它们当前已经足够贴近使用点，再往外拆收益不高。

采取动作：
- 这一轮不直接修改它。
- 把它作为“自定义调试浮层先保护交互骨架和订阅时序”的样本写回 skill。

没有做的事：
- 没有把拖拽逻辑拆到额外 controller / gesture manager。
- 没有把日志订阅和位置回调移到外部协调器。
- 没有改动收起/展开、拖拽吸附和 callback 通知顺序。

原因：
- 对调试浮层来说，最重要的是直接看懂交互骨架和日志刷新路径；过早拆层只会增加触摸与回调问题的排查成本。

## FlowDebugOverlay / FlowLogCenter: 浮层宿主与日志中心先保护挂载和订阅时序

对应代码：
- `lib/src/main/java/com/wifi/lib/flowdebug/FlowDebugOverlay.java`
- `lib/src/main/java/com/wifi/lib/flowdebug/FlowLogCenter.java`

判断：
- `FlowDebugOverlay` 负责 Application 生命周期安装、Activity 恢复时挂载/卸载浮层、显示状态保存和位置记忆。
- `FlowLogCenter` 负责 DLog 观察、缓冲区裁剪、主线程分发和监听器即时快照回放。
- 真正敏感的是 Activity 生命周期回调、`attachTo(...) / detachOverlay()` 的时机，以及日志订阅后何时在主线程通知 UI；这些都属于调试基础设施边界。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“调试基础设施先保护挂载/订阅时序”的样本写回 skill。

没有做的事：
- 没有把生命周期安装逻辑拆到额外 manager。
- 没有改动 `addListener(...)` 的即时回放语义、主线程通知方式或 buffer 裁剪顺序。
- 没有改动浮层 attach / detach、位置记忆和显示状态同步时机。

原因：
- 对调试基础设施来说，最重要的是挂载时机、日志订阅和 UI 更新路径稳定；一旦拆散，排查悬浮层或日志丢失问题会更难。

## NetworkDemoViewModel: 轻量异步场景入口保留显式，状态发布骨架可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/network/ui/NetworkDemoViewModel.java`

判断：
- `runGetExample()`、`runPostJsonExample()`、`runPostFormExample()`、`runUploadExample()` 本身就是网络示例页的功能清单，适合继续显式保留。
- 重复主要发生在“切换到运行中状态”和“发布执行结果状态”，而不是场景入口本身。

采取动作：
- 提炼 `renderRunningScenario(...)`、`publishResultState(...)`、`resolveCurrentState()`。
- 保留 `executeScenario(...)` 的异步编排骨架，以及 4 个显式场景入口方法。

没有做的事：
- 没有把场景入口改成枚举或配置表驱动。
- 没有改动 repository 回调顺序、loading 语义和消息文案。

原因：
- 对这类轻量 demo ViewModel 来说，最重要的是让人一眼看见支持哪些请求示例；最稳的收益来自状态发布 helper 去重，而不是引入更重的场景抽象。

## NetworkDemoRepository: 演示请求清单保留显式，响应预览 helper 可收敛

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/network/data/NetworkDemoRepository.java`

判断：
- `requestGetExample()`、`requestPostJsonExample()`、`requestPostFormExample()`、`requestUploadExample()` 本身就是网络示例仓库的教学清单，适合继续显式保留。
- 重复主要发生在 `dispatchCall(...)`、`buildResult(...)`、`buildResponsePreview(...)`、`renderValue(...)` 这类共用预览和回包整理逻辑，而不是 4 个场景入口本身。
- `prepareDemoFile()` 虽然也是辅助逻辑，但它承载的是“上传示例素材怎么准备”，继续贴近上传场景更清楚。

采取动作：
- 优先把请求分发、响应预览、异常摘要视为稳定 helper 边界。
- 保留 4 个显式请求场景方法和上传示例素材准备入口。
- 把它作为“教学型 repository 先保留显式场景清单，再收敛 preview helper”的样本写回 skill。

没有做的事：
- 没有把 4 个请求场景改成枚举、配置表或通用 request factory。
- 没有把 `requestPreview` 文案拼装改成模板系统或额外 DSL。
- 没有改 `BASE_URL`、header 语义或上传缓存文件行为。

原因：
- 对教学型网络仓库来说，最重要的是一眼看懂有哪些请求样例、每个样例发什么；最稳的重构应停留在 preview/helper 层，而不是把场景入口抽空。

## BrvahDemoViewModel: BRVAH 场景清单保留显式，分页状态骨架留在本类更清楚

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahDemoViewModel.java`

判断：
- `resetDevices()`、`clearDevices()`、`rotateCharts()`、`appendWorkflowAlert()`、`insertFollowUpProgram()` 这些方法本身就是 BRVAH 示例页要演示的动作清单，适合继续显式保留。
- `resetReportPaging()`、`loadNextReportPage()`、`retryLoadMoreReports()` 共同构成了一个带“首轮故障注入”的分页状态机，价值在于直接看见演示逻辑，而不是抽成通用 load-more 引擎。
- 当前类已经把 `updateReportState(...)` 这类稳定状态发布 helper 收敛在本类内，再往外拆容易让“这个 demo 到底想演示什么”变得不直观。

采取动作：
- 这一轮不直接修改它。
- 把它作为“BRVAH demo ViewModel 优先保留显式场景入口与分页状态骨架”的样本写回 skill。

没有做的事：
- 没有把所有 demo 动作统一成配置表、命令总线或场景注册器。
- 没有把分页错误注入、延时加载和 footer 状态切换拆到额外 coordinator。
- 没有把多个列表示例抽成通用 adapter demo engine。

原因：
- 对教学型 BRVAH ViewModel 来说，最重要的是一眼看见每个列表场景和分页状态是怎么演示的；直接保留显式动作清单通常更稳。

## BrvahDemoRepository: 演示素材仓库保留显式样例清单，分页切片 helper 保持直接

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/brvah/data/BrvahDemoRepository.java`

判断：
- `createKnownDevices()`、`createVisionCharts()`、`createWorkflowItems()`、`createExamPrograms()`、`createAllReports()` 本质上都在陈列 BRVAH 示例素材，本身就是教学内容。
- `getReportPage(...)` 和 `formatSeenTime(...)` 属于贴近素材使用点的小型 helper，当前直接写在仓库里更容易看出“分页规则是什么、时间展示怎么做”。
- 如果把这类样例素材抽成配置表、随机生成器或通用 factory，反而会让“示例到底长什么样”变得不直观。

采取动作：
- 这一轮不直接修改它。
- 把它作为“教学型素材仓库优先保留显式样例清单”的样本写回 skill。

没有做的事：
- 没有把样例数据改成外部 JSON、配置表或随机生成器。
- 没有把分页切片规则拆到额外 pager / datasource。
- 没有把 `createStep(...)`、`createReport(...)` 再抽成多层装配器。

原因：
- 对 demo 素材仓库来说，最重要的是一眼看见每类样例数据长什么样；显式素材清单通常比进一步模板化更稳。

## DemoFeatureFragment: 演示动作面板保留显式入口，局部交互 helper 可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/ui/DemoFeatureFragment.java`

判断：
- 这个 Fragment 的核心价值就是直观展示“刷新记录、追加记录、保存备注、确认弹框、BottomSheet、权限申请”这些演示入口。
- 真正稳定的重复点不在按钮入口，而在备注读取、记录项 View 构造、空态展示和权限回调桥接。

采取动作：
- 提炼 `handleSaveNote()`、`readNoteInput()`。
- 提炼 `renderEmptyRecordsHint()`、`createRecordView(...)`、`createRecordLayoutParams()`。
- 提炼 `canUseNotificationWithoutRuntimeRequest()`、`hasNotificationPermission()`、`requestNotificationPermissionWithDelegate()`、`createNotificationPermissionCallback()`。
- 保留按钮接线、确认弹框和 BottomSheet 打开入口的显式写法。

没有做的事：
- 没有把动作入口改成菜单配置表。
- 没有把弹框、BottomSheet、权限和列表渲染拆到额外 controller。
- 没有改动权限申请语义和页面交互顺序。

原因：
- 对演示功能面板来说，最重要的是让人直接看到“这个页面能演示什么”；最稳的做法是只抽局部交互 helper，不打散动作入口清单。

## DemoHomeActivity: 目录入口页保留显式导航清单，统一跳转 helper 可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/ui/DemoHomeActivity.java`

判断：
- 这个页面的主要职责就是列出 demo 模块入口，并提供一个说明弹框。
- 真正稳定的重复点在页面 chrome 初始化和 `startActivity(new Intent(...))` 这种跳转外壳，不在导航清单本身。

采取动作：
- 提炼 `initPageChrome()`。
- 提炼 `bindDemoEntries()` 和 `openDemoPage(...)`。
- 保留按钮到目标 Activity 的显式映射。

没有做的事：
- 没有把 demo 入口改成配置表或反射注册。
- 没有改动页面入口顺序和说明弹框语义。

原因：
- 对目录入口页来说，最重要的是一眼看见有哪些 demo；最稳的收益来自统一跳转 helper，而不是抽掉显式导航清单。

## DemoMvvmActivity: Fragment 容器页保留页面级编排，挂载 helper 可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/ui/DemoMvvmActivity.java`

判断：
- 这个 Activity 的核心职责是作为 MVVM 演示容器，承担标题栏、页面级按钮、顶部状态区和 `DemoFeatureFragment` 的挂载。
- 重复点主要在页面 chrome 初始化、Fragment 首次挂载判断和顶部状态文本渲染，不在容器骨架本身。

采取动作：
- 提炼 `initPageChrome()`、`bindActivityActions()`、`attachFeatureFragmentIfNeeded(...)`。
- 提炼 `renderSummary(...)` 和 `renderPermissionState(...)`。
- 保留 `savedInstanceState` 判定和容器页编排骨架。

没有做的事：
- 没有把 Fragment 挂载拆到额外 host helper。
- 没有改动页面级按钮行为和状态观察路径。

原因：
- 对容器页来说，最重要的是保留“页面动作 + 子 Fragment 挂载 + 顶部状态镜像”的编排可读性；最稳的做法是只抽局部骨架 helper。

## CommunicationDemoActivity: 按钮式场景集合页保留显式场景清单，渲染 helper 可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/communication/ui/CommunicationDemoActivity.java`

判断：
- 这个页面的价值就在于直接展示“动作控制、字段设置、查询响应、状态上报、结果上报、传输、流式、ACK、分发”等场景入口。
- 重复主要发生在页面 chrome、按钮接线骨架以及状态文本和状态颜色渲染，不在场景按钮清单本身。

采取动作：
- 提炼 `initPageChrome()`、`bindScenarioActions()`。
- 提炼 `renderStateTexts(...)` 和 `renderStatusColor(...)`。
- 保留按钮到 ViewModel 场景方法的显式映射。

没有做的事：
- 没有把场景入口改成配置表或循环注册。
- 没有改动状态字段含义、说明弹框和场景顺序。

原因：
- 对按钮式场景集合页来说，最重要的是一眼看见支持哪些演示场景；最稳的收益来自收敛页面壳 helper，而不是隐藏场景清单。

## BrvahDemoActivity: Tab 场景集合页保留 pager 骨架，挂载 helper 可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahDemoActivity.java`
- `demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahDemoPagerAdapter.java`

判断：
- 这个页面本身就是一个 tab 场景集，`ViewPager + TabLayoutMediator + PagerAdapter` 的骨架直接表达了页面结构。
- 风险最低的重复点在页面 chrome、pager 初始化和 tab 标题挂载，不在 tab 标题清单和 fragment 映射本身。

采取动作：
- 提炼 `initPageChrome()`、`initPagerTabs()`、`attachTabTitles()`。
- 保留 `BrvahDemoPagerAdapter` 里的显式标题清单和 fragment 分发。

没有做的事：
- 没有把 tab 标题和 fragment 映射改成动态配置。
- 没有改动 tab 顺序、adapter 结构和说明弹框语义。

原因：
- 对 tab 场景页来说，最重要的是直接看见分页结构和标题顺序；最稳的做法是只抽 pager 挂载壳 helper，不打散分页语义。

## BrvahDragSwipeFragment: 拖拽侧滑示例保留显式监听器骨架，状态提示与持久化收尾可抽

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahDragSwipeFragment.java`

判断：
- 这个 Fragment 的价值就在于把拖拽、侧滑、列表更新和持久化反馈直接演示出来，`QuickDragAndSwipe` 相关监听器本身就是场景说明的一部分。
- 真正稳定的重复点在状态提示、`persistPrograms(...)` 收尾和 `renderPrograms(...)` 的列表包装，不在各个监听器入口本身。
- 如果为了统一把拖拽监听、侧滑监听和点击监听都藏进一个大配置对象，反而会削弱示例的教学价值。

采取动作：
- 这一轮不直接修改它。
- 把它作为“拖拽侧滑示例保留显式监听器骨架，状态提示与持久化收尾可抽”的样本写回 skill。

没有做的事：
- 没有把拖拽、侧滑和点击监听器折叠成统一 action 配置。
- 没有改动长按拖动、左滑删除和删除后持久化的语义。
- 没有改动 `QuickDragAndSwipe` 的 flags、attach 时机和示例说明文案。

原因：
- 对教学型拖拽示例来说，最重要的是一眼看见每种交互在什么时候触发、如何收尾；显式监听器比内部极致去重更有价值。

## DeviceHistoryActivity: 历史详情页保留过滤语义，页面壳 helper 可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/device/DeviceHistoryActivity.java`
- `demo/src/main/java/com/example/wifidemo/DeviceHistoryActivity.java`

判断：
- 这类页面的核心职责是展示单个设备的历史摘要，并按过滤条件渲染日志正文。
- 重复主要发生在页面 chrome、过滤器监听、空态渲染和日志文本落盘，不在过滤规则和摘要字段本身。
- `app` 和 `demo` 中存在镜像实现，应同步调整。

采取动作：
- 提炼 `initPageChrome()`、`bindFilter()`。
- 提炼 `renderEmptyHistory()`、`renderSummary(...)`、`renderLogs(...)`。
- 保留 `renderHistory()` 的线性骨架，以及 `resolveFilter()`、`buildSummaryText(...)`、`buildLogText(...)` 的显式结构。

没有做的事：
- 没有改动过滤选项语义。
- 没有改动摘要字段顺序、日志格式和 `onResume()` 刷新时机。
- 没有把页面拆到额外 presenter / delegate。

原因：
- 对历史详情页来说，最重要的是直接看见“摘要怎么展示、过滤怎么切、日志怎么刷新”；最稳的收益来自页面壳 helper 去重，而不是隐藏业务展示结构。

## PatientFragment: 业务台账页保留动作与字段清单，卡片和保存壳可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/PatientFragment.java`

判断：
- 这个页面的核心价值是直接表达“搜索被测者、选中被测者、编辑档案、导入扫码串”这些动作，以及姓名/电话/性别/出生日期/地址/备注这些字段。
- 重复主要发生在当前选中摘要、患者卡片构造、动作按钮壳和保存壳，不在动作清单和字段顺序本身。

采取动作：
- 提炼 `renderSelectedPatientSummary()`、`renderEmptyPatientHint()`。
- 提炼 `createPatientCard(...)`、`createPatientActions(...)`、`createActionButtonParams()`。
- 提炼 `buildSelectedPatientSummary(...)`、`buildPatientBaseInfo(...)`、`buildPatientNoteInfo(...)`。
- 提炼 `saveEditedPatient(...)`，收敛对话框保存壳。

没有做的事：
- 没有把患者编辑表单改成配置表或通用表单引擎。
- 没有改动搜索逻辑、选中逻辑、导入入口和字段顺序。
- 没有把页面拆到额外 presenter / controller。

原因：
- 对业务台账页来说，最重要的是一眼看见“有哪些动作、编辑哪些字段、卡片怎么展示”；最稳的做法是只抽卡片和保存壳 helper，不打散业务字段清单。

## ProgramFragment: 程序页保留显式骨架，卡片与摘要 helper 可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/ProgramFragment.java`

判断：
- 这个页面的核心价值是直接表达“当前程序是什么、有哪些程序可选、当前程序有哪些步骤、可以新增什么自定义步骤”。
- 重复主要发生在程序卡片构造、步骤卡片构造、当前摘要拼装和自定义步骤保存壳，不在渲染骨架、步骤顺序和新增入口语义本身。

采取动作：
- 保留 `renderHeader()`、`renderPrograms()`、`renderSteps()` 的显式页面骨架。
- 提炼 `createProgramCard(...)`、`createProgramSelectButton(...)`、`createStepCard(...)`。
- 提炼 `buildCurrentProgramSummary(...)`、`buildCurrentStepSummary(...)`、`buildStepTargetSummary(...)`、`buildStepOptionSummary(...)`。
- 提炼 `createCustomStepInput()`、`saveCustomStep(...)`、`readInputValue(...)`，收敛自定义步骤弹窗保存壳。

没有做的事：
- 没有把程序卡片和步骤卡片改成通用配置表或统一 renderer。
- 没有改动 `selectProgram(...)`、`appendCustomProgramStep(...)` 的行为语义。
- 没有把页面拆到额外 presenter / controller。

原因：
- 对程序页来说，最重要的是一眼看见当前程序、步骤顺序和新增入口；最稳的做法是只收敛卡片和摘要 helper，不打散业务展示骨架。

## ReportFragment: 报告页保留显式汇总骨架，摘要与卡片 helper 可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/ReportFragment.java`

判断：
- 这个页面的核心价值是直接表达“当前报告摘要是什么、有哪些视功能指标、最近保存过哪些报告、二维码内容如何查看”。
- 重复主要发生在当前报告摘要拼装、指标卡片构造、历史报告卡片构造、空态提示和按钮动作壳，不在报告字段结构、指标分组和历史展示语义本身。

采取动作：
- 保留 `renderCurrentReport()`、`renderMetrics()`、`renderHistory()` 的显式页面骨架。
- 提炼 `saveCurrentReport()`、`importLatestReport()`、`showPrintPreviewHint()`、`renderQrPayload(...)`。
- 提炼 `buildCurrentReportHeader(...)`、`buildVisionSummary(...)`、`buildPrescriptionSummary(...)`、`resolvePatientName(...)`。
- 提炼 `createMetricCard(...)`、`buildMetricTitle(...)`、`buildMetricSummary(...)`、`renderEmptyMetricsHint()`。
- 提炼 `createHistoryCard(...)`、`buildHistoryTitle(...)`、`buildHistorySummary(...)`、`renderEmptyHistoryHint()`、`resolveVisibleHistoryCount()`。

没有做的事：
- 没有把报告摘要、指标卡片和历史列表改成统一 renderer、adapter 配置表或报表 DSL。
- 没有改动保存报告、导入最近报告、二维码内容和历史展示上限的现有语义。
- 没有把页面拆到额外 presenter / controller。

原因：
- 对报告页来说，最重要的是一眼看见报告摘要、指标结构和历史卡片内容；最稳的做法是只收敛摘要和卡片 helper，不打散汇总页骨架。

## DeviceFragment: 设备连接页保留显式操作骨架，卡片与消息壳可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/DeviceFragment.java`

判断：
- 这个页面的核心价值是直接表达“服务是否在运行、当前待发送什么消息、有哪些在线模块、有哪些已建档设备、最近日志是什么”。
- 重复主要发生在待发送消息校验、服务状态摘要、在线模块卡片构造、已知设备卡片构造、按钮参数和日志拼装，不在页面操作入口、设备摘要字段和历史页跳转语义本身。

采取动作：
- 保留 `renderState()`、`renderConnectedDevices()`、`renderKnownDevices()`、`renderLogs()` 的显式页面骨架。
- 提炼 `toggleServer()`、`broadcastPendingMessage()`、`sendPendingMessageToSelected()`、`sendPendingMessageToClient(...)`、`requirePendingMessage()`。
- 提炼 `renderServerSummary(...)`、`syncPendingMessageField(...)`、`resolveToggleServerText(...)`。
- 提炼 `createConnectedDeviceCard(...)`、`buildConnectedDeviceSummary(...)`、`createConnectedDeviceActions(...)`、`resolveConnectedDeviceHistoryId(...)`。
- 提炼 `createKnownDeviceCard(...)`、`buildKnownDeviceSummary(...)`、`createInlineActionButtonParams()`、`createTopActionButtonParams()`、`buildLogOutput(...)`。

没有做的事：
- 没有把在线模块卡片、设备台账卡片和操作按钮改成通用面板引擎或配置表。
- 没有改动启动/停止监听、广播、定向发送、选中模块和打开历史页的现有语义。
- 没有把页面拆到额外 presenter / controller。

原因：
- 对设备连接页来说，最重要的是一眼看见当前状态、设备卡片和可操作入口；最稳的做法是只收敛消息壳和卡片 helper，不打散运维页面骨架。

## DeviceServiceGateway: 薄网关接口保持直接，不要为了层次继续套壳

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/state/DeviceServiceGateway.java`

判断：
- 这个接口只暴露“查询服务状态、获取地址、获取在线模块、启动/停止、发送消息”这些稳定能力。
- 当前价值主要在于隔离 `ViewModel` 与具体服务实现，而不是承载业务逻辑。

采取动作：
- 这一轮不直接修改它。
- 把它作为“薄网关接口不必过度重构”的样本写回 skill。

没有做的事：
- 没有再套额外 facade / adapter。
- 没有为了“接口更完整”引入命令对象、builder 或多余包装。

原因：
- 对薄网关接口来说，最重要的是直接、稳定、易读；当前没有足够收益支撑继续抽象。

## BaseClinicFragment: 轻量页面基类先保持共享 helper 骨架

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/main/BaseClinicFragment.java`

判断：
- 这个基类主要承担共享 `ClinicViewModel`、`Toast`、`dp` 转换、卡片和文本 helper。
- 它本身已经足够轻，价值在于让多个页面直接复用相同 UI 骨架。

采取动作：
- 这一轮不直接修改它。
- 把它作为“基础页面基类先保持轻量直接”的样本写回 skill。

没有做的事：
- 没有把现有 helper 再拆到多个 delegate / 工厂类。
- 没有改动页面继承方式或基类 API。

原因：
- 对基础页面基类来说，最重要的是共享能力稳定、调用方式直接；当前没有明显职责混入，不值得继续拆。

## BaseVBActivity: 轻量 ViewBinding 基类先保持模板方法骨架

对应代码：
- `lib/src/main/java/com/wifi/lib/baseui/BaseVBActivity.java`

判断：
- 该基类只承担 ViewBinding inflate、页面初始化模板方法和少量 UI delegate 的懒加载。
- `onBindingCreated(...) -> initWidgets() -> bindListeners() -> observeUi() -> loadData()` 本身就是对子类最有价值的骨架，不适合为了“更灵活”再打散。
- `onRequestPermissionsResult(...)` 对 `PermissionDelegate` 的转发也是稳定边界，优先保持直接。

采取动作：
- 这一轮不直接修改它。
- 把它作为“基础 Activity 基类先保护模板方法骨架”的样本写回 skill。

没有做的事：
- 没有把 delegate 获取逻辑再拆成更多 manager / factory。
- 没有改动 `onCreate(...)`、`onBindingCreated(...)` 和权限结果转发顺序。
- 没有把空实现 hook 改成额外接口或多层生命周期分发器。

原因：
- 对基础 Activity 基类来说，最重要的是生命周期骨架稳定、子类接入点直接；过度抽象只会增加继承理解成本。

## BaseVBFragment / ViewBindingReflector: Fragment 模板骨架与反射 inflate 边界先保护

对应代码：
- `lib/src/main/java/com/wifi/lib/baseui/BaseVBFragment.java`
- `lib/src/main/java/com/wifi/lib/baseui/internal/ViewBindingReflector.java`

判断：
- `BaseVBFragment` 和 `BaseVBActivity` 一样，真正的价值是把 `initWidgets -> bindListeners -> observeUi -> loadData` 这条模板骨架稳定交给子类。
- `ViewBindingReflector` 虽然用了反射，但它保护的是“如何从泛型里解析出 ViewBinding 并正确调用 inflate”这条基础设施边界，不适合顺手改成更复杂的缓存或工厂体系。
- 风险最低的切入点是局部错误文案或只读辅助逻辑，不是改泛型解析、inflate 回退路径或 `onDestroyView()` 的清理边界。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“Fragment 模板骨架与反射 inflate 边界先保护”的样本写回 skill。

没有做的事：
- 没有改动 `onCreateView()`、`onBindingCreated(...)`、`onDestroyView()` 的生命周期顺序。
- 没有改动 `ViewBindingReflector` 的泛型解析、双 inflate 签名回退或异常抛出路径。
- 没有把模板 hook 再拆成额外接口、注解驱动或更多反射缓存层。

原因：
- 对基础 Fragment 基类和反射辅助器来说，最重要的是生命周期骨架和 inflate 语义稳定；当前结构比继续追求“更高级封装”更可靠。

## BaseConfirmDialog: 基础确认弹框保留显式按钮入口与文案设置

对应代码：
- `lib/src/main/java/com/wifi/lib/baseui/BaseConfirmDialog.java`

判断：
- 该类本质上是一个“标题、内容、确认、取消”四要素明确的基础弹框，价值在于调用时能直接设置各区域文案和行为。
- `setTitleTxt(...)`、`setContentTxt(...)`、`setCancelTxt(...)`、`setOkTxt(...)` 看起来重复，但它们正是调用端最清楚的 API，不适合为了统一改成大而全的 config 对象。
- 敏感点在按钮点击后的 `dismiss()` 时机、无标题模式切换和取消按钮隐藏语义，不在方法数量本身。

采取动作：
- 这一轮不直接修改它。
- 把它作为“基础确认弹框保留显式按钮入口与文案设置”的样本写回 skill。

没有做的事：
- 没有把按钮和文案入口折叠成统一 builder / config。
- 没有改动确认、取消点击时先 `dismiss()` 再回调的顺序。
- 没有改动无标题模式和隐藏取消按钮的现有语义。

原因：
- 对基础确认弹框来说，最重要的是 API 直观、行为稳定；当前显式入口比继续抽象更利于业务页面直接使用。

## PageTitleUIDelegate: 标题栏委托保留延迟挂载骨架，显式入口比统一配置更重要

对应代码：
- `lib/src/main/java/com/wifi/lib/baseui/delegate/PageTitleUIDelegate.java`

判断：
- 该委托同时负责标题栏惰性 inflate、状态栏占位高度调整、返回行为注入，以及左右文本/图标入口的初始化。
- `initTitle(...)`、`initIvBack(...)`、`initTvLeft(...)`、`initTvRight(...)` 看起来有重复，但这些方法本身就是调用侧最清楚的 UI 入口，强行改成统一配置对象会削弱可读性。
- 受保护边界在 `ensureView()`：它要求 `ViewGroup` 根节点、只挂载一次标题栏，并在挂载时同步处理状态栏占位高度。

采取动作：
- 这一轮不直接修改它。
- 把它作为“标题栏委托保留延迟挂载骨架，显式入口比统一配置更重要”的样本写回 skill。

没有做的事：
- 没有把所有 `init*` 方法折叠成通用 `applyAction(...)` / config 模式。
- 没有改动 `ensureView()` 的 `inflate -> 找控件 -> 设占位高度 -> addView(...)` 顺序。
- 没有把返回逻辑、左右按钮和标题文本拆到多个更细的 delegate。

原因：
- 对基础标题栏委托来说，最重要的是调用端能直接看出“标题、左按钮、右按钮”各自怎么初始化；当前显式 API 比内部绝对去重更有价值。

## ConnectedDeviceInfo / KnownDeviceSummary: 轻量摘要模型保持直接字段结构

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/ConnectedDeviceInfo.java`
- `app/src/main/java/com/wifi/optometry/domain/model/KnownDeviceSummary.java`

判断：
- 这两个类只是设备连接摘要和设备台账摘要的数据载体，主要承担字段表达和 getter 访问。
- 当前没有复杂构造、派生逻辑或校验规则，不需要为了“更规范”引入额外包装。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“简单数据载体不必过度重构”的样本写回 skill。

没有做的事：
- 没有引入 builder、mapper、copy 工具或多层 DTO 包装。
- 没有把字段拆成更多中间对象。

原因：
- 对轻量摘要模型来说，最重要的是字段清晰、调用直接；在没有复杂行为之前，保持简单通常更稳。

## TrackedDeviceEntity / DeviceLogEntity: Room 实体优先保持字段直达与空值归一化

对应代码：
- `lib/src/main/java/com/wifi/lib/db/TrackedDeviceEntity.java`
- `lib/src/main/java/com/wifi/lib/db/DeviceLogEntity.java`

判断：
- 这两个类直接对应设备台账和设备日志表结构，字段名、索引和空值默认语义本身就是数据库边界的一部分。
- 构造函数和 setter 里的工作主要是 `null -> ""` 归一化与简单赋值，当前没有复杂领域行为，不值得为了“更面向对象”再套 builder 或多层包装。
- 真正的业务判断已经在 `DeviceHistoryStore` 这类事务类里；实体类更适合保持 schema 到字段的直达映射。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“Room 实体优先保持字段直达与空值归一化”的样本写回 skill。

没有做的事：
- 没有把远端/本地地址拆成额外 endpoint 对象或更多嵌套结构。
- 没有引入 builder、DTO 壳、额外 mapper 或多层 entity wrapper。
- 没有把 `null` 归一化规则从构造函数和 setter 挪走。

原因：
- 对 Room 实体来说，最重要的是表结构、字段映射和默认值语义直观稳定；过度封装通常只会增加持久化链路的理解成本。

## ClinicSettings: 镜像配置模型保持直接字段与 copy 语义

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/ClinicSettings.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/ClinicSettings.java`

判断：
- 这两个类本质上都是诊所配置快照，字段虽然多，但它们直接对应设置页、持久化和运行时配置项。
- `copy()` 方法是直接、低风险的快照复制语义，当前没有复杂校验、派生规则或跨字段不变量需要额外封装。
- `app` 和 `demo` 中是镜像实现，后续如果新增字段或调整 `copy()` 规则，应默认同步评估两边。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“镜像配置模型不必为了层次继续套 builder / wrapper”的样本写回 skill。

没有做的事：
- 没有把字段拆成 cloud / display / step 等多个嵌套对象。
- 没有引入 builder、mapper、不可变包装或额外配置壳类。
- 没有只修改其中一个模块的字段或 `copy()` 语义。

原因：
- 对轻量配置模型来说，最重要的是字段直达、映射直接和镜像实现一致；当前结构已经足够支撑设置页与配置流转。

## FunctionalTestState: 镜像功能检查状态模型保持直接字段与 copy 语义

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/FunctionalTestState.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/FunctionalTestState.java`

判断：
- 这两个类集中承载 NPC / NPA / NRA / PRA / ACA / AMP、近灯状态和备注字段，本质上是一次功能检查的状态快照。
- `copy()` 方法是直接、低风险的复制语义；当前没有复杂派生逻辑、跨字段不变量或行为方法需要额外封装。
- `app` 和 `demo` 中是镜像实现，后续如果新增检查项、备注字段或复制规则，应默认同步评估两边。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“镜像功能检查状态模型保持直接字段与 copy 语义”的样本写回 skill。

没有做的事：
- 没有引入 builder、mapper、不可变包装或更细的子状态对象。
- 没有把各检查项拆成多个 holder / context。
- 没有只修改其中一个模块的字段或 `copy()` 语义。

原因：
- 对功能检查状态模型来说，最重要的是字段和检查项一眼可见、与页面和会话映射直接；当前结构已经足够轻，不值得为了形式继续拆层。

## ExamStep: 镜像流程步骤描述保持枚举与跳步字段集中

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/ExamStep.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/ExamStep.java`

判断：
- 这两个类本质上是验光流程步骤的描述对象，`DistanceMode`、`EyeScope`、`Comparator` 这些枚举和跳步字段共同表达了流程规则。
- 当前字段虽然多，但它们围绕的是“一个步骤如何展示、作用在哪只眼、何时跳过”这一个语义中心，过早拆成多个子对象反而不利于读流程。
- `app` 和 `demo` 中是镜像实现，后续若新增步骤属性、比较符或说明字段，应默认同步评估两边。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“镜像流程步骤描述保持枚举与跳步字段集中”的样本写回 skill。

没有做的事：
- 没有把步骤描述拆成 display / rule / skip 多个对象。
- 没有把枚举和跳步字段改成外部配置 DSL。
- 没有只修改其中一个模块的字段或枚举语义。

原因：
- 对流程步骤描述对象来说，最重要的是步骤字段和跳步规则能集中查看；当前结构比继续分层更利于排查流程问题。

## ExamSession: 镜像领域会话模型保持集中状态字段

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/ExamSession.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/ExamSession.java`

判断：
- 该类集中承载当前被测者、当前流程/步骤、选中视标、当前操作字段、眼别/距离模式，以及远用/近用/最终处方和视功能状态。
- 这些字段虽然多，但它们共同表达的是“当前验光会话的完整快照”，拆散后反而更难判断会话此刻处于什么状态。
- `app` 和 `demo` 中是镜像实现，后续若新增状态位、模式枚举或测量容器，应默认同步评估两边。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“领域会话模型优先保持集中状态字段”的样本写回 skill。

没有做的事：
- 没有把当前会话拆成多个子状态 holder、context wrapper 或额外 manager。
- 没有把现有枚举和测量容器拆到更细的装配层。
- 没有只修改其中一个模块的字段结构或默认状态。

原因：
- 对领域会话模型来说，最重要的是一眼看见当前会话包含哪些状态；保持集中字段通常比过早拆层更利于排查流程问题。

## LensMeasurement / PatientProfile: 镜像轻量领域模型保持 copy 与少量展示 helper

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/LensMeasurement.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/LensMeasurement.java`
- `app/src/main/java/com/wifi/optometry/domain/model/PatientProfile.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/PatientProfile.java`

判断：
- 这几类对象本质上都是轻量领域快照：`LensMeasurement` 表达镜片与棱镜数据，`PatientProfile` 表达被测者档案。
- `copy()` 和 `getDisplayName()` 这类方法属于紧贴字段的低风险辅助语义，当前并没有复杂不变量或行为逻辑，需要额外拆出 service / formatter / wrapper。
- `app` 和 `demo` 中是镜像实现，后续如果新增字段或默认展示语义，应默认同步评估两边。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“镜像轻量领域模型保持 copy 与少量展示 helper”的样本写回 skill。

没有做的事：
- 没有把镜片数据或被测者档案拆成更多嵌套对象。
- 没有引入 builder、mapper 或额外的展示包装类。
- 没有只修改其中一个模块的字段结构或默认展示语义。

原因：
- 对轻量领域模型来说，最重要的是字段直达、复制直接、少量展示 helper 贴着字段本身；当前结构已经足够清楚。

## CommandViewHelper / OptometryCommandCodes: 显式命令码清单保留，纯提示文案小步提炼

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandViewHelper.java`
- `lib/src/main/java/com/wifi/lib/command/profile/OptometryCommandCodes.java`

判断：
- `OptometryCommandCodes` 的编号常量本身就是稳定协议素材，适合继续显式保留。
- `CommandViewHelper` 的重复点只在长按提示文案拼装，属于纯文本辅助逻辑。

采取动作：
- 在 `CommandViewHelper` 中提炼 `buildCodeHintText(...)`。
- 保持 `OptometryCommandCodes` 常量列表和 `attachLongPressCodeHint(...)` 的对外用法不变。

没有做的事：
- 没有把命令码常量改成枚举、注册中心或动态配置。
- 没有把编码展示规则拆到多个 formatter / builder。

原因：
- 对命令码目录来说，可直接看到稳定编号比多一层抽象更重要；当前最稳的收益来自纯文本 helper 去重。

## TransferSender / TransferReceiver: 传输链路核心先保护分块与校验语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/transfer/TransferSender.java`
- `lib/src/main/java/com/wifi/lib/command/transfer/TransferReceiver.java`

判断：
- 这两个类直接承载了分块发送、进度通知、chunk 校验、offset 校验、MD5 校验、完成态判断和 payload 组装。
- `sendBytes(...)` / `sendFile(...)` 虽然有结构相似处，但它们分别对应内存载荷和文件流来源，强行合并容易模糊错误语义。
- `acceptChunk(...)` 的校验链和 `buildPayload()` 的组装/校验顺序属于受保护边界，不适合一边“顺手重构”一边改验证逻辑。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“传输链路核心先保护分块、校验和状态机语义”的样本写回 skill。

没有做的事：
- 没有把发送端和接收端抽成统一 transfer session engine。
- 没有改动 chunk 顺序、offset 规则、MD5 校验或完成态判断。
- 没有改动进度通知和异常抛出语义。

原因：
- 对传输链路核心来说，最重要的是分块规则、校验条件和状态推进一眼可查；过早统一模板通常会放大调试成本。

## StreamSender / StreamReceiver: 实时流核心先保护序号、状态与收发边界

对应代码：
- `lib/src/main/java/com/wifi/lib/command/stream/StreamSender.java`
- `lib/src/main/java/com/wifi/lib/command/stream/StreamReceiver.java`

判断：
- 这两个类直接承载实时流 `start -> send/accept -> finish/cancel/reset` 的状态推进、帧大小限制、序号递增和统计快照。
- `StreamSender.sendPayload(...)`、`finish(...)` 和 `StreamReceiver.acceptFrame(...)` 看起来都围绕 frame 收发，但它们分别保护发送端结束帧语义、接收端 CRC/sessionId/sequence 校验，以及 droppedFrames 统计，不适合为了统一抽成单一 session engine。
- 真正的敏感点在 `PREPARED / STREAMING / STOPPED / FAILED / CANCELED` 状态切换、结束帧处理和 sequence 不回退语义，不在能不能多拆几个 helper。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“实时流核心先保护序号、状态与收发边界”的样本写回 skill。

没有做的事：
- 没有把发送端和接收端抽成统一 stream session manager。
- 没有改动 `start(...)`、`finish(...)`、`cancel()`、`reset()` 的状态切换语义。
- 没有改动 sessionId 校验、CRC 校验、frameSize 限制、掉帧统计或结束帧处理规则。

原因：
- 对实时流核心来说，最重要的是状态推进、序号规则和结束条件能直接看懂；当前显式结构比继续统一模板更利于排查问题。

## CommandSettingsActivity: 入口流程保留显式，服务回调桥接可抽

对应代码：
- `app/src/main/java/com/wifi/optometry/ui/command/CommandSettingsActivity.java`
- `demo/src/main/java/com/example/wifidemo/sample/command/CommandSettingsActivity.java`

判断：
- 这类 Activity 直接承担文档选择、服务绑定、页面按钮接线和生命周期解绑，入口流程本身应保持显式。
- 重复主要发生在服务回调桥接、文档类型常量和发送目标选择逻辑，而不是入口流程本身。
- `app` 和 `demo` 中存在镜像实现，应同步调整。

采取动作：
- 提炼 `TABLE_DOCUMENT_MIME_TYPES`、`BROADCAST_TARGET_LABEL`。
- 提炼 `handleServiceMessageReceived(...)`、`handleServiceClientEvent(...)`、`handleServiceError(...)`、`handleServerStarted(...)`。
- 提炼 `resolveSelectedClientId()`，收敛目标选择校验。
- 保留 `startAndBindService()`、`onDestroy()`、按钮接线和页面入口骨架。

没有做的事：
- 没有改动服务启动和绑定顺序。
- 没有把入口流程拆到额外 delegate / manager。

原因：
- 对入口页来说，最重要的是保留生命周期和服务交互骨架的可读性，重复收敛应停留在旁路桥接层。

## ClinicFormatters: 小型纯格式化类未必需要继续拆

对应代码：
- `app/src/main/java/com/wifi/optometry/util/ClinicFormatters.java`

判断：
- 该类已经足够小、纯、直接，格式规则集中且容易定位。
- 当前没有足够高价值的重复支撑进一步拆分。

采取动作：
- 这一轮不直接修改它。
- 把它作为“纯格式化类先判断是否已经足够好”的样本写回 skill。

没有做的事：
- 没有为了统一再拆更多 formatter helper。
- 没有把展示格式分散到多个小函数或多个文件。

原因：
- 对这种小型纯工具类，过度重构反而可能降低格式规则的集中性。

## AckCodec / AckFactory: ACK 文本协议先保护前缀、字段约定与渠道语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/ack/AckCodec.java`
- `lib/src/main/java/com/wifi/lib/command/ack/AckFactory.java`

判断：
- `AckCodec` 真正承载的是 ACK 文本协议的线格式：状态前缀、`TYPE / REF / SESSION / TS / CODE / MSG / EXT_*` 这些字段名、`Uri.encode / decode`、失败时必须带错误码，以及 extra key 的归一化方式。
- `AckFactory` 虽然方法成对出现，但这些 `success / failure + command / transfer / stream` 的显式组合，本身就是调用端最清楚的 ACK 渠道语义清单。
- 这类协议编解码和便捷工厂最安全的切入点是补只读 helper，不是改前缀、字段名、可选字段规则或工厂入口形态。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“ACK 文本协议优先保护字段约定、渠道语义和显式工厂入口”的样本写回 skill。

没有做的事：
- 没有把字段拼装改成通用字段注册表或反射式 codec。
- 没有把 `AckFactory` 折叠成一个高参数化的 `create(...)` 再让调用端自己传状态和渠道。
- 没有改动 ACK 前缀、字段名、失败 ACK 必须带 `errorCode` 的校验，或 `EXT_` 扩展字段的归一化语义。

原因：
- 对 ACK 协议层来说，最重要的是一眼看见线格式和渠道语义；当前显式结构比再抽象一层更利于排查设备侧兼容问题。

## CommandReservation / CommandTable: 命令预留位与已加载编码表保留显式索引和匹配语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/CommandReservation.java`
- `lib/src/main/java/com/wifi/lib/command/CommandTable.java`

判断：
- `CommandReservation` 本质上是“预留命令位 + 业务说明”的轻量目录项，`moduleName / subModuleName / actionName / codeExplanation / description` 这些字段本身就是业务可读性来源。
- `CommandTable` 真正保护的是定义列表按 `order` 排序、重复编码拦截、不可变快照，以及 `matchIncoming(...)` 逐条匹配并生成 `InboundCommand` 的直接语义。
- 这两类命令目录对象更适合保留直接字段和显式匹配，不适合为了“更通用”再套 registry、mapper 或额外索引层。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“命令目录对象保留显式索引、排序和匹配语义”的样本写回 skill。

没有做的事：
- 没有把 `CommandReservation` 改成 builder、枚举组合或多层描述对象。
- 没有改动 `CommandTable` 对重复编码的拦截、按顺序匹配的主流程或 `InboundCommand` 的创建时机。
- 没有把编码表查找和匹配再拆成额外 cache / matcher / resolver 链。

原因：
- 对命令目录来说，最重要的是编码索引和匹配规则直接可见；当前结构已经很好地贴合了“查表 + 命中”的使用方式。

## ApiResult / EchoEnvelope: 网络返回载体保留静态工厂和直接字段结构

对应代码：
- `lib/src/main/java/com/wifi/lib/network/ApiResult.java`
- `demo/src/main/java/com/example/wifidemo/sample/network/model/EchoEnvelope.java`

判断：
- `ApiResult` 集中表达 `success / httpCode / code / data / message / errorBody / throwable`，`success(...)` 和 `failure(...)` 的静态工厂已经把“成功/失败结果该带什么”说得很清楚。
- `EchoEnvelope` 是 demo 网络示例里的 Gson 映射对象，`url / origin / method / data / args / form / files / headers / json` 这些直接字段和 setter 正是 JSON 包装层的边界。
- 这类网络返回载体更适合保持扁平和直达，不适合为了形式继续引入 builder、sealed result hierarchy 或额外 mapper。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“网络返回载体优先保留静态工厂和直接字段映射”的样本写回 skill。

没有做的事：
- 没有把 `ApiResult` 改成多层状态子类、Result 包装器链或异常专用对象树。
- 没有把 `EchoEnvelope` 的字段拆成更多嵌套 DTO 或隐藏掉 setter。
- 没有改动 `ApiResult.failure(...)` 在失败时固定 `data=null` 的语义，或 `EchoEnvelope` 各个 `Map` 字段的默认初始化方式。

原因：
- 对网络结果和 DTO 来说，最重要的是映射边界直接、成功失败语义清楚；当前结构比继续包装更利于调试接口返回。

## JLogEntry: 日志记录值对象保留展示拼装与优先级映射语义

对应代码：
- `lib/src/main/java/com/wifi/lib/log/JLogEntry.java`

判断：
- 该类本质上是单条日志记录的快照，`timestamp / priority / tag / message / throwable / threadName` 这些字段就是日志链路的边界本身。
- `getDisplayMessage()` 和 `getPriorityLabel()` 虽然是小 helper，但它们直接贴着字段和 UI 展示需求，比分散到额外 formatter 更直观。
- 真正应保护的是异常栈拼接语义、优先级到单字母标签的映射，以及字段快照本身的稳定性，不在方法数量本身。

采取动作：
- 这一轮不直接修改它。
- 把它作为“日志记录值对象保留展示拼装和优先级映射”的样本写回 skill。

没有做的事：
- 没有把日志记录拆成 message / throwable / thread 多个展示对象。
- 没有改动 `getDisplayMessage()` 中“消息为空则只返回栈，否则消息后拼接换行和栈”的语义。
- 没有改动 `DEBUG / INFO / WARN / ERROR / 其他 -> V` 的优先级标签映射。

原因：
- 对单条日志记录来说，最重要的是快照字段和展示语义足够直接；当前结构已经很适合被日志浮层、导出器和调试页面复用。

## TransferPacketCodec / StreamPacketCodec / StreamFrame: 文本帧编解码与流帧值对象先保护字段顺序、长度校验和结束语义

对应代码：
- `lib/src/main/java/com/wifi/lib/command/transfer/TransferPacketCodec.java`
- `lib/src/main/java/com/wifi/lib/command/stream/StreamPacketCodec.java`
- `lib/src/main/java/com/wifi/lib/command/stream/StreamFrame.java`

判断：
- `TransferPacketCodec` 和 `StreamPacketCodec` 真正保护的是固定前缀、固定字段名、文本帧字段顺序、Base64 载荷和 `SIZE` 对实际 payload 长度的校验，不是“能不能抽出一个更通用的 packet codec”。
- `StreamFrame` 保护的是 `sessionId`、`sequence`、时间戳裁剪、防御性复制、CRC 默认值和 `endOfStream` 语义，这些构造期约束和派生 helper 本身就是实时流边界的一部分。
- 对这类帧对象和 codec，最安全的切入点是补只读辅助逻辑，而不是修改线格式、结束帧表示、CRC 规则或字段解析顺序。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“文本帧编解码优先保护字段顺序、长度校验和结束语义”的样本写回 skill。

没有做的事：
- 没有把 transfer 和 stream 两套文本帧合并成单一泛化 codec。
- 没有改动 `TF+`、`SF+` 前缀、`SID / IDX / TOTAL / OFFSET / SIZE / CRC / DATA`、`SEQ / TS / EOS` 等字段名，或 Base64 解码后的长度校验。
- 没有改动 `StreamFrame` 的 `sequence > 0`、默认 CRC、payload 防御性复制或 `isEndOfStream()` 的语义。

原因：
- 对文本帧协议来说，最重要的是线格式稳定、结束语义直接、校验点明确；当前显式实现比再抽象一层更利于和设备协议对齐。

## BaseVBBottomSheetDialog: BottomSheet 模板基类先保护初始化与展示位置骨架

对应代码：
- `lib/src/main/java/com/wifi/lib/baseui/BaseVBBottomSheetDialog.java`

判断：
- 该类真正的价值是把 `ViewBindingReflector.inflate(...) -> setContentView(...) -> initWidgets() -> bindListeners() -> setShowBottomWithAnim()` 这条底部弹层模板骨架稳定交给子类。
- 敏感点在 `show()` 时的全高处理、`setShowPosition(...)` 对窗口宽高和 gravity 的设置、以及 `setShowBottomWithAnim()` 的动画入口，不在于能不能再拆几个 helper。
- 对基础弹层基类来说，最安全的切入点是只读辅助函数或局部文案，不是改初始化顺序、默认全高策略或窗口属性设置。

采取动作：
- 这一轮不直接修改它。
- 把它作为“BottomSheet 模板基类优先保护初始化与展示位置骨架”的样本写回 skill。

没有做的事：
- 没有改动构造函数里 `init()` 的调用时机。
- 没有改动默认 `fullHeight=true`、`show()` 中的 peekHeight 行为或底部动画样式入口。
- 没有把模板 hook 再拆成额外 delegate、builder 或多层窗口配置器。

原因：
- 对基础 BottomSheet 基类来说，最重要的是子类一打开就能得到稳定的绑定、初始化和展示位置语义；当前结构已经足够直接可靠。

## ReportRecord: 镜像报告模型保持直接字段聚合与列表容器语义

对应代码：
- `app/src/main/java/com/wifi/optometry/domain/model/ReportRecord.java`
- `demo/src/main/java/com/example/wifidemo/clinic/model/ReportRecord.java`

判断：
- 这两个类本质上都是报告快照：`id / patientName / programName / createdAt / visionSummary / prescriptionSummary / qrPayload / finalRight / finalLeft / metrics` 这些字段共同构成报告边界。
- `metrics` 直接作为列表容器暴露，和 `finalRight / finalLeft` 一起表达“报告就是一组汇总字段 + 最终结果 + 指标列表”的结构，当前没有足够收益支撑再拆 section、summary wrapper 或 builder。
- `app` 和 `demo` 中是镜像实现，后续如果新增报告字段或调整默认容器语义，应默认同步评估两边。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“镜像报告模型保持直接字段聚合与列表容器语义”的样本写回 skill。

没有做的事：
- 没有把报告拆成更多嵌套对象、builder 或 mapper。
- 没有把 `metrics` 改成额外的分页容器、展示对象或只在 getter 中动态计算。
- 没有只修改其中一个模块的字段结构或默认列表承载方式。

原因：
- 对轻量报告模型来说，最重要的是字段聚合直接、读写路径简单；当前结构比继续包装更利于页面和 demo 示例直接使用。

## DemoViewModel: 演示页 ViewModel 保留显式动作，状态同步 helper 留在本类

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/ui/DemoViewModel.java`

判断：
- `refreshRecords()`、`addMockRecord()`、`appendNote()`、`updatePermissionState()`、`appendSystemRecord()` 本身就是 demo 页要演示的动作清单，适合继续显式保留。
- 稳定的公共骨架其实已经收敛在 `syncState(...)` 里，当前最重要的是保留“做了什么动作”而不是把每个动作再套成统一命令对象。
- 敏感点在 `showLoading() -> handler.postDelayed(...) -> syncState() -> hideLoading() / dispatchMessage()` 这类页面反馈顺序，不在方法数量本身。

采取动作：
- 这一轮不直接修改它。
- 把它作为“演示页 ViewModel 保留显式动作，状态同步 helper 留在本类”的样本写回 skill。

没有做的事：
- 没有把 demo 动作改成场景注册表、命令总线或脚本配置。
- 没有改动刷新延时、提示文案或 `dispatchMessage(...)` 的触发时机。
- 没有把 `syncState(...)` 再拆到额外 presenter / reducer。

原因：
- 对教学型 ViewModel 来说，最重要的是一眼看见每个动作会怎么改状态；当前结构已经在显式动作和局部去重之间取得了平衡。

## BaseBrvahScenarioFragment / BrvahLoadMoreFragment: BRVAH 场景基类与加载更多示例保留模板骨架和尾部状态映射

对应代码：
- `demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BaseBrvahScenarioFragment.java`
- `demo/src/main/java/com/example/wifidemo/sample/brvah/ui/BrvahLoadMoreFragment.java`

判断：
- `BaseBrvahScenarioFragment` 的职责就是给各个 BRVAH 场景页提供统一的标题、说明、按钮和空态模板，`configureScenario(...)`、`setScenarioState(...)`、`createEmptyView(...)` 这些 helper 已经贴着 UI 模板边界存在。
- `BrvahLoadMoreFragment` 的价值则在于显式演示 `QuickAdapterHelper`、Header/Footer、尾部自动加载和 `LoadState` 映射，`mapLoadState(...)` 这种分支本身就是示例说明的一部分。
- 对 BRVAH 教学型页面来说，真正应保护的是列表装配顺序、监听器接线和尾部状态切换，而不是把场景页改成统一 DSL 或通用 load-more 引擎。

采取动作：
- 这一轮不直接修改它们。
- 把它们作为“BRVAH 场景基类和加载更多示例保留模板骨架与尾部状态映射”的样本写回 skill。

没有做的事：
- 没有把场景说明、按钮区和空态模板改成额外 JSON/配置驱动。
- 没有把 `BrvahLoadMoreFragment` 的 Header/Footer、点击监听或 `mapLoadState(...)` 再抽成通用分页基类。
- 没有改动 `QuickAdapterHelper` 的 attach 顺序、尾部自动加载设置或观察 `reportUiState` 后的渲染流程。

原因：
- 对 BRVAH 示例页来说，最重要的是读代码的人能直接看懂“这个场景在演示什么”；当前显式模板和状态映射已经比更抽象的方案更清楚。
