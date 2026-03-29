---
name: android-java-delivery
description: 用于当前仓库的 Android Java 原生开发、XML/ViewBinding 页面开发、Gradle 构建排错、生命周期与线程边界检查、资源命名与可访问性检查。适用于用户要求“新增/修改 Android 页面”“修构建错误”“排查资源问题”“补 Android 工程护栏”“先确认值不值得改再动代码”这类场景。
---

# Android Java 交付护栏

## 工作目标

在不偏离当前项目技术栈的前提下，提高 Android 改动的可交付性、可验证性和边界清晰度。

## 当前仓库前提

- 语言默认是 Java 11，不默认切到 Kotlin。
- UI 默认是 Android View + XML + ViewBinding，不默认引入 Compose。
- 架构默认沿用轻量 MVVM、`LiveData`、`Repository` 和 `lib/` 里的基础类。
- 设备通信由前台 `Service`、TCP、HC-25 发现链路承载，真实设备兼容性优先。
- 仓库是多模块结构：`app/`、`demo/`、`lib/`。
- 这是 Windows 环境，Gradle 命令默认使用 `.\gradlew.bat`。

## 工作流程

1. 先确认本轮任务属于哪类：页面开发、资源调整、构建排错、生命周期清理、线程问题、DTO/网络映射、共享模块改动。
2. 先识别影响范围：只动 `app`、只动 `demo`、还是会影响 `lib` 或镜像实现。
3. 先选最小验证命令，确认当前基线可构建，再开始写业务改动。
4. 如果涉及 `Service`、socket、设备发现、事务写入、导出分享或其他时序敏感链路，先把它当受保护边界处理。
5. 优先保持现有 Java/XML/ViewBinding 方案，不为了“更现代”引入 Kotlin、Compose、Flavor 或大规模 UI 翻修。
6. 完成后按影响面做最小充分验证，并明确说明本轮没动什么。

## 决策规则

- 构建能过比“结构更优雅”更优先。
- 对 `lib/` 的改动默认同时验证 `app` 和 `demo`。
- 对 `app` / `demo` 的镜像实现，先判断是否需要同步，不默认只改一边。
- 新页面或新功能优先复用 `lib/` 中已有基类和基础组件。
- 新增可复用文案优先进 `strings.xml`，但不要顺手批量迁移老文案。
- 不默认补测试依赖；只有用户明确要求测试，或当前任务本身就是补测试，才扩测试配置。
- 如果问题本质是“小步重构和边界判断”，同时参考 `../java-clean-refactor/SKILL.md`。

## 参考资料

- 需要确认多模块构建、Gradle 排错、验证矩阵时，读取 `references/project-build-guardrails.md`
- 需要确认线程、生命周期、监听器清理、DTO 空值策略和日志等级时，读取 `references/runtime-boundaries.md`
- 需要确认 XML/ViewBinding 页面、资源命名、可访问性和 UI 基线时，读取 `references/ui-resource-checklist.md`
- 需要确认当前仓库测试边界、默认验证命令和何时补测试依赖时，读取 `references/testing-verification.md`

## 快速导航

- 如果当前是构建失败、依赖冲突、AAPT 报错、模块配置问题，优先读 `references/project-build-guardrails.md`
- 如果当前是 Activity / Fragment / 自定义 View / Service 代码，优先读 `references/runtime-boundaries.md` 和 `references/ui-resource-checklist.md`
- 如果当前涉及 `TcpServerService`、`HeartbeatManager`、`DeviceManager`、`DeviceHistoryStore`、`Hc25MacDiscoveryClient` 这类高风险链路，先结合 `../java-clean-refactor/SKILL.md` 判断受保护边界
- 如果当前是网络接口返回、Gson/Jackson 映射、Repository 收口或日志记录，优先读 `references/runtime-boundaries.md`
- 如果当前是 XML、drawable、string、layout、icon 或资源报错，优先读 `references/ui-resource-checklist.md`
- 如果当前需要决定跑哪些 Gradle 命令、要不要补测试依赖、要不要把单测当默认验证，优先读 `references/testing-verification.md`

## 使用路线

- 如果任务是“修 Android 构建 / 资源错误 / 配置错误”，先读 `references/project-build-guardrails.md`，按最小排错链路处理。
- 如果任务是“新增或修改页面”，先读 `references/ui-resource-checklist.md`，再结合 `references/runtime-boundaries.md` 检查生命周期和线程问题。
- 如果任务是“调 Service / 监听器 / 线程 / 回调”，先读 `references/runtime-boundaries.md`，确认哪些注册、清理、线程切换必须成对处理。
- 如果任务是“补交付质量护栏”，按 `references/project-build-guardrails.md`、`references/ui-resource-checklist.md`、`references/testing-verification.md` 组合使用。
- 如果任务最后发现不值得引入 Kotlin、Compose、Flavor、Material 3 全量改造，也要明确说出原因，而不是默认升级。

## 典型触发

- “帮我给这个 Android 页面加功能，但别偏离当前项目栈”
- “这个模块编不过，先帮我排查 Gradle / 资源问题”
- “这段 Service / Fragment / ViewBinding 代码有什么生命周期风险”
- “帮我看看这个 XML 页面和资源命名有没有明显问题”
- “先按 Android 工程护栏审一遍，再决定怎么改”

## 输出要求

- 先说明当前判断：影响了哪些模块、是否涉及受保护边界、用什么命令验证。
- 如果刻意不引入 Kotlin、Compose、Flavor、Material 3 大改，要明确说明是为了贴合当前仓库前提。
- 如果只验证了 `app` 或 `demo` 一边，要说明原因。
- 如果改动影响 `lib/` 或镜像实现，要明确说明是否同步验证或同步修改。
