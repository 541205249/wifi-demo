# Stream 框架说明

## 1. 这套框架是干什么的

这套 `stream` 基础层是给“实时连续数据”准备的。

它解决的是下面这些问题：

- 数据是一帧一帧不断来的
- 不适合按“一个按钮发一条命令、设备回一条命令”来处理
- 需要记录帧序号、时间戳、校验值
- 需要发现丢帧或乱序
- 需要把实时流和普通命令、文件传输分层

它和 `command` 的关系是：

- `command` 负责“开始流、停止流、流状态、流错误”
- `stream` 负责“真正每一帧实时数据怎么编码、怎么接收、怎么统计”

最简单地说：

- 小消息继续用 `command`
- 大文件用 `command + transfer`
- 实时连续数据用 `command + stream`

## 2. 当前提供了什么

代码位置：

- `StreamMetadata`
- `StreamFrame`
- `StreamPacketCodec`
- `StreamSender`
- `StreamReceiver`
- `StreamStats`

## 3. 一条实时流一般怎么走

以“模块持续上报验光波形给 App”为例：

### 第一步：先走命令层握手

App 先发一条控制命令：

- `s330101` -> `STREAM_START+TYPE=waveform,RATE=50`

模块收到以后回：

- `r330102` -> `STREAM_START=OK+SESSION=abc123`

这一步的意思是：

- 告诉模块“开始推实时流”
- 告诉模块流类型，例如波形、实时测量值
- 告诉模块采样频率或刷新节奏
- 双方确认这次流会话的 `sessionId`

### 第二步：进入 stream 实时帧

模块开始一帧一帧发数据。

每一帧大致长这样：

```text
SF+SID=abc123,SEQ=1,TS=1711620000000,SIZE=24,EOS=0,CRC=8a1f6c2d,DATA=BASE64...
```

这几个字段分别是什么意思：

- `SID`：这次实时流的会话 ID
- `SEQ`：第几帧
- `TS`：这一帧的时间戳
- `SIZE`：这一帧的数据长度
- `EOS`：是否结束，`1` 表示结束帧
- `CRC`：这一帧自己的 CRC32 校验值
- `DATA`：真正的数据内容，做了 Base64 编码

### 第三步：接收端逐帧处理

接收端用 `StreamReceiver` 一帧一帧收。

它会做这些检查：

- `sessionId` 对不对
- `CRC` 对不对
- `sequence` 有没有回退
- 有没有丢帧
- 这一帧长度有没有超出约定的 `frameSize`

和文件传输不同的是：

- 实时流允许“统计丢帧并继续”
- 不要求把所有帧都攒齐后再一次性组装
- 一般是收到一帧处理一帧、刷新一帧 UI

### 第四步：流结束

模块停流时，通常会有两层动作：

1. 命令层发 `STREAM_STOP=OK`
2. 数据层发一个 `EOS=1` 的结束帧

这样业务层和传输层都能知道“这次流已经结束了”。

## 4. 代码示例

### 4.1 发送端逐帧发送

```java
StreamMetadata metadata = new StreamMetadata.Builder()
        .setSessionId(StreamMetadata.createSessionId())
        .setDirection(StreamDirection.DEVICE_TO_APP)
        .setStreamType("waveform")
        .setSampleRateHz(50)
        .setFrameSize(128)
        .build();

StreamSender sender = new StreamSender();
sender.start(metadata);

sender.sendPayload("WAVE+12,18,21,26".getBytes(StandardCharsets.UTF_8), frame -> {
    tcpServerService.sendMessageToClient(clientId, frame);
});
```

这是什么意思：

- 先定义这次流是什么类型
- 再让 `StreamSender` 按帧编号和打包
- 每发一帧，就交给 TCP 层

### 4.2 接收端收帧并统计

```java
StreamReceiver receiver = new StreamReceiver();
receiver.start(metadataFromStartCommand);

StreamStats stats = receiver.acceptFrame(rawStreamFrame);
```

这是什么意思：

- 先根据握手阶段拿到的流参数 `start`
- 每来一帧就 `acceptFrame`
- 每次都能拿到累计统计信息，例如帧数、字节数、丢帧数

## 5. 它适合什么，不适合什么

### 适合

- 实时波形
- 实时测量值
- 传感器连续输出
- 连续图表刷新

### 目前不太适合

- 视频级高吞吐流
- 多路高并发复用流
- 自适应码率流
- 需要复杂重传与拥塞控制的场景

这些后面可以继续扩：

- ACK / NACK
- 自定义采样窗口
- 抖动缓冲
- 丢帧补偿
- 二进制流帧编解码器

## 6. 现阶段推荐你怎么用

你当前项目还在搭架构，最推荐的做法是：

1. 实时流开始与停止，继续放在 `command` 编码表里
2. 真正的每一帧数据，统一交给 `stream`
3. UI 层不要自己处理帧校验，交给 `StreamReceiver`
4. 文件传输和实时流保持分开，不要混到一起

这样后面协议扩展时最稳，不容易把小指令、文件传输、实时流搅在一起。

## 7. 建议配合统一 ACK 使用

如果你希望实时流开始、停止、异常这些控制回执也统一，建议再配合：

- `lib.command.ack`

例如：

- 开始成功：`ACK+TYPE=STREAM,REF=s330101,SESSION=wave001,TS=...,MSG=stream_started`
- 流中断：`ERR+TYPE=STREAM,REF=s330101,SESSION=wave001,TS=...,CODE=INTERRUPTED,MSG=wifi_jitter`

这样实时流的控制回执、错误回执和普通命令、文件传输都能共用一套解析入口。
