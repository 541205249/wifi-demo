# 统一协议网关说明

## 1. 这层是干什么的

这层 `gateway` 是把下面四层真正串起来的桥：

- `command`
- `ack`
- `transfer`
- `stream`

它不替代 TCP，不负责 socket 建连、设备管理、历史归档。

它只负责两件事：

1. 发送时，帮你把“按编码发送命令 / 发送统一 ACK / 发送 transfer / 发送 stream”统一成一套入口
2. 接收时，帮你把原始字符串识别成：
   - 普通命令
   - ACK / ERR
   - transfer 分片
   - stream 帧
   - unknown / invalid

## 2. 为什么要单独做这一层

如果没有网关，`TcpServerService` 里拿到的永远只是原始字符串：

- `INFO+HC25,FW=1.0.0`
- `ACK+TYPE=CMD,REF=s120101,...`
- `TF+SID=...`
- `SF+SID=...`

然后每个页面、每个业务入口都要自己判断：

- 这到底是命令还是 ACK
- 这是 transfer 还是 stream
- 这条消息该给谁处理

现在有了网关以后，服务层只要把原始消息交给它，它就会先帮你分类。

## 3. 当前提供了什么

代码位置：

- `ProtocolGateway`
- `ProtocolInboundEvent`
- `ProtocolPayloadType`
- `ProtocolMessageTransport`

## 4. 接收链路怎么走

最推荐的接入方式是：

1. TCP 服务收到原始字符串
2. 交给 `ProtocolGateway.resolveInbound(raw)`
3. 拿到 `ProtocolInboundEvent`
4. 再按类型分发给业务层

例如：

- `ACK+...` -> `ACK`
- `TF+...` -> `TRANSFER`
- `SF+...` -> `STREAM`
- 能匹配编码表 -> `COMMAND`
- 都不是 -> `UNKNOWN`

## 5. 发送链路怎么走

最推荐的接入方式是：

- 业务层不要自己手拼字符串
- 尽量走网关的发送入口

例如：

- 发送普通命令：`sendCommand(...)`
- 发送统一 ACK：`sendAck(...)`
- 发送文件传输：`sendTransferBytes(...) / sendTransferFile(...)`
- 发送实时流：`createStreamSender(...) + sendStreamPayload(...) + finishStream(...)`

## 6. 它和 TcpServerService 的关系

你可以把两层关系理解成：

- `TcpServerService`：负责网络连接、设备在线、原始收发、历史归档
- `ProtocolGateway`：负责协议识别、编码发送、结构化分发

这样通信底座就被拆成了：

- 连接层
- 协议层
- 业务层

后面你做业务时会更稳。

## 7. 当前项目已经怎么接了

这套网关现在已经接到：

- `app` 的 `TcpServerService`
- `demo` 的 `TcpServerService`

当前接入效果是：

1. TCP 服务收到模块原始字符串后，会先走 `ProtocolGateway.resolveInbound(...)`
2. 然后在原有 `onMessageReceived(raw)` 回调之外，再额外分发协议级事件
3. 命令设置页发送命令时，也不再自己拼 raw，而是改成走服务里的“按编码发送”入口

也就是说，当前工程已经不是“页面自己拼字符串 -> 服务直接发”这条单线了，而是：

- 页面按编码发
- 服务交给网关
- 网关解析编码 / ACK / transfer / stream
- 服务继续负责 TCP 收发和历史归档
