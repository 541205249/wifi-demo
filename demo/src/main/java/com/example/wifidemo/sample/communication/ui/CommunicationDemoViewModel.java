package com.example.wifidemo.sample.communication.ui;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.wifidemo.sample.communication.model.CommunicationDemoUiState;
import com.wifi.lib.command.ack.AckCodec;
import com.wifi.lib.command.ack.AckErrorCodes;
import com.wifi.lib.command.ack.AckFactory;
import com.wifi.lib.command.ack.AckMessage;
import com.wifi.lib.command.dispatcher.ProtocolDispatchResult;
import com.wifi.lib.command.dispatcher.ProtocolDispatcher;
import com.wifi.lib.command.gateway.ProtocolGateway;
import com.wifi.lib.command.gateway.ProtocolInboundEvent;
import com.wifi.lib.command.profile.OptometryCommandCodes;
import com.wifi.lib.command.profile.OptometryCommandProfile;
import com.wifi.lib.command.stream.StreamDirection;
import com.wifi.lib.command.stream.StreamFrame;
import com.wifi.lib.command.stream.StreamMetadata;
import com.wifi.lib.command.stream.StreamPacketCodec;
import com.wifi.lib.command.stream.StreamReceiver;
import com.wifi.lib.command.stream.StreamSender;
import com.wifi.lib.command.stream.StreamStats;
import com.wifi.lib.command.transfer.TransferChecksums;
import com.wifi.lib.command.transfer.TransferDirection;
import com.wifi.lib.command.transfer.TransferMetadata;
import com.wifi.lib.command.transfer.TransferProgress;
import com.wifi.lib.command.transfer.TransferReceiver;
import com.wifi.lib.command.transfer.TransferSender;
import com.wifi.lib.log.DLog;
import com.wifi.lib.mvvm.BaseViewModel;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommunicationDemoViewModel extends BaseViewModel {
    private static final String TAG = "CommunicationDemoVM";
    private static final int MAX_CONSOLE_LINE_COUNT = 60;

    private final MutableLiveData<CommunicationDemoUiState> uiStateLiveData = new MutableLiveData<>();
    private final ArrayDeque<String> consoleLines = new ArrayDeque<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final ProtocolGateway protocolGateway;

    public CommunicationDemoViewModel(@NonNull Application application) {
        super(application);
        protocolGateway = new ProtocolGateway(application, OptometryCommandProfile.getInstance());
        showActionControl();
    }

    public LiveData<CommunicationDemoUiState> getUiStateLiveData() {
        return uiStateLiveData;
    }

    public void showActionControl() {
        DLog.i(TAG, "切换通信场景示例: 单动作控制");
        renderPresetScenario(
                "当前示例：单动作控制",
                "单动作控制",
                "意思是 App 只想让模块做一件非常明确的事，通常一个按钮对应一个动作。最典型的就是开始、停止、开灯、切模式。消息很短，重点不是传很多数据，而是“做或不做”。",
                "业务例子：用户点击“开始验光”\n"
                        + "发送编码：s120101\n"
                        + "发送指令：START+OPT\n"
                        + "模块确认：r120102 -> ACK+START",
                "适合什么：一键动作、开关、开始/停止\n"
                        + "开发重点：请求和确认最好拆成两个编码\n"
                        + "页面上会发生什么：按钮点下后先发命令，收到确认后再切 UI 状态",
                console -> {
                    console.accept("用户点击“开始验光”按钮");
                    console.accept("App 通过编码 s120101 找到真实指令 START+OPT");
                    console.accept("通过 TCP 把 START+OPT 发给模块");
                    console.accept("模块回 r120102 -> ACK+START");
                    console.accept("App 把界面切到“验光中”");
                }
        );
    }

    public void showFieldSetting() {
        DLog.i(TAG, "切换通信场景示例: 带字段设置");
        renderPresetScenario(
                "当前示例：带字段设置",
                "带字段设置",
                "意思是这次不是让模块做一个简单动作，而是要把几个参数一起传过去。比如验光参数、阈值、模式、时间等。这里的重点是字段和值，而不是动作本身。",
                "业务例子：App 设置验光参数\n"
                        + "发送编码：s220101\n"
                        + "发送指令：SET+REF=SPH:-1.25,CYL:-0.50,AXIS:180\n"
                        + "模块确认：r220102 -> AT+REF=OK",
                "适合什么：设置参数、提交配置、保存一次业务参数\n"
                        + "开发重点：字段顺序尽量固定，占位符统一\n"
                        + "页面上会发生什么：用户填参数，App 拼好一条消息发给模块，等模块确认配置成功",
                console -> {
                    console.accept("用户在页面上填写球镜、柱镜、轴位");
                    console.accept("App 把表单值填进 s220101 对应模板");
                    console.accept("形成真实指令 SET+REF=SPH:-1.25,CYL:-0.50,AXIS:180");
                    console.accept("模块写入参数后回 AT+REF=OK");
                    console.accept("App 提示“参数已同步到设备”");
                }
        );
    }

    public void showQueryResponse() {
        DLog.i(TAG, "切换通信场景示例: 查询与应答");
        renderPresetScenario(
                "当前示例：查询与应答",
                "查询与应答",
                "意思是 App 不知道模块当前状态，所以先问一句，模块再回一句。这类场景常用于查版本、查模式、查设备信息、查当前任务状态。",
                "业务例子：查询模块版本\n"
                        + "发送编码：s230101\n"
                        + "发送指令：AT+VER?\n"
                        + "接收编码：r230102\n"
                        + "返回指令：VER+1.0.8",
                "适合什么：查版本、查模式、查设备能力\n"
                        + "开发重点：查询编码和返回编码分开；返回内容不固定时用前缀或正则匹配\n"
                        + "页面上会发生什么：进入页面或点击刷新时发查询，拿到结果后更新显示",
                console -> {
                    console.accept("页面进入“设备信息”页，自动发起版本查询");
                    console.accept("App 按 s230101 发出 AT+VER?");
                    console.accept("模块返回 VER+1.0.8");
                    console.accept("接收侧按 r230102 匹配并解析版本号");
                    console.accept("页面显示当前模块固件版本 1.0.8");
                }
        );
    }

    public void showStatusReport() {
        DLog.i(TAG, "切换通信场景示例: 状态上报");
        renderPresetScenario(
                "当前示例：状态上报",
                "状态上报",
                "意思是不用 App 每次都去问，模块自己主动把当前状态推给 App。适合“状态会变化，而且 App 需要及时知道”的场景，比如空闲、运行中、故障、缺纸、过热。",
                "业务例子：设备主动推送状态\n"
                        + "接收编码：r240101\n"
                        + "接收指令：STATUS+READY\n"
                        + "或者：STATUS+BUSY / STATUS+PAUSE / STATUS+ERROR",
                "适合什么：设备状态、告警状态、运行阶段变化\n"
                        + "开发重点：状态和告警最好分开；App 只负责展示和联动，不要把连接层逻辑写进页面\n"
                        + "页面上会发生什么：设备状态一变化，App 不用刷新就能立刻改 UI",
                console -> {
                    console.accept("模块空闲时主动上报 STATUS+READY");
                    console.accept("App 收到后把设备状态灯改成绿色");
                    console.accept("开始执行任务后，模块继续上报 STATUS+BUSY");
                    console.accept("App 把“开始按钮”置灰，把“停止按钮”点亮");
                    console.accept("如果模块异常，可再上报 ERROR+XXXX 让页面提示故障");
                }
        );
    }

    public void showResultReport() {
        DLog.i(TAG, "切换通信场景示例: 结果上报");
        renderPresetScenario(
                "当前示例：结果上报",
                "结果上报",
                "意思是模块已经把某项业务做完了，现在把结果主动回传给 App。它不是简单状态，也不是确认，而是业务真正关心的数据结果。",
                "业务例子：模块返回一次验光结果\n"
                        + "接收编码：r250101\n"
                        + "接收指令：RESULT+SPH=-1.25,CYL=-0.50,AXIS=180",
                "适合什么：验光结果、测量值、打印结果、最终报告摘要\n"
                        + "开发重点：结果字段固定时可用正则直接解析；字段过多时可考虑 JSON 文本\n"
                        + "页面上会发生什么：收到结果后更新报告卡片、保存数据库、允许用户确认",
                console -> {
                    console.accept("模块完成一次测量");
                    console.accept("模块主动发 RESULT+SPH=-1.25,CYL=-0.50,AXIS=180");
                    console.accept("App 根据 r250101 识别为“验光结果上报”");
                    console.accept("解析出球镜、柱镜、轴位并刷新报告区");
                    console.accept("同时可以把结果落库，供后续报告页查看");
                }
        );
    }

    public void runTransferExample() {
        DLog.i(TAG, "切换通信场景示例: 文件传输");
        clearConsole();
        try {
            String payloadText = buildTransferPayload();
            byte[] payload = payloadText.getBytes(StandardCharsets.UTF_8);
            TransferMetadata metadata = new TransferMetadata.Builder()
                    .setSessionId(TransferMetadata.createSessionId())
                    .setDirection(TransferDirection.APP_TO_DEVICE)
                    .setFileName("exam-template.json")
                    .setMediaType("application/json")
                    .setTotalBytes(payload.length)
                    .setChunkSize(32)
                    .setMd5(TransferChecksums.md5Hex(payload))
                    .putExtra("type", "template")
                    .build();

            String beginCommand = "s310101 -> FILE_BEGIN+TYPE=template,NAME="
                    + metadata.getFileName()
                    + ",SIZE=" + metadata.getTotalBytes()
                    + ",MD5=" + metadata.getMd5();
            String readyCommand = "r310102 -> FILE_READY+SESSION="
                    + metadata.getSessionId()
                    + ",CHUNK=" + metadata.getChunkSize();

            appendConsole("App 先发握手命令，通知模块准备收文件");
            appendConsole(beginCommand);
            appendConsole("模块确认可接收，并分配本次 sessionId");
            appendConsole(readyCommand);

            List<String> frames = new ArrayList<>();
            TransferSender sender = new TransferSender();
            TransferProgress sendProgress = sender.sendBytes(
                    metadata,
                    payload,
                    frame -> {
                        frames.add(frame);
                        appendConsole("发送分片 " + frames.size() + "/" + metadata.getTotalChunks()
                                + "，帧长=" + frame.length());
                    },
                    progress -> appendConsole("发送进度 "
                            + progress.getPercent()
                            + "%（" + progress.getTransferredChunks()
                            + "/" + progress.getTotalChunks() + "）")
            );

            TransferReceiver receiver = new TransferReceiver();
            receiver.start(metadata);
            TransferProgress receiveProgress = null;
            for (String frame : frames) {
                receiveProgress = receiver.acceptFrame(frame);
                appendConsole("接收端确认分片 "
                        + receiveProgress.getTransferredChunks()
                        + "/" + receiveProgress.getTotalChunks()
                        + "，累计 " + receiveProgress.getPercent() + "%");
            }

            byte[] rebuiltPayload = receiver.buildPayload();
            String rebuiltText = new String(rebuiltPayload, StandardCharsets.UTF_8);
            appendConsole("所有分片组包完成，MD5 校验通过");
            appendConsole("模块回复 r310104 -> FILE_DONE");

            String exampleText = "业务例子：App 给模块发送一份验光流程模板文件\n"
                    + "握手开始：\n" + beginCommand + "\n"
                    + "模块就绪：\n" + readyCommand + "\n"
                    + "数据帧格式：\n"
                    + "TF+SID=<sessionId>,IDX=<index>,TOTAL=<total>,OFFSET=<offset>,SIZE=<size>,CRC=<crc32>,DATA=<base64>";
            String flowText = "适合什么：模板文件、日志包、配置文件、升级包\n"
                    + "开发重点：小命令走 command，大内容走 command + transfer\n"
                    + "页面上会发生什么：先握手，再分片发送，模块按片接收，全部收完后再回 FILE_DONE";
            String statusText = "文件传输示例完成：发送 "
                    + sendProgress.getTransferredChunks() + "/" + sendProgress.getTotalChunks()
                    + " 片，接收 "
                    + (receiveProgress == null ? 0 : receiveProgress.getTransferredChunks())
                    + "/" + (receiveProgress == null ? metadata.getTotalChunks() : receiveProgress.getTotalChunks())
                    + " 片";
            updateScenarioState(
                    statusText,
                    "文件传输",
                    "意思是这次数据太大，已经不适合塞进一条普通命令，所以先通过命令层做握手，再把真正的大内容拆成很多片逐条发过去。接收端收到后再按顺序组装、校验。",
                    exampleText,
                    flowText + "\n\n实际分片示例：\n" + joinFrames(frames),
                    buildTransferConsole(sendProgress, receiveProgress, rebuiltText),
                    true
            );
        } catch (Exception exception) {
            renderFailureScenario(
                    "文件传输示例失败",
                    "文件传输",
                    "文件传输适合大内容场景，需要命令握手和分片传输配合。",
                    "本次示例执行失败，请检查 transfer 基础层日志。",
                    "文件传输示例",
                    exception
            );
        }
    }

    public void runStreamExample() {
        DLog.i(TAG, "切换通信场景示例: 实时流");
        clearConsole();
        try {
            StreamMetadata metadata = new StreamMetadata.Builder()
                    .setSessionId(StreamMetadata.createSessionId())
                    .setDirection(StreamDirection.DEVICE_TO_APP)
                    .setStreamType("waveform")
                    .setSampleRateHz(50)
                    .setFrameSize(32)
                    .putExtra("scene", "optometry")
                    .build();

            String startCommand = "s330101 -> STREAM_START+TYPE=waveform,RATE=" + metadata.getSampleRateHz();
            String readyCommand = "r330102 -> STREAM_START=OK+SESSION=" + metadata.getSessionId();

            appendConsole("App 先通过命令层请求模块开始推实时流");
            appendConsole(startCommand);
            appendConsole("模块确认开始推流，并返回本次流会话的 sessionId");
            appendConsole(readyCommand);

            List<String> rawFrames = new ArrayList<>();
            List<String> payloads = buildStreamPayloads();
            StreamSender sender = new StreamSender();
            sender.start(metadata);
            for (String payloadText : payloads) {
                sender.sendPayload(
                        payloadText.getBytes(StandardCharsets.UTF_8),
                        frame -> rawFrames.add(frame),
                        stats -> appendConsole("发送流帧 seq=" + stats.getLastSequence()
                                + "，累计帧数=" + stats.getTotalFrames()
                                + "，累计字节=" + stats.getTotalBytes())
                );
            }
            StreamStats sendStats = sender.finish(
                    frame -> rawFrames.add(frame),
                    stats -> appendConsole("发送结束帧，发送端状态=" + stats.getState())
            );

            StreamPacketCodec codec = new StreamPacketCodec();
            StreamReceiver receiver = new StreamReceiver();
            receiver.start(metadata);
            StreamStats receiveStats = receiver.snapshot();
            for (String rawFrame : rawFrames) {
                StreamFrame frame = codec.decodeFrame(rawFrame);
                receiveStats = receiver.acceptFrame(frame);
                if (frame.isEndOfStream()) {
                    appendConsole("接收端收到结束帧，流状态=" + receiveStats.getState());
                } else {
                    appendConsole("接收流帧 seq=" + frame.getSequence()
                            + " -> " + new String(frame.getPayload(), StandardCharsets.UTF_8));
                }
            }

            String exampleText = "业务例子：模块持续上报验光过程中的实时波形\n"
                    + "开始推流：\n" + startCommand + "\n"
                    + "模块确认：\n" + readyCommand + "\n"
                    + "实时帧格式：\n"
                    + "SF+SID=<sessionId>,SEQ=<sequence>,TS=<timestamp>,SIZE=<size>,EOS=<0|1>,CRC=<crc32>,DATA=<base64>";
            String flowText = "适合什么：实时波形、连续测量值、传感器流数据\n"
                    + "开发重点：开始/停止走 command，每一帧数据走 stream；允许统计丢帧但继续处理\n"
                    + "页面上会发生什么：模块一边推流，App 一边按帧解码、刷新曲线，并统计接收质量";
            String statusText = "实时流示例完成：有效帧 "
                    + receiveStats.getTotalFrames()
                    + " 帧，累计 " + receiveStats.getTotalBytes()
                    + " 字节，丢帧 " + receiveStats.getDroppedFrames() + " 帧";
            updateScenarioState(
                    statusText,
                    "实时流",
                    "意思是模块不是回一条结果就结束，而是持续不断地一帧一帧往 App 推数据。App 收到后不是攒到最后再处理，而是来一帧处理一帧、刷新一帧。",
                    exampleText,
                    flowText + "\n\n实际流帧示例：\n" + joinRawFrames(rawFrames),
                    buildStreamConsole(sendStats, receiveStats),
                    true
            );
        } catch (Exception exception) {
            renderFailureScenario(
                    "实时流示例失败",
                    "实时流",
                    "实时流适合高频连续数据，需要把控制命令和实时帧分层。",
                    "本次示例执行失败，请检查 stream 基础层日志。",
                    "实时流示例",
                    exception
            );
        }
    }

    public void showAckModel() {
        DLog.i(TAG, "切换通信场景示例: 统一ACK/错误码");
        clearConsole();
        try {
            AckCodec codec = new AckCodec();
            AckMessage commandAck = AckFactory.successForCommand("s120101", "accepted");
            AckMessage transferErr = AckFactory.base(
                            com.wifi.lib.command.ack.AckStatus.FAILURE,
                            com.wifi.lib.command.ack.AckChannel.TRANSFER,
                            "s310101",
                            "file001",
                            AckErrorCodes.CRC_FAIL,
                            "chunk_3_crc_fail"
                    )
                    .putExtra("chunk", "3")
                    .putExtra("stage", "upload")
                    .build();
            AckMessage streamAck = AckFactory.base(
                            com.wifi.lib.command.ack.AckStatus.SUCCESS,
                            com.wifi.lib.command.ack.AckChannel.STREAM,
                            "s330101",
                            "wave001",
                            null,
                            "stream_started"
                    )
                    .putExtra("rate", "50")
                    .putExtra("type", "waveform")
                    .build();

            String rawCommandAck = codec.encode(commandAck);
            String rawTransferErr = codec.encode(transferErr);
            String rawStreamAck = codec.encode(streamAck);

            appendConsole("模块回普通命令成功回执");
            appendConsole(rawCommandAck);
            appendConsole("模块回文件传输失败回执");
            appendConsole(rawTransferErr);
            appendConsole("模块回实时流启动成功回执");
            appendConsole(rawStreamAck);

            AckMessage parsedCommandAck = codec.decode(rawCommandAck);
            AckMessage parsedTransferErr = codec.decode(rawTransferErr);
            AckMessage parsedStreamAck = codec.decode(rawStreamAck);

            appendConsole("统一解析后：CMD -> status=" + parsedCommandAck.getStatus()
                    + ", ref=" + parsedCommandAck.getReference());
            appendConsole("统一解析后：TRANSFER -> status=" + parsedTransferErr.getStatus()
                    + ", errorCode=" + parsedTransferErr.getErrorCode()
                    + ", session=" + parsedTransferErr.getSessionId());
            appendConsole("统一解析后：STREAM -> status=" + parsedStreamAck.getStatus()
                    + ", session=" + parsedStreamAck.getSessionId()
                    + ", extras=" + parsedStreamAck.getExtras().size());

            String exampleText = "普通命令成功：\n" + rawCommandAck
                    + "\n\n文件传输失败：\n" + rawTransferErr
                    + "\n\n实时流成功：\n" + rawStreamAck;
            String flowText = "适合什么：统一普通命令、文件传输、实时流的回执格式\n"
                    + "开发重点：模块端统一返回 ACK+/ERR+；App 端统一用 AckCodec 解析\n"
                    + "页面上会发生什么：收到任何成功/失败回执，都能先按同一模型拿到 status、type、ref、session、errorCode";
            String statusText = "统一回执示例完成：1 条命令 ACK，1 条传输 ERR，1 条流 ACK";
            updateScenarioState(
                    statusText,
                    "统一 ACK / 错误码",
                    "意思是不管模块回应的是普通命令、文件传输还是实时流，都尽量回成同一种固定结构。这样 App 不需要为三套场景各写一套成功失败解析器。",
                    exampleText,
                    flowText,
                    buildAckConsole(parsedCommandAck, parsedTransferErr, parsedStreamAck),
                    true
            );
        } catch (Exception exception) {
            renderFailureScenario(
                    "统一回执示例失败",
                    "统一 ACK / 错误码",
                    "统一回执适合给 command、transfer、stream 共用。",
                    "本次示例执行失败，请检查 ack 基础层日志。",
                    "统一回执示例",
                    exception
            );
        }
    }

    public void runDispatcherExample() {
        DLog.i(TAG, "切换通信场景示例: 业务分发");
        clearConsole();
        try {
            ProtocolDispatcher dispatcher = new ProtocolDispatcher();

            dispatcher.registerCommandUseCase(OptometryCommandCodes.CODE_REPORT_OPTOMETRY_RESULT, context -> {
                java.util.List<String> groups = context.getInboundCommand().getRegexGroups();
                appendConsole("业务层命中验光结果处理器，code=" + context.getCode()
                        + "，SPH=" + safeGroup(groups, 0)
                        + "，CYL=" + safeGroup(groups, 1)
                        + "，AXIS=" + safeGroup(groups, 2));
            });
            dispatcher.registerAckUseCase(
                    com.wifi.lib.command.ack.AckChannel.COMMAND,
                    OptometryCommandCodes.CODE_START_OPTOMETRY,
                    context -> appendConsole("业务层命中开始验光 ACK，status="
                            + context.getAckMessage().getStatus()
                            + "，message=" + context.getAckMessage().getMessage())
            );

            String transferPayloadText = buildTransferPayload();
            byte[] transferPayload = transferPayloadText.getBytes(StandardCharsets.UTF_8);
            TransferMetadata transferMetadata = new TransferMetadata.Builder()
                    .setSessionId("filedemo001")
                    .setDirection(TransferDirection.DEVICE_TO_APP)
                    .setFileName("optometry-template.json")
                    .setMediaType("application/json")
                    .setTotalBytes(transferPayload.length)
                    .setChunkSize(40)
                    .setMd5(TransferChecksums.md5Hex(transferPayload))
                    .putExtra("type", "template")
                    .build();
            dispatcher.registerTransferSession(transferMetadata, context -> {
                appendConsole("业务层命中文件传输处理器，session=" + context.getMetadata().getSessionId()
                        + "，进度=" + context.getProgress().getPercent() + "%");
                if (context.hasCompletedPayload()) {
                    appendConsole("文件已组包完成，内容预览="
                            + new String(context.getCompletedPayload(), StandardCharsets.UTF_8));
                }
            });

            StreamMetadata streamMetadata = new StreamMetadata.Builder()
                    .setSessionId("wave001")
                    .setDirection(StreamDirection.DEVICE_TO_APP)
                    .setStreamType("waveform")
                    .setSampleRateHz(50)
                    .setFrameSize(32)
                    .putExtra("scene", "dispatcher-demo")
                    .build();
            dispatcher.registerStreamSession(streamMetadata, context -> {
                if (context.getStreamFrame().isEndOfStream()) {
                    appendConsole("业务层命中波形流结束处理器，session=" + context.getMetadata().getSessionId()
                            + "，有效帧=" + context.getStats().getTotalFrames()
                            + "，丢帧=" + context.getStats().getDroppedFrames());
                    return;
                }
                appendConsole("业务层命中波形流处理器，seq=" + context.getStreamFrame().getSequence()
                        + " -> " + new String(context.getStreamFrame().getPayload(), StandardCharsets.UTF_8));
            });

            dispatcher.setUnknownUseCase(
                    context -> appendConsole("unknown 分支收到原始消息: " + context.getRawMessage()));
            dispatcher.setInvalidUseCase(
                    context -> appendConsole("invalid 分支收到坏包，error=" + context.getEvent().getErrorMessage()));

            appendConsole("步骤 1：模拟模块上报一次验光结果");
            dispatchDemoEvent(dispatcher, "device-001",
                    protocolGateway.resolveInbound("RESULT+SPH=-1.25,CYL=-0.50,AXIS=180"));

            appendConsole("步骤 2：模拟模块返回开始验光 ACK");
            AckCodec ackCodec = new AckCodec();
            String ackRaw = ackCodec.encode(
                    AckFactory.successForCommand(OptometryCommandCodes.CODE_START_OPTOMETRY, "exam_started")
            );
            dispatchDemoEvent(dispatcher, "device-001", protocolGateway.resolveInbound(ackRaw));

            appendConsole("步骤 3：模拟模块推送模板文件分片");
            java.util.List<String> transferFrames = new ArrayList<>();
            TransferSender transferSender = new TransferSender();
            transferSender.sendBytes(transferMetadata, transferPayload, transferFrames::add, null);
            for (String frame : transferFrames) {
                dispatchDemoEvent(dispatcher, "device-001", protocolGateway.resolveInbound(frame));
            }

            appendConsole("步骤 4：模拟模块推送实时波形流");
            java.util.List<String> streamFrames = new ArrayList<>();
            StreamSender streamSender = new StreamSender();
            streamSender.start(streamMetadata);
            for (String payloadText : buildStreamPayloads()) {
                streamSender.sendPayload(payloadText.getBytes(StandardCharsets.UTF_8), streamFrames::add, null);
            }
            streamSender.finish(streamFrames::add, null);
            for (String frame : streamFrames) {
                dispatchDemoEvent(dispatcher, "device-001", protocolGateway.resolveInbound(frame));
            }

            appendConsole("步骤 5：模拟一条编码表外的未知消息");
            dispatchDemoEvent(dispatcher, "device-001", protocolGateway.resolveInbound("AT+UNKNOWN=1"));

            String exampleText = "示例 1：RESULT+SPH=-1.25,CYL=-0.50,AXIS=180 -> 命中 " + OptometryCommandCodes.CODE_REPORT_OPTOMETRY_RESULT
                    + "\n示例 2：ACK for " + OptometryCommandCodes.CODE_START_OPTOMETRY + " -> 命中开始验光 ACK 处理器"
                    + "\n示例 3：TF+SID=filedemo001 -> 命中文件传输会话"
                    + "\n示例 4：SF+SID=wave001 -> 命中波形流会话";
            String flowText = "这个场景演示的不是“如何发消息”，而是“消息进来后怎么自动路由到业务层”。\n"
                    + "开发重点：gateway 先识别协议类型，dispatcher 再按编码 / ACK 引用 / sessionId 做二次分发。\n"
                    + "真正写业务时，ViewModel 不需要直接判断原始字符串，只要注册 UseCase 即可。";
            String statusText = "业务分发示例完成：1 条 command，1 条 ACK，"
                    + transferMetadata.getTotalChunks() + " 片 transfer，"
                    + (buildStreamPayloads().size() + 1) + " 帧 stream";
            updateScenarioState(
                    statusText,
                    "业务分发 / 路由",
                    "意思是协议层已经把消息识别出来了，但业务层还需要知道这条消息到底该交给哪个处理器。dispatcher 的作用就是把这个路由动作从页面里抽走，统一放进 lib。",
                    exampleText,
                    flowText,
                    buildConsole(),
                    true
            );
        } catch (Exception exception) {
            renderFailureScenario(
                    "业务分发示例失败",
                    "业务分发 / 路由",
                    "业务分发示例负责演示消息命中不同处理器的过程。",
                    "本次示例执行失败，请检查 dispatcher / gateway 基础层日志。",
                    "业务分发示例",
                    exception
            );
        }
    }

    private void renderPresetScenario(
            @NonNull String statusText,
            @NonNull String title,
            @NonNull String meaningText,
            @NonNull String exampleText,
            @NonNull String flowText,
            @NonNull Consumer<Consumer<String>> consoleProducer
    ) {
        clearConsole();
        consoleProducer.accept(this::appendConsole);
        updateScenarioState(
                statusText,
                title,
                meaningText,
                exampleText,
                flowText,
                buildConsole(),
                true
        );
    }

    private void updateScenarioState(
            @NonNull String statusText,
            @NonNull String title,
            @NonNull String meaningText,
            @NonNull String exampleText,
            @NonNull String flowText,
            @NonNull String consoleText,
            boolean success
    ) {
        uiStateLiveData.setValue(new CommunicationDemoUiState(
                statusText,
                title,
                meaningText,
                exampleText,
                flowText,
                consoleText,
                success
        ));
    }

    private void renderFailureScenario(
            @NonNull String statusText,
            @NonNull String title,
            @NonNull String meaningText,
            @NonNull String exampleText,
            @NonNull String scenarioLabel,
            @NonNull Exception exception
    ) {
        String errorMessage = safeMessage(exception);
        appendConsole(scenarioLabel + "失败: " + errorMessage);
        DLog.e(TAG, "运行" + scenarioLabel + "失败", exception);
        updateScenarioState(
                statusText,
                title,
                meaningText,
                exampleText,
                errorMessage,
                buildConsole(),
                false
        );
        dispatchMessage(scenarioLabel + "失败: " + errorMessage);
    }

    @NonNull
    private String buildTransferPayload() {
        return "{\n"
                + "  \"template\":\"subjective_exam\",\n"
                + "  \"step\":\"cross-cylinder\",\n"
                + "  \"distance\":\"5m\",\n"
                + "  \"voice\":\"请比较一和二哪个更清楚\",\n"
                + "  \"retry\":2\n"
                + "}";
    }

    @NonNull
    private List<String> buildStreamPayloads() {
        return Arrays.asList(
                "WAVE+12,18,21,26",
                "WAVE+13,19,22,25",
                "WAVE+11,17,23,27",
                "WAVE+10,16,21,24"
        );
    }

    private void dispatchDemoEvent(
            @NonNull ProtocolDispatcher dispatcher,
            @NonNull String clientId,
            @NonNull ProtocolInboundEvent event
    ) {
        ProtocolDispatchResult result = dispatcher.dispatch(clientId, event);
        appendConsole("分发结果 -> type=" + event.getPayloadType()
                + "，status=" + result.getStatus()
                + "，route=" + result.getRouteKey());
        if (result.isAutoRemoved()) {
            appendConsole("会话已自动移除，route=" + result.getRouteKey());
        }
    }

    @NonNull
    private String joinFrames(@NonNull List<String> frames) {
        if (frames.isEmpty()) {
            return "无分片";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < frames.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append("第 ").append(index + 1).append(" 片：\n").append(frames.get(index));
        }
        return builder.toString();
    }

    @NonNull
    private String joinRawFrames(@NonNull List<String> frames) {
        if (frames.isEmpty()) {
            return "无流帧";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < frames.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append("第 ").append(index + 1).append(" 帧：\n").append(frames.get(index));
        }
        return builder.toString();
    }

    @NonNull
    private String buildTransferConsole(
            @NonNull TransferProgress sendProgress,
            TransferProgress receiveProgress,
            @NonNull String rebuiltText
    ) {
        StringBuilder builder = new StringBuilder(buildConsole());
        builder.append("\n\n")
                .append("发送完成度：")
                .append(sendProgress.getPercent())
                .append("%");
        if (receiveProgress != null) {
            builder.append("\n")
                    .append("接收完成度：")
                    .append(receiveProgress.getPercent())
                    .append("%");
        }
        builder.append("\n\n组包后的内容预览：\n").append(rebuiltText);
        return builder.toString();
    }

    @NonNull
    private String buildStreamConsole(@NonNull StreamStats sendStats, @NonNull StreamStats receiveStats) {
        StringBuilder builder = new StringBuilder(buildConsole());
        builder.append("\n\n发送端统计：")
                .append("\n有效帧数=").append(sendStats.getTotalFrames())
                .append("\n累计字节=").append(sendStats.getTotalBytes())
                .append("\n最后序号=").append(sendStats.getLastSequence())
                .append("\n状态=").append(sendStats.getState());
        builder.append("\n\n接收端统计：")
                .append("\n有效帧数=").append(receiveStats.getTotalFrames())
                .append("\n累计字节=").append(receiveStats.getTotalBytes())
                .append("\n最后序号=").append(receiveStats.getLastSequence())
                .append("\n丢帧数=").append(receiveStats.getDroppedFrames())
                .append("\n状态=").append(receiveStats.getState());
        return builder.toString();
    }

    @NonNull
    private String buildAckConsole(
            @NonNull AckMessage commandAck,
            @NonNull AckMessage transferErr,
            @NonNull AckMessage streamAck
    ) {
        StringBuilder builder = new StringBuilder(buildConsole());
        builder.append("\n\n解析后的统一字段预览：")
                .append("\nCMD -> status=").append(commandAck.getStatus())
                .append(", channel=").append(commandAck.getChannel())
                .append(", ref=").append(commandAck.getReference());
        builder.append("\nTRANSFER -> status=").append(transferErr.getStatus())
                .append(", channel=").append(transferErr.getChannel())
                .append(", ref=").append(transferErr.getReference())
                .append(", session=").append(transferErr.getSessionId())
                .append(", errorCode=").append(transferErr.getErrorCode());
        builder.append("\nSTREAM -> status=").append(streamAck.getStatus())
                .append(", channel=").append(streamAck.getChannel())
                .append(", ref=").append(streamAck.getReference())
                .append(", session=").append(streamAck.getSessionId())
                .append(", extras=").append(streamAck.getExtras());
        return builder.toString();
    }

    private void clearConsole() {
        consoleLines.clear();
    }

    private void appendConsole(@NonNull String message) {
        if (TextUtils.isEmpty(message)) {
            return;
        }
        consoleLines.addLast("[" + timeFormat.format(new Date()) + "] " + message);
        while (consoleLines.size() > MAX_CONSOLE_LINE_COUNT) {
            consoleLines.removeFirst();
        }
    }

    @NonNull
    private String buildConsole() {
        return TextUtils.join("\n", consoleLines);
    }

    @NonNull
    private String safeMessage(@NonNull Throwable throwable) {
        return TextUtils.isEmpty(throwable.getMessage()) ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    @NonNull
    private String safeGroup(@NonNull List<String> groups, int index) {
        if (index < 0 || index >= groups.size()) {
            return "";
        }
        return groups.get(index);
    }
}
