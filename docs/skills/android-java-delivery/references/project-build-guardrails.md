# 构建与模块护栏

## 当前仓库基线

- `gradle.properties` 已存在，`android.useAndroidX=true` 已配置好，不需要重复创建。
- 当前模块：
  - `app/`：正式验光业务应用
  - `demo/`：演示与样例应用
  - `lib/`：公共基础库
- 当前基线配置：
  - Java 11
  - `compileSdk 36`
  - `minSdk 26`
  - `targetSdk 36`
  - `viewBinding true`
- 当前仓库已经有 Gradle Wrapper，默认使用 `.\gradlew.bat`。

## 默认验证矩阵

| 改动范围 | 默认验证 |
|----------|----------|
| 只改 `app/` 页面或业务代码 | `.\gradlew.bat :app:assembleDebug` |
| 只改 `demo/` 页面或样例代码 | `.\gradlew.bat :demo:assembleDebug` |
| 改 `lib/`、共享基础类、镜像实现 | `.\gradlew.bat :app:assembleDebug` 和 `.\gradlew.bat :demo:assembleDebug` |
| 只改 skill / 文档 | 不强制跑 APK 构建，优先跑对应文档校验 |

## 排错顺序

1. 先读完整错误信息，定位模块、文件和行号。
2. 先跑最小命令，不要默认全仓 `clean`。
3. 如果是依赖或类冲突，再看 `:app:dependencies` 或 `:demo:dependencies`。
4. 如果普通报错信息不够，再加 `--stacktrace`。
5. 只有普通构建链路无法定位时，才考虑 `clean` 或 `--refresh-dependencies`。

## 常用命令

```powershell
.\gradlew.bat projects
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :demo:assembleDebug
.\gradlew.bat :app:dependencies
.\gradlew.bat :demo:dependencies
.\gradlew.bat :app:assembleDebug --stacktrace
.\gradlew.bat :demo:assembleDebug --stacktrace
```

## 不要默认做的事

- 不要因为是 Android 项目就默认补 `gradle.properties`、Wrapper 或 AndroidX 配置，本仓库已经具备。
- 不要默认引入 Product Flavors。当前 `app`、`demo`、`lib` 都没有 flavor 体系，除非任务明确要求区分环境包。
- 不要把官方 Kotlin DSL 配置片段直接搬过来。当前仓库是 Groovy Gradle 脚本。
- 不要把 `clean assembleDebug` 当第一反应；它更适合普通构建无法定位后的第二层排查。

## 与当前项目强相关的注意点

- 如果改动触达 `lib/`，默认把它当共享基础设施，验证范围至少覆盖 `app` 和 `demo`。
- 如果改动涉及设备通信或协议类，构建通过不等于风险可接受，还要结合代码边界判断。
- 对 `app` 和 `demo` 的镜像实现，不要因为一边先编过了就默认另一边不用看。
