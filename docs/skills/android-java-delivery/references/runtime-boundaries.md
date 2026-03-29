# 运行时边界

## 线程边界

### 默认原则

- UI 更新、`LiveData` 推送、View 状态变更要回主线程。
- 网络请求、文件读写、数据库访问、TCP/UDP 收发不要放主线程。
- 解析、压缩、校验、排序等重计算不要阻塞页面线程。

### Java 项目里的落地方式

- 使用已有的 `Handler`、`Executor`、前台 `Service`、Repository 封装和库里的基础设施。
- 不要为了套用外部 skill 规则，顺手把当前 Java 代码改成 coroutine / `viewModelScope`。
- 如果只是修线程问题，优先做最小切换，不顺手重写异步模型。

## 生命周期与资源清理

### 必须成对出现的东西

- `register` / `unregister`
- `bindService` / `unbindService`
- `addObserver` / `removeObserver`
- `addListener` / `removeListener`
- `postDelayed` / `removeCallbacks`
- `open` / `close`

### 在当前仓库尤其要小心的点

- `Service` 绑定和解绑顺序
- socket、流、心跳、设备发现监听器的清理顺序
- Fragment `onCreateView()` / `onDestroyView()` 之间的 ViewBinding 生命周期
- Activity / View / Dialog 中匿名监听器是否会滞留

如果任务涉及这些链路，同时结合 `../java-clean-refactor/SKILL.md` 做受保护边界判断，不要只因为“能提炼 helper”就重排主时序。

## DTO 与接口返回

### 默认策略

- 服务端返回字段要允许“缺省”这件事显式存在，不要用一堆假默认值把问题吞掉。
- DTO 层优先保持“原始映射语义”，展示兜底放在 UI 或转换层。
- 对 Gson / Jackson 模型，宁可显式判空，也不要把“服务端没给值”和“服务端给了空字符串”混为一谈。

### Java 写法下的含义

- 可以用引用类型的 `null` 表达缺字段。
- 需要默认值时，在转换成 UI 状态、表单态或展示文案时再兜底。
- 如果当前类是轻量模型且字段本身就是边界，不要为了更“面向对象”再套 mapper 或 wrapper。

## 异常与错误处理

- 不要无声吞异常。
- Repository / 数据层可以包装错误并向上抛出或返回明确失败结果。
- ViewModel / 页面层负责把错误转换成 UI 状态、提示文案或日志。
- 如果错误来自受保护边界，不要顺手改变回调顺序或失败分支语义。

## 日志等级

当前仓库优先沿用已有 `DLog` / `JLog`，不要默认切回零散的 `android.util.Log`。

| 等级 | 用途 |
|------|------|
| `d` | 调试细节、状态快照、计数变化 |
| `i` | 正常流程关键节点、启动、注册、成功收尾 |
| `w` | 可恢复异常、兜底分支、忽略重复请求 |
| `e` | 失败、异常、不可恢复问题 |

## 什么时候要更保守

以下场景默认按高风险处理：

- 前台 `Service`
- 设备发现与 MAC 归一化
- TCP/UDP 收发
- 心跳与定时任务
- 事务写入
- 导出与分享
- 流式传输与协议状态机

这些场景下，优先只动纯 helper、格式化、空值保护、日志和无副作用辅助逻辑。
