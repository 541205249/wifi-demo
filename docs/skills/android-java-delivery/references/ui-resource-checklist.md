# XML、资源与 UI 基线

## 当前仓库默认 UI 路线

- Android View + XML + ViewBinding
- 新页面优先复用 `lib/` 中已有基类
- 不默认引入 Compose
- 不默认按 Material 3 做大规模视觉重设计

## 页面开发优先级

1. 先复用已有基类和页面骨架。
2. 先保证布局可用、事件清楚、状态完整。
3. 再看文案复用、命名整理和局部样式统一。
4. 不为了一次改动顺手把全页面翻成另一套设计语言。

## 资源命名风格

当前仓库已经有稳定风格，新增资源优先跟随现状：

| 类型 | 当前推荐前缀 | 示例 |
|------|--------------|------|
| Activity 布局 | `activity_` | `activity_main.xml` |
| Fragment 布局 | `fragment_` | `fragment_report.xml` |
| Dialog 布局 | `dialog_` | `dialog_demo_tips.xml` |
| 列表项布局 | `item_` | `item_brvah_report.xml` |
| 复用 View 布局 | `view_` | `view_nav_header.xml` |
| 图标 | `ic_` | `ic_launcher` |
| 背景 | `bg_` | `bg_card` |

## 避免的资源命名

不要新建过于空泛或容易冲突的裸名字：

- `background`
- `icon`
- `image`
- `layout`
- `view`
- `button`
- `app`
- `data`
- `action`

更好的写法：

- `screen_background`
- `ic_device_connected`
- `bg_report_card`
- `view_device_empty_state`

重点不是死记“保留字清单”，而是避免含义过泛、后期难检索、容易和系统概念混淆的名字。

## 文案规则

- 新增可复用文案优先放进 `strings.xml`
- 一次性调试文案、强业务内联文案可以按任务要求处理
- 不要顺手批量迁移旧页面里的所有内联文本

## 交互与可访问性

### 默认检查项

- 交互控件点击区域尽量不小于 `48dp`
- 纯图标按钮、重要交互入口补 `contentDescription`
- 装饰性图片不要伪装成有意义控件
- 至少考虑空态、加载态、错误态是否可见

### 不要机械套模板

- 已有文本按钮如果语义非常清楚，不必为了形式额外堆说明文案
- 工具型页面和设备调试页优先保证信息密度和可操作性，不默认照搬内容型应用的视觉规范

## 与当前项目强相关的建议

- 新页面优先看 `BaseVBActivity`、`BaseMvvmActivity`、`BaseMvvmFragment`、`BaseVBFragment` 这一套基类能力
- 如果只是补功能，不要顺手把 XML 页面改成 Compose
- 如果只是调样式，不要顺手把整个页面重做成 Material 3 视觉实验
- 对 `demo/` 中教学型页面，显式按钮清单、场景清单和示例布局经常本来就该保留
