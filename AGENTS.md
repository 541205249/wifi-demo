# 项目协作说明

## 项目概况

- 这是一个多模块 Android 项目，核心场景是基于 WiFi 的验光业务流程。
- `app/` 是正式业务应用，包名为 `com.wifi.optometry`。
- `demo/` 是演示和样例应用，包名为 `com.example.wifidemo`，同时放了设备资料和命令清单。
- `lib/` 是公共基础库，包含 BaseUI、MVVM、日志、数据库和通用工具。

## 主要入口

- 正式应用入口：`app/src/main/java/com/wifi/optometry/ui/MainActivity.java`
- Demo 应用入口：`demo/src/main/java/com/example/wifidemo/sample/ui/DemoHomeActivity.java`
- 公共 Activity 基类：`lib/src/main/java/com/wifi/lib/baseui/BaseVBActivity.java`

## 技术栈与约定

- 语言：Java 11。除非任务明确要求，否则优先继续使用 Java，不引入 Kotlin。
- UI 方案：Android View + XML + ViewBinding。除非明确要求，否则不要引入 Compose。
- 架构风格：轻量 MVVM，主要依赖 `BaseMvvmActivity`、`BaseMvvmFragment`、`LiveData`、`Repository` 和 `lib/` 里的基础类。
- 设备通信：以前台 `Service` 承载 TCP 通信，并结合 HC-25 相关的发现逻辑。

## 模块边界

- 可复用的基础设施放在 `lib/`，例如基础 UI、对话框、委托类、日志、数据库、通用框架能力。
- 正式验光流程、面向业务的页面和状态放在 `app/`。
- 样例页面、展示页、BRVAH 示例、演示专用流程放在 `demo/`。
- 如果改动影响 `lib/`，必须确认 `app` 和 `demo` 都还能正常构建。

## 重复代码约束

- `app` 和 `demo` 中存在一批相似的设备通信类，例如 `DeviceManager`、`DeviceHistoryStore`、`Hc25MacDiscoveryClient`、`TcpServerService`、`HeartbeatManager`、`ServerConstance`。
- 业务模型也有镜像结构，主要体现在 `app/domain/...` 和 `demo/clinic/...`。
- 修改共享行为之前，先检查两个模块是否都需要同步调整，不要默认只改一边。
- 如果本次改动有意只修改其中一个模块，必须在最终说明里明确指出，避免后续误判为遗漏。

## 设备与协议安全

- 不要随意修改 `SERVER_PORT`，当前 `app` 和 `demo` 都使用 `39509`。
- 涉及 HC-25 发现流程、命令格式、WiFi/TCP 行为的改动，先查本地资料，再动代码。
- 重点参考资料：
  - `demo/命令清单`
  - `demo/HC-25（板载天线）用户手册V2.0-20251212.pdf`
  - `demo/验光仪功能说明.docx`
- 真实设备兼容性优先级高于“为了代码更整齐”的重构。
- 修改 `DeviceHistoryStore`、`DeviceManager`、`TcpServerService`、`Hc25MacDiscoveryClient` 时，默认保持 MAC 归一化和历史记录行为不变，除非任务明确要求改变设备身份规则。

## UI 与代码风格

- 延续当前 XML + ViewBinding 方案，新增页面或能力时优先复用 `lib/` 中已有基类和基础组件。
- 优先做小范围、可验证的改动，不做大面积架构翻修。
- 新增可复用文案优先放入 `strings.xml`。除非任务明确要求，否则不要顺手把现有内联字符串批量迁移。
- 保持当前项目风格：Java 类结构直接、依赖关系显式、少做框架层面的折腾。

## 需要谨慎对待的目录与文件

- `build/`、`.gradle/`、`.tmp_res/` 属于构建产物或辅助目录。除非任务明确和生成物、资源恢复有关，否则不要改它们。
- `demo/` 下的硬件手册、命令清单属于参考资料，不是清理目标。

## 验证命令

- 已验证可用：
  - `.\gradlew.bat projects`
  - `.\gradlew.bat :app:assembleDebug`
  - `.\gradlew.bat :demo:assembleDebug`
- 当前限制：
  - `.\gradlew.bat :demo:testDebugUnitTest` 目前会失败，因为 `demo` 模块没有声明 JUnit 测试依赖。除非先补齐测试依赖，否则不要把单元测试当成默认验证手段。

## 建议的 Codex 工作方式

- 先读实际受影响的模块，不要默认 `app` 和 `demo` 的实现完全一致。
- 涉及设备通信时，同时检查代码和本地硬件资料。
- 验证时先跑最小范围的 Gradle 命令，只有在改动影响共享代码时再扩大验证范围。
