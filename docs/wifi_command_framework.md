# WiFi 模块指令编号框架说明

## 1. 目标

这套框架放在 `lib` 模块中，`app` 和 `demo` 共用同一套命令基础层，解决的是下面这件事：

- App 先按业务预留固定的 6 位编号。
- UI、业务流程、发送入口全部只依赖“编号”，不依赖真实命令字符串。
- 设备端开发人员后续只需要维护一份“编码表文档”，把编号和真实命令填上。
- App 在运行时加载这份文档，就能完成：
  - `编号 -> 真实发送命令`
  - `接收到的真实命令 -> 编号 -> App 业务处理`

这样做以后，App 可以先把流程和页面全部按编号打通，真实协议后补。

## 2. 编号规则

编号固定为 6 位数字，结构如下：

- 前 2 位：大模块
- 中 2 位：子模块
- 后 2 位：动作

例子：

- `100101`
- `110201`

建议长期保持：

- 编号一旦在 App 中使用，就不要随意改号。
- 新增功能只新增编号，不复用旧编号。
- 收和发建议都用固定编号，不要临时靠字符串硬判断。
- App 中所有已绑定编号的按钮，都建议支持长按显示编码。

## 3. 推荐文档格式

推荐使用 `CSV（UTF-8）`。

原因：

- 设备端开发人员可以直接用 Excel/WPS 打开和填写。
- Android 端读取最简单，依赖最少，稳定性高。
- 文本格式方便版本管理、比对差异和回溯历史。
- 后面如果要导入、导出、分享模板，也最容易做。

不推荐优先用 `xlsx` 作为第一版格式，因为：

- App 解析复杂度更高。
- 兼容性和依赖成本更高。
- 对你现在“先按编号跑流程”的目标没有额外收益。

## 4. 编码表字段

CSV 表头建议如下：

- `编码`
- `编号解释`
- `大模块`
- `子模块`
- `动作`
- `方向`
- `发送命令`
- `接收命令`
- `接收匹配方式`
- `描述`
- `示例`
- `启用`
- `备注`

字段说明：

- `编码`：6 位固定编号，例如 `100101`
- `编号解释`：直接说明这个编号代表什么业务动作，方便设备端同事对照填写命令
- `大模块/子模块/动作`：给人看的业务名称，方便维护
- `方向`：`发送` / `接收` / `双向`
- `发送命令`：App 发给模块时真正发送的命令模板
- `接收命令`：模块发给 App 时用于匹配的命令
- `接收匹配方式`：`精确` / `前缀` / `包含` / `正则`
- `启用`：是否生效，支持 `是/否`、`true/false`、`1/0`

### 关于“发送命令”和“接收命令”

一条编号可以只配置发送、只配置接收，或者双向都配置。

例如：

- 发送类动作：`方向=发送`，只填 `发送命令`
- 上报类动作：`方向=接收`，只填 `接收命令`
- 请求/回包复用同一业务编号时：`方向=双向`，两边都填

更推荐的做法是：

- 请求使用一个发送编号
- 确认回包再使用一个独立的接收编号

例如：

- `120102` 表示“停止验光请求”
- `120104` 表示“停止验光确认”

这样即使请求字符串和确认字符串暂时一样，App 侧也能把“我发出去的”和“模块确认收到后的回包”明确区分开，后面协议扩展成 `OK/ERR` 也更容易维护。

### 关于“接收匹配方式”

建议默认从简单到复杂：

- 能精确匹配就用 `精确`
- 有固定头部就用 `前缀`
- 确实存在动态参数时再用 `正则`

如果设备回包带动态值，例如：

`RSP+MODE=AUTO`

可以这样配置：

- `接收命令=^RSP\\+MODE=(.+)$`
- `接收匹配方式=正则`

解析成功后可以从 `regexGroups` 里拿捕获组。

## 5. 当前已经实现的 lib 框架

代码位置：

- `lib/src/main/java/com/wifi/lib/command`

核心类：

- `CommandCode`
  - 负责 6 位编码校验和分段解析
- `CommandReservation`
  - App 侧预留编码项，只关心编号和业务语义
- `CommandCatalog`
  - App 侧预留编码目录
- `CommandTableLoader`
  - 从手机里的 CSV 文档加载编码表
- `CommandTable`
  - 已加载完成的命令映射表
- `CommandDefinition`
  - 一行映射定义
- `CommandEngine`
  - 发送按编号解析、接收按命令反查、分发处理
- `CommandTableCsvExporter`
  - 根据 App 预留编码导出模板 CSV

## 6. 推荐接入方式

### 第一步：在 App 中固定预留编号

