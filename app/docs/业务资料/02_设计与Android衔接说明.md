# 设计与 Android 衔接说明

## 1. 目标

本文档解决两个问题：

- 如何把当前需求快速转成可出稿的平板横屏设计文件。
- 如何让设计稿更容易落进 Android 项目，而不是停留在视觉稿层面。

## 2. Figma/国产设计工具的页面拆分建议

建议按 5 个 Page 管理设计文件：

1. `00_Foundation`
   - 颜色
   - 字体
   - 间距
   - 圆角
   - 阴影
   - 图标规范
2. `01_Components`
   - 顶部状态栏
   - 左侧操作按钮
   - 参数格
   - 旋钮控件
   - 视标卡片
   - 底部工具卡片
   - 弹窗
   - 表格
3. `02_Screens_Workbench`
   - 首页默认态
   - 首页设备离线态
   - 首页程序执行态
   - 首页视功能工具展开态
4. `03_Screens_Modules`
   - 被测者
   - 报告中心
   - 程序中心
   - WiFi/设备连接
   - 步长设置
   - 时间语言
   - 公司云端设置
5. `04_Handoff`
   - 标注稿
   - 交互流
   - Android 资源导出区

## 3. 视觉基线建议

### 3.1 屏幕尺寸

- 基线 Frame：`2560 x 1600`
- 同时补一套 `1920 x 1200` 检查稿
- 页面最外层建议预留 `48 px` 安全边距

### 3.2 栅格

- 12 列栅格
- 左右边距：48
- 列间距：24
- 主工作台按 “左侧栏 + 中央参数 + 右侧视标” 三栏布局

### 3.3 字体建议

验光设备类产品更适合稳定、清晰、数字可读性高的方案：

- 中文：思源黑体 / HarmonyOS Sans SC
- 英文和数字：DIN / Roboto Condensed / HarmonyOS Sans

### 3.4 色彩建议

建议以“医疗设备蓝 + 中性灰 + 功能色”组织，而不是消费级强装饰风格：

- 主色：连接、选中、主 CTA
- 成功色：设备在线、保存成功
- 警告色：未保存、参数越界
- 错误色：连接失败、导入失败
- 中性色：背景、分割线、禁用态

## 4. 组件层建议

## 4.1 必须组件

| 组件 | 说明 | Android 映射建议 |
| --- | --- | --- |
| `SideActionButton` | 左侧功能按钮 | Compose 自定义按钮 / Material Button |
| `MetricChip` | 参数格，支持选中和编辑态 | 自定义可选卡片 |
| `KnobControl` | 旋钮控制器 | 自定义 View 或 Compose Canvas |
| `ChartSwitcher` | 视标切换器 | 横向列表或网格 |
| `ToolCard` | NPC/NRA 等工具卡片 | 底部抽屉或内容卡片 |
| `StatusBadge` | 在线/离线/草稿标签 | 小尺寸标签 |
| `ProgramStepBar` | 当前程序步骤条 | 横向步骤组件 |
| `RefractionTable` | 报告表格 | LazyColumn / RecyclerView |
| `DeviceListItem` | 设备连接列表项 | 列表组件 |

## 4.2 参数格的状态定义

`MetricChip` 至少要有以下状态：

- 默认态
- 选中态
- 正在调节态
- 禁用态
- 异常态
- 来自导入数据态

## 4.3 弹窗类型

- 被测者录入弹窗
- 帮助弹窗
- 保存确认弹窗
- 未保存离开确认弹窗
- 导入覆盖确认弹窗
- 连接失败弹窗

## 5. 设计稿命名规范

### 5.1 页面命名

建议统一采用：

`模块_页面_状态`

例如：

- `Workbench_Home_Default`
- `Workbench_Home_DeviceOffline`
- `Report_Current_Vision`
- `Program_Regular_StepDetail`
- `Settings_StepLength_Default`

### 5.2 组件命名

建议统一采用：

`Comp/模块/组件名/状态`

例如：

- `Comp/Home/MetricChip/Selected`
- `Comp/Common/Dialog/Confirm`
- `Comp/Program/StepBar/Active`

## 6. Android 侧页面与路由建议

## 6.1 页面路由

| Route | 页面 | 说明 |
| --- | --- | --- |
| `home/workbench` | 验光工作台 | 主入口 |
| `subject/manage` | 被测者管理 | 弹窗或独立页 |
| `report/current` | 当前报告 | 三页签 |
| `report/history` | 历史报告 | 列表 |
| `program/list` | 程序列表 | 三类程序 |
| `program/detail` | 程序配置详情 | 步骤编辑 |
| `device/connection` | 设备连接 | WiFi 服务和外设 |
| `settings/step` | 步长设置 | 配置页 |
| `settings/time-language` | 时间语言 | 配置页 |
| `settings/company-cloud` | 公司云端 | 配置页 |

