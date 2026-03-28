# dispatcher 分发层说明

## 1. 这一层解决什么问题

`gateway` 已经能把原始 TCP 文本识别成：

- `command`
- `ack`
- `transfer`
- `stream`
- `unknown`
- `invalid`

但如果业务层直接拿这些事件继续写判断，页面里很快就会出现很多：

- `if code == ...`
- `if ack.ref == ...`
- `if sessionId == ...`

所以在 `lib.command.dispatcher` 里补了一层统一分发器：

- 普通命令按 `编码` 路由
- ACK 按 `channel + ref`、`ref`、`sessionId` 路由
- transfer 按 `sessionId` 路由
- stream 按 `sessionId` 路由

这样页面或 ViewModel 只注册业务处理器，不再关心底层怎么分辨消息类型。

## 2. 核心类

| 类 | 作用 |
| --- | --- |
| `ProtocolDispatcher` | 协议事件总分发器 |
| `ProtocolDispatchResult` | 返回本次分发是否命中、是否失败、是否自动移除会话 |
| `CommandUseCase` | 处理命令编码事件 |
| `AckUseCase` | 处理 ACK / ERR 回执 |
| `TransferUseCase` | 处理分片传输事件 |
| `StreamUseCase` | 处理实时流事件 |

## 3. transfer / stream 为什么要注册会话

`transfer` 和 `stream` 不是单条消息语义，而是一段持续中的会话。

所以分发层会在注册会话时自动准备好接收器：

- `TransferReceiver`
- `StreamReceiver`

收到数据后会先做：

- sessionId 校验
- CRC 校验
- 进度统计
- 完成态判断
- 自动组包 / 自动统计

业务层拿到的已经不是裸分片，而是“带状态的上下文”。

## 4. 建议接法

### 4.1 普通命令

```java
dispatcher.registerCommandUseCase("r120202", context -> {
    // 刷新验光结果 UI / 存库
});
```

### 4.2 ACK

```java
dispatcher.registerAckUseCase(AckChannel.COMMAND, "s120101", context -> {
    // 处理“开始验光”成功或失败
});
```

### 4.3 文件传输

```java
TransferMetadata metadata = ...;
dispatcher.registerTransferSession(metadata, context -> {
    TransferProgress progress = context.getProgress();
    if (context.hasCompletedPayload()) {
        byte[] payload = context.getCompletedPayload();
        // 文件已经组包完成
    }
});
```

### 4.4 实时流

```java
StreamMetadata metadata = ...;
dispatcher.registerStreamSession(metadata, context -> {
    StreamStats stats = context.getStats();
    StreamFrame frame = context.getStreamFrame();
    // 刷新曲线、统计丢帧
});
```

## 5. 推荐链路

推荐整个通信链路按下面顺序使用：

1. TCP 收到原始消息
2. `ProtocolGateway.resolveInbound(raw)`
3. `ProtocolDispatcher.dispatch(clientId, event)`
4. 命中业务 `UseCase`
5. ViewModel / Repository 更新状态

这样职责会比较清楚：

- `gateway` 负责识别协议
- `dispatcher` 负责路由业务
- `usecase` 负责具体业务处理

## 6. 当前项目里的用途

现在这层已经适合承接：

- 验光结果上报
- 状态上报
- 开始 / 停止 ACK
- 模板文件接收
- 波形流接收

也就是说，后面你在正式业务里要做页面联动时，优先注册业务处理器，不要让页面自己直接判断原始字符串。