建议每个业务模块在 `lib` 中维护一份固定编号目录，例如当前的：

- `lib/src/main/java/com/wifi/lib/command/profile/OptometryCommandCodes.java`
- `lib/src/main/java/com/wifi/lib/command/profile/OptometryCommandCatalogs.java`
- `lib/src/main/java/com/wifi/lib/command/profile/OptometryCommandProfile.java`

其中：

- `OptometryCommandCodes` 只负责定义稳定业务编号
- `OptometryCommandCatalogs` 只负责把这些编号组装成预留目录

真实命令仍然来自 CSV。

例如：

```java
CommandCatalog catalog = new CommandCatalog.Builder()
        .addReservation("100101", "系统", "握手", "查询模块信息",
                "10=系统，01=握手，01=查询模块信息",
                CommandDirection.OUTBOUND, "App 主动查询模块基础信息")
        .addReservation("100102", "系统", "握手", "模块信息上报",
                "10=系统，01=握手，02=模块信息上报",
                CommandDirection.INBOUND, "模块返回基础信息")
        .addReservation("110201", "验光流程", "模式控制", "切换自动验光",
                "11=验光流程，02=模式控制，01=切换自动验光",
                CommandDirection.OUTBOUND, "切换到自动验光模式")
        .build();
```

然后 UI、ViewModel、业务层都只引用这些固定编号。

### 第二步：导出模板给设备端填写

```java
CommandTableCsvExporter exporter = new CommandTableCsvExporter();
exporter.exportTemplate(catalog, outputStream);
```

设备端同事拿到模板后，只需要填写真实命令内容。

### 第三步：给已绑定编码的按钮加长按提示

```java
CommandViewHelper.bindClickWithCodeHint(
        button,
        reservation,
        v -> {
            // 原有点击逻辑
        }
);
```

效果：

- 点击走原业务逻辑
- 长按直接弹出 `编码 + 编号解释`

### 第四步：在“命令设置页”里加载手机中的 CSV

```java
CommandTableLoader loader = new CommandTableLoader();
CommandTable table = loader.loadFromUri(context, uri);
CommandCatalog.ValidationResult result = catalog.validate(table);
commandEngine.replaceCommandTable(table);
```

说明：

- `validate(table)` 可以检查：
  - 哪些预留编码在文档里缺失
  - 文档里有没有 App 没预留的编码
  - 方向是否不一致
  - 哪些编码还没真正填好命令

### 第五步：发送时只传编号

```java
Map<String, String> args = new LinkedHashMap<>();
args.put("mode", "AUTO");

commandEngine.sendByCode("110201", args, command ->
        tcpServerService.sendMessageToClient(clientId, command.getRawMessage())
);
```

如果 `发送命令` 配成：

`AT+MODE=${mode}`

最终就会发出：

`AT+MODE=AUTO`

### 第六步：接收时先反查编号，再执行业务逻辑

```java
commandEngine.registerInboundHandler("100102", inboundCommand -> {
    String raw = inboundCommand.getRawMessage();
    // 根据编号进入对应业务逻辑
});

commandEngine.dispatchInbound(rawMessageFromDevice);
```

如果使用正则匹配，还可以取：

```java
List<String> groups = inboundCommand.getRegexGroups();
```

## 7. 这套方案适合你的原因

你的场景里，最核心的是“协议未定，但 App 流程要先做”。  
这套设计把“业务编号”和“真实协议字符串”彻底拆开了：

- App 只跟固定编号耦合
- 真实命令晚点再填
- 设备端修改命令时，优先改文档，不改 App 编号
- 接收端也不用直接写死字符串判断，而是先反查编号

这会让你后面协议迭代时稳定很多。

## 8. 建议的团队协作规则

- App 侧负责维护“编号目录”，只增不改。
- 设备端负责填写“真实命令”列。
- 文档回传后，App 在命令设置页加载并校验。
- 如果校验发现缺号、错号、方向不一致，直接提示，不继续上线使用。

## 9. 默认编码表与模板

模板文件已放到：

- `docs/templates/command_table_template.csv`
- `lib/src/main/res/raw/command_table_optometry_default.csv`

其中：

- `docs/templates/command_table_template.csv` 适合直接复制后发给设备端填写
- `lib/src/main/res/raw/command_table_optometry_default.csv` 是共享内置默认编码表，`app` 和 `demo` 的命令设置页首次进入时都会从这里加载
- `app` 和 `demo` 不再各自维护一份重复仓库、重复编号目录、重复默认 CSV，统一依赖 `lib` 中的 `CommandSettingsRepository + CommandProfile`
