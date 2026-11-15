# 更新日志

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 的写法，
版本号对应 `versionName`，括号里是 `versionCode`。

## [0.52-native] - 2026-11-12

最低支持 Android 5.0，代价是依赖退回 2025 年的版本线。

### 变更

- `minSdk` 24 → **21**，最低支持 Android 5.0（Lollipop）
- 为守住 API 21，依赖退回各自最后一条支持 21 的版本：
  - Compose BOM 2026.09.00 → **2025.10.00**（Compose 1.9.3 / Material3 1.4.0）
  - activity-compose 1.13.0 → **1.11.0**（1.12+ 引入 `navigationevent`，要求 23）
  - lifecycle 2.11.0 → **2.9.4**（2.10+ 要求 23）
  - navigation-compose 2.10.2 → **2.8.5**（2.9+ 要求 24）
  - core-ktx 1.19.1 → **1.17.0**
- `versionCode` 100008

### 修复

- v1 签名从「兼容性冗余」变成 **API 21–23 必需**，确认 AOSP testkey 签的 JAR 签名有效

## [0.51-native] - 2026-11-12

首个面向社区反馈的版本。

### 新增

- 从 iApp（裕语言）版「function制作助手 0.50b」完整重写为 Kotlin + Jetpack Compose
  - 作品管理：新建 / 删除行为包，卡片显示封面、名称、修改时间
  - 指令编辑：`.mcfunction` 增删改查、行号、命令高亮、未保存标记
  - 批量生成：元音占位符 + 代数式，支持 `x*2+1` 表达式
  - 作品信息：封面 / 名称 / 简介 / 作者，写入 `manifest.json` 并重生成 UUID
  - 装载到游戏：国际版 / 中国版存档列表，合并 `world_behavior_packs.json`
- **V3 密钥轮换签名**：AOSP testkey → ipuser，v1 + v2 + v3 三套签名齐全，
  Android 9+ 可从旧证书平滑升级
- GitHub Actions CI：单测 + debug/release 构建 + APK 产物

### 变更

- AGP 8.13.0 → **9.5.1**，Gradle 8.14.5 → **9.8.2**
- 迁移到 AGP 9 的**内置 Kotlin**，移除 `kotlin-android` 插件，启用新 DSL
- 依赖升级：Kotlin 2.4.21、compileSdk 37、Compose BOM（当时为最新）
- Gradle 发行版改用腾讯云镜像，CI 自动切回官方源

### 修复

- Android 11+ 读不到 `Android/data` 时改用 SAF 目录选择；
  旧共享目录被其他安装占用时自动降级到应用私有目录并提示
- `world_behavior_packs.json` 改为 JSON 解析 + 按 `pack_id` 去重，不再字符串拼接
- 重命名先落盘再改名，避免原版「重命名变成删除」的问题
- 命令高亮改用 `VisualTransformation`，修掉原版 `telltellraw` 笔误，补上 `title` 等
- 批量生成算法明确语义，替换原版不稳定的自我替换逻辑

### 已知限制

- 装载到存档的完整流程未在真机验证（开发机未安装 Minecraft），
  选目录与 SAF 兜底已验证，写入逻辑有 instrumented 测试覆盖
- 中文版（网易）只按路径扫描，特殊目录需用 SAF 手动选

---

更早的重写过程（工程搭建、界面重构、原版行为复原）没有单独发版，
可以直接看 `git log`。

[0.52-native]: https://github.com/codesenlin/FunctionBuilder/releases/tag/v0.52
[0.51-native]: https://github.com/codesenlin/FunctionBuilder/releases/tag/v0.51
