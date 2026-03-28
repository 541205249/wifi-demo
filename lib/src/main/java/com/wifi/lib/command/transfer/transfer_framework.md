# Transfer 框架说明

## 1. 这套框架是干什么的

这套 `transfer` 基础层是给“文件传输、大内容分片传输”准备的。

它解决的是下面这些问题：

- 一个文件太大，不能直接塞进一条普通指令
- 需要把内容拆成很多片
- 每一片都要有索引、偏移、校验
- 接收端需要按顺序组装回来
- 需要知道当前进度

它和 `command` 的关系是：

- `command` 负责“开始传、准备好了、传输完成、失败”
- `transfer` 负责“真正的一片一片数据怎么发”

也就是说：

- 小消息继续用 `command`
- 大消息用 `command + transfer`

## 2. 当前提供了什么

代码位置：

- `TransferMetadata`
- `TransferChunk`
- `TransferPacketCodec`
- `TransferSender`
- `TransferReceiver`
- `TransferChecksums`
- `TransferProgress`

## 3. 一条文件传输一般怎么走

以“App 发一个报告模板文件给模块”为例：

### 第一步：先走命令层握手

App 先发一条控制命令：

- `s310101` -> `FILE_BEGIN+TYPE=template,NAME=exam.json,SIZE=20480,MD5=xxxx`

模块收到以后回：

- `r310102` -> `FILE_READY+SESSION=abc123,CHUNK=2048`

这一步的意思是：

- 告诉模块“我要传文件了”
- 告诉模块文件名、大小、摘要
- 模块回一个 `sessionId`
- 双方约好每片多大

### 第二步：进入 transfer 分片发送

App 用 `TransferSender` 把文件切成很多片，然后每片编码成文本帧发出去。

每一片大致长这样：

```text
TF+SID=abc123,IDX=1,TOTAL=10,OFFSET=0,SIZE=2048,CRC=7f6a3c1d,DATA=BASE64...
```

这几个字段分别是什么意思：

- `SID`：这次传输的会话 ID
- `IDX`：当前是第几片
- `TOTAL`：总共有多少片
- `OFFSET`：这片数据在原文件里的起始位置
- `SIZE`：这片实际字节数
- `CRC`：这片自己的 CRC32 校验值
- `DATA`：真正的数据内容，做了 Base64 编码

### 第三步：接收端组包

接收端用 `TransferReceiver` 一片一片收。

它会做这些检查：

- 会话 ID 对不对
- 片序号对不对
- 偏移对不对
- CRC 对不对
- 总片数对不对

全部收完以后，再把所有片按顺序拼回完整文件。

### 第四步：传输结束

组装成功以后，再回一条命令层消息：

- `r310104` -> `FILE_DONE`

如果失败：

- `r310105` -> `FILE_ERR+CODE=CRC_FAIL`

## 4. 代码示例

### 4.1 App 发送内存数据

```java
TransferMetadata metadata = new TransferMetadata.Builder()
        .setSessionId(TransferMetadata.createSessionId())
        .setDirection(TransferDirection.APP_TO_DEVICE)
        .setFileName("report.json")
        .setMediaType("application/json")
        .setTotalBytes(payload.length)
        .setChunkSize(2048)
        .setMd5(TransferChecksums.md5Hex(payload))
        .build();

TransferSender sender = new TransferSender();
sender.sendBytes(metadata, payload, frame -> {
    tcpServerService.sendMessageToClient(clientId, frame);
});
```

这是什么意思：

- 先定义这次要传什么
- 再让 `TransferSender` 自动拆片
- 每片拆好后发给 TCP 层

### 4.2 App 发送文件

```java
File file = new File(path);
TransferMetadata metadata = TransferMetadata.fromFile(
        TransferMetadata.createSessionId(),
        TransferDirection.APP_TO_DEVICE,
        file,
        "application/octet-stream",
        4096
);

TransferSender sender = new TransferSender();
sender.sendFile(metadata, file, frame -> {
    tcpServerService.sendMessageToClient(clientId, frame);
}, progress -> {
    DLog.i("TransferDemo", "percent=" + progress.getPercent());
});
```

这是什么意思：

- `fromFile` 自动把文件大小、MD5 算好
- 每发一片，就会回调一次进度

### 4.3 接收端收片并组装

```java
TransferReceiver receiver = new TransferReceiver();
receiver.start(metadataFromBeginCommand);

TransferProgress progress = receiver.acceptFrame(rawTransferFrame);
if (progress.isCompleted()) {
    byte[] payload = receiver.buildPayload();
}
```

这是什么意思：

- 先根据握手阶段拿到的元信息 `start`
- 每来一条分片帧就 `acceptFrame`
- 收满以后 `buildPayload`

## 5. 它适合什么，不适合什么

### 适合

- 报告模板文件
- 日志包
- 配置文件
- 中小型升级包
- 一次性的大文本数据

### 目前不太适合

- 超大文件断点续传
- 高并发多会话同时重传
- 二进制高速流
- 图片/视频实时连续传输

这些后面可以继续扩：

- ACK 帧
- 重传机制
- 断点续传
- 文件落盘型接收器
- 二进制帧编解码器

## 6. 它和普通通信场景的关系

最简单的理解：

- 开灯、关灯、开始、停止：普通命令
- 设置几个参数：普通命令
- 查询状态、返回结果：普通命令
- 发一个完整文件：`command + transfer`
- 连续不断的实时流：`command + stream`

## 7. 现阶段推荐你怎么用

你当前项目还在搭架构，最推荐的做法是：

1. 先把所有小指令都继续放在 `command` 编码表里
2. 文件类场景统一走 `FILE_BEGIN / FILE_READY / FILE_DONE / FILE_ERR`
3. 真正的数据块统一交给 `transfer`
4. 实时流先别塞进这套文件传输里，后面单独做 `stream`

这样后面协议扩展时最稳，不容易把小指令、文件传输、实时流搅在一起。

## 8. 建议配合统一 ACK 使用

如果你希望文件传输类场景的成功 / 失败也能跟普通命令保持一致，建议再配合：

- `lib.command.ack`

例如：

- 模块准备好时回：`ACK+TYPE=TRANSFER,REF=s310101,SESSION=abc123,TS=...,MSG=ready`
- 传输失败时回：`ERR+TYPE=TRANSFER,REF=s310101,SESSION=abc123,TS=...,CODE=CRC_FAIL,MSG=chunk_3_crc_fail`

这样 App 侧能统一处理回执，不需要单独再写一套文件传输错误解析。
