# AGENTS.md

## Project Snapshot

- This repository is a multi-module Android project for a WiFi-connected optometry workflow.
- `app/` is the main product app under `com.wifi.optometry`.
- `demo/` is a demo/sample app under `com.example.wifidemo`; it also contains hardware reference documents and command notes.
- `lib/` is the shared base library for BaseUI, MVVM helpers, logging, database access, and utility code.

## Entry Points

- Main product launcher: `app/src/main/java/com/wifi/optometry/ui/MainActivity.java`
- Demo launcher: `demo/src/main/java/com/example/wifidemo/sample/ui/DemoHomeActivity.java`
- Shared activity base: `lib/src/main/java/com/wifi/lib/baseui/BaseVBActivity.java`

## Tech Stack And Patterns

- Language: Java 11. Prefer Java over Kotlin unless the task explicitly asks for Kotlin.
- UI stack: Android Views + XML + ViewBinding. Do not introduce Compose unless requested.
- Architecture style: lightweight MVVM using `BaseMvvmActivity`, `BaseMvvmFragment`, `LiveData`, repositories, and shared `lib/` base classes.
- Networking/device communication uses a foreground TCP service plus HC-25-related WiFi discovery logic.

## Module Boundaries

- Put reusable infrastructure in `lib/`: base UI, dialogs, delegates, logging, DB, and shared framework helpers.
- Put production optometry workflow and clinic-facing screens in `app/`.
- Put experiments, showcase screens, BRVAH examples, and demo-only flows in `demo/`.
- If a change touches `lib/`, verify both `app` and `demo` still build.

## Duplication Guardrails

- Device communication code exists in both `app` and `demo` with similar classes such as `DeviceManager`, `DeviceHistoryStore`, `Hc25MacDiscoveryClient`, `TcpServerService`, `HeartbeatManager`, and `ServerConstance`.
- Domain/model code is also mirrored between `app/domain/...` and `demo/clinic/...`.
- Before changing shared behavior, inspect both modules and decide whether the fix should be mirrored.
- If only one side is intentionally changed, call that out in the summary so the divergence is explicit.

## Device And Protocol Safety

- Do not casually change `SERVER_PORT`; both `app` and `demo` currently use `39509`.
- Do not change HC-25 discovery, command formats, or WiFi/TCP behavior without checking the local reference docs first.
- Relevant local references:
  - `demo/命令清单`
  - `demo/HC-25（板载天线）用户手册V2.0-20251212.pdf`
  - `demo/验光仪功能说明.docx`
- Treat real-device compatibility as more important than cleanup-oriented refactors.
- When editing `DeviceHistoryStore`, `DeviceManager`, `TcpServerService`, or `Hc25MacDiscoveryClient`, preserve MAC normalization/history behavior unless the task explicitly changes device identity rules.

## UI And Code Style

- Follow the existing XML + ViewBinding approach and reuse shared base classes from `lib/` before adding new scaffolding.
- Prefer small targeted changes over broad architectural rewrites.
- For reusable new UI text, prefer `strings.xml`. Do not mass-migrate existing inline strings unless requested.
- Match the current project style: straightforward Java classes, explicit wiring, and minimal framework churn.

## Files To Treat Carefully

- `build/`, `.gradle/`, and `.tmp_res/` are generated or support artifacts; ignore them unless the task is explicitly about generated outputs or resource recovery.
- Hardware manuals and command notes in `demo/` are reference material, not cleanup targets.

## Validation

- Verified useful commands:
  - `.\gradlew.bat projects`
  - `.\gradlew.bat :app:assembleDebug`
  - `.\gradlew.bat :demo:assembleDebug`
- Current limitation:
  - `.\gradlew.bat :demo:testDebugUnitTest` currently fails because the `demo` module does not declare JUnit test dependencies. Do not assume unit tests are ready to run unless that setup is added first.

## Preferred Workflow For Codex

- Read the affected module first instead of assuming `app` and `demo` share identical behavior.
- When touching device communication, check both code and local hardware docs.
- Validate with the narrowest relevant Gradle command, then expand only if the change affects shared code.