## 6.2 包结构建议

如果你是 MVVM 或 MVI 架构，建议至少拆成：

```text
ui/
  home/
  subject/
  report/
  program/
  device/
  settings/
domain/
  model/
  usecase/
  repository/
data/
  local/
  remote/
  device/
core/
  ui/
  design/
  navigation/
  logger/
```

## 7. 状态和事件建模建议

## 7.1 首页状态对象建议

```kotlin
data class WorkbenchUiState(
    val currentSubject: SubjectUi? = null,
    val currentProgram: ProgramUi? = null,
    val deviceState: DeviceStateUi = DeviceStateUi(),
    val distanceMode: DistanceMode = DistanceMode.FAR,
    val eyeMode: EyeMode = EyeMode.RIGHT,
    val dataSource: LensDataSource = LensDataSource.SUBJECTIVE,
    val selectedMetric: RefractionMetric? = null,
    val prismMode: PrismMode = PrismMode.CARTESIAN,
    val cylinderSignMode: CylinderSignMode = CylinderSignMode.MINUS,
    val activeChart: ChartType = ChartType.VA,
    val activeTool: ToolType? = null,
    val hasUnsavedChanges: Boolean = false
)
```

## 7.2 首页事件建议

```kotlin
sealed interface WorkbenchAction {
    data object ClickSubject : WorkbenchAction
    data object ClickHelp : WorkbenchAction
    data object ClickIn : WorkbenchAction
    data object ClickPrint : WorkbenchAction
    data object ClickShift : WorkbenchAction
    data class SelectMetric(val metric: RefractionMetric) : WorkbenchAction
    data class AdjustMetric(val delta: BigDecimal) : WorkbenchAction
    data class SelectChart(val chart: ChartType) : WorkbenchAction
    data class SelectTool(val tool: ToolType) : WorkbenchAction
    data class ChangeDistanceMode(val mode: DistanceMode) : WorkbenchAction
}
```

## 7.3 设备层接口建议

把设备通信和页面动作彻底解耦：

```kotlin
interface DeviceCommandGateway {
    suspend fun connectMainDevice(deviceId: String): Result<Unit>
    suspend fun connectPrinter(deviceId: String): Result<Unit>
    suspend fun sendLensInCommand(payload: LensInPayload): Result<Unit>
    suspend fun switchLamp(on: Boolean): Result<Unit>
    suspend fun updateRefraction(payload: RefractionPayload): Result<Unit>
}
```

这样无论后面是 WiFi、串口网关还是其他传输层，UI 层都不用改太多。

## 8. 设计到开发的交付清单

设计师交付时建议必须包含：

1. 页面设计稿
2. 交互状态稿
3. 组件库
4. 字体和字号规范
5. 颜色 token
6. 间距 token
7. 图标导出
8. 切图或 SVG
9. 关键交互说明
10. 页面跳转图

## 9. Android 对设计稿的接入建议

### 9.1 如果你用 Jetpack Compose

- 优先把颜色、字号、圆角做成 `DesignToken`
- 参数格、左侧按钮、步骤条都组件化
- 旋钮用 `Canvas` 自绘
- 大列表页如报告历史用 `LazyColumn`

### 9.2 如果你用 XML + ViewBinding

- 先抽公共 style 和 shape
- 报告页和设置页可以先 XML 实现
- 旋钮和视标区域仍建议自定义 View
- 复杂状态切换用 `StateFlow` 或 `LiveData`

## 10. 资源利用建议

当前资源建议分三类使用：

1. Word 截图
   - 用于还原页面布局、按钮文案、模块边界
2. `res/` 资源包
   - 用于二次比对图标、颜色、背景形态
3. 你后续 Android 工程资源
   - 用于输出最终命名规范和可维护资源

说明：

- 现有 `res/` 文件命名已明显脱离业务语义，更适合作为视觉比对材料，不建议直接照搬命名。
- 你自己的项目里建议重新建立一套有语义的资源命名。

## 11. 设计验收检查单

- 所有页面均为横屏
- 首页存在默认态、离线态、程序执行态
- 所有弹窗均有关闭和确认路径
- 报告页有“视力 / 视功能 / 处方”三个页签
- 程序页覆盖三种程序类型
- 步长设置可覆盖普通步长和 SHIFT 步长
- 设备连接页能区分主设备和打印机
- 文案中所有医学术语拼写统一
- 颜色和字号已 token 化
- 组件已可复用，不是纯页面拼装
