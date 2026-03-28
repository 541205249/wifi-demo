# ACK / 错误码统一模型说明

## 1. 这套模型是干什么的

这套 `ack` 基础层是给“模块如何统一回执 App”准备的。

它解决的是下面这些问题：

- 普通命令有自己的确认格式
- 文件传输也有自己的成功失败格式
- 实时流又可能有另一套错误格式
- App 侧就会出现三套不同解析逻辑

这套模型的目标是：

- 不管是普通命令、文件传输还是实时流
- 统一都能回成 `ACK+...` 或 `ERR+...`
- App 只需要一个解析器，就能知道：
  - 成功还是失败
  - 这是哪一类通道的回执
  - 对应的是哪一个请求
  - 会话号是什么
  - 错误码是什么

## 2. 推荐格式

成功回执：

```text
ACK+TYPE=CMD,REF=s120101,TS=1711620000000,MSG=accepted
```

失败回执：

```text
ERR+TYPE=TRANSFER,REF=s310101,SESSION=abc123,TS=1711620002000,CODE=CRC_FAIL,MSG=chunk_3_crc_fail
```

实时流成功回执：

```text
ACK+TYPE=STREAM,REF=s330101,SESSION=wave001,TS=1711620003000,MSG=stream_started
```

## 3. 字段说明

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `ACK+ / ERR+` | 是 | 前缀，表示成功或失败 |
| `TYPE` | 是 | 通道类型，当前支持 `CMD / TRANSFER / STREAM / GENERIC` |
| `REF` | 是 | 对应哪个请求，通常写请求编码或业务编号 |
| `SESSION` | 否 | 会话号，文件传输和实时流建议带 |
| `TS` | 是 | 时间戳，毫秒 |
| `CODE` | 失败时必填 | 错误码 |
| `MSG` | 否 | 补充说明 |
| `EXT_xxx` | 否 | 扩展字段 |

## 4. 推荐错误码

当前默认给了这些通用错误码：

- `INVALID_PARAM`
- `UNSUPPORTED`
- `BUSY`
- `TIMEOUT`
- `NOT_READY`
- `CRC_FAIL`
- `SESSION_MISMATCH`
- `FRAME_DROPPED`
- `INTERRUPTED`
- `INTERNAL_ERROR`

建议做法：

- 错误码尽量稳定、短小、可枚举
- `MSG` 用来补充上下文，不要让业务判断依赖 `MSG`

## 5. 三类通道怎么用

### 5.1 普通命令

例如开始验光：

- App 发：`s120101`
- 模块回成功：`ACK+TYPE=CMD,REF=s120101,TS=...,MSG=accepted`
- 模块回失败：`ERR+TYPE=CMD,REF=s120101,TS=...,CODE=BUSY,MSG=device_busy`

### 5.2 文件传输

例如模板文件上传：

- App 发：`s310101 -> FILE_BEGIN`
- 模块准备好后回：`ACK+TYPE=TRANSFER,REF=s310101,SESSION=abc123,TS=...,MSG=ready`
- 中途校验失败回：`ERR+TYPE=TRANSFER,REF=s310101,SESSION=abc123,TS=...,CODE=CRC_FAIL,MSG=chunk_3_crc_fail`

### 5.3 实时流

例如波形流开始：

- App 发：`s330101 -> STREAM_START`
- 模块回：`ACK+TYPE=STREAM,REF=s330101,SESSION=wave001,TS=...,MSG=stream_started`
- 流中断回：`ERR+TYPE=STREAM,REF=s330101,SESSION=wave001,TS=...,CODE=INTERRUPTED,MSG=wifi_jitter`

## 6. 代码入口

当前代码位置：

- `AckMessage`
- `AckCodec`
- `AckFactory`
- `AckErrorCodes`

最常见的用法是：

```java
AckCodec codec = new AckCodec();

AckMessage ack = AckFactory.successForCommand("s120101", "accepted");
String raw = codec.encode(ack);

AckMessage parsed = codec.decode(raw);
```

## 7. 现阶段推荐你怎么用

你当前项目还在搭架构，最推荐的做法是：

1. 先把业务编码表照常维护
2. 模块端如果要回统一成功/失败，就走 `ack`
3. 普通命令、文件传输、实时流都尽量复用这套回执格式
4. 如果某类场景必须保留原始老回执，也可以先并存，App 侧逐步迁移

这样后面联调时，定位问题会轻松很多。
