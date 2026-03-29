# 测试与验证边界

## 当前仓库现状

- 已验证可用：
  - `.\gradlew.bat projects`
  - `.\gradlew.bat :app:assembleDebug`
  - `.\gradlew.bat :demo:assembleDebug`
- 当前限制：
  - `.\gradlew.bat :demo:testDebugUnitTest` 默认会失败，因为 `demo` 模块还没有补齐 JUnit 测试依赖

## 默认验证策略

- 先跑最小影响面的构建命令
- 只有当改动触达共享基础设施时，再扩大到双模块验证
- 不要把“补测试依赖”当成每次任务的默认动作

## 何时需要测试依赖

只有以下情况再考虑补：

- 用户明确要求补单元测试或仪器测试
- 当前任务本身就是修测试体系
- 某段逻辑高风险且已有明确测试落点，补测试比只跑 assemble 更合适

## 不该默认做的事

- 不要因为外部 Android skill 提到 Robolectric / Espresso / Managed Device，就默认往当前仓库加一整套测试栈
- 不要把 `demo:testDebugUnitTest` 当成常规收尾步骤，除非本轮先补齐依赖
- 不要为了凑“完整流程”而新增大量测试基础设施，超出当前任务边界

## 推荐汇报方式

最终说明里尽量明确写出：

- 本轮改动影响了哪个模块
- 实际跑了哪些 Gradle 命令
- 哪些命令没有跑，以及为什么
- 如果涉及 `lib/` 或镜像实现，是否双边验证
