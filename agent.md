# Agent Guide

## 项目概览

- 项目名称：`ArBoxMeasure`
- 项目类型：Android AR 测量应用
- 主要技术：`Kotlin`、`Android SDK`、`Jetpack Compose`、`ARCore`、`Sceneform`
- 包名：`com.rms.boxmeasure`
- 构建工具：`Gradle` / `AGP 8.13.2`

## 目录说明

- `app/src/main/java/com/rms/boxmeasure`：核心业务代码
- `app/src/main/res`：布局、图片、字符串等资源
- `app/src/main/assets`：模型等静态资源
- `app/src/test`：本地单元测试
- `app/src/androidTest`：设备端测试
- `build/`、`app/build/`：构建产物，不应手工修改

## 关键入口

- `FirstActivity.kt`：启动入口
- `ArMeasureActivity.kt`：AR 测量主流程
- `AnchorInfoBean.kt`：锚点数据结构
- `FaceToCameraNode.kt`：朝向相机的节点逻辑

## 常用命令

在 Windows PowerShell 下优先使用：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat lint
```

如果需要清理构建缓存：

```powershell
.\gradlew.bat clean
```

## 修改约束

- 优先在 `app/src/main` 下进行业务修改。
- 不要手工编辑 `build/`、`.gradle/`、APK、编译缓存等产物。
- 修改资源文案时，优先放在 `app/src/main/res/values/strings.xml`，不要在代码中硬编码中文。
- 涉及 AR 行为调整时，优先检查 `ArMeasureActivity.kt` 及相关布局/资源。
- 如果只是修正文档，不要顺带改动 Gradle、Manifest 或资源文件。

## Windows 与中文编码

当前环境运行在 Windows，处理中文时必须避免乱码：

- 新增或修改文本文件统一使用 `UTF-8` 编码。
- 在 PowerShell 中读取中文文件时，优先显式指定 UTF-8。
- 如果终端输出乱码，先设置：

```powershell
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
```

- 写入文件时优先使用支持 UTF-8 的方式，避免混用 ANSI、GBK、UTF-8。
- 当前仓库的 `README.md` 在终端中已出现乱码迹象，编辑前先确认原文件实际编码，避免直接覆盖导致内容损坏。

## 验证建议

- 纯代码修改：至少执行 `.\gradlew.bat test`
- 资源或清单修改：补充执行 `.\gradlew.bat lint`
- AR 功能修改：除静态检查外，最好在支持 ARCore 的真机上验证

## 提交原则

- 仅提交与当前任务直接相关的文件。
- 避免把本地环境文件、编译产物或无关格式化改动带入提交。
- 若发现现有文件存在编码问题，先单独确认，再决定是否修复。
