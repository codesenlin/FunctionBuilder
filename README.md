# function制作助手（原生重写版）

把 iApp（裕语言 5.0.1012）编写的「function制作助手 0.50b」重写为 **Kotlin + Jetpack Compose** 的
原生 Android 应用。

原版是一个 Minecraft 基岩版行为包（behavior pack）制作工具：在手机上新建作品、写 `/function` 指令、
一键装载进存档。本仓库是它的原生实现，保留了原有功能、数据格式和视觉风格。

> 原版作者：小傻羊驼（QQ 3485453922）。本项目是对其作品的界面与工程重写，指令模板
> （`assets/functiontestin.zip`）与图片素材来自原工程。

## 功能

- **作品管理**：新建 / 删除行为包，卡片显示名称、封面、上次修改时间
- **指令编辑**：`.mcfunction` 文件增删改查，行号、命令高亮（`say`、`execute` 等 40+ 指令染成原版粉色）
- **批量生成**：用元音占位符（ā á ǎ à…）+ 代数式批量产出指令，支持 `x*2+1` 这类表达式
- **作品信息**：改封面、名称、简介、作者，写入 `manifest.json` 并重新生成 UUID
- **装载到游戏**：国际版 / 中国版存档列表，复制行为包并合并 `world_behavior_packs.json`
- **兼容旧数据**：沿用原版的目录、设置文件与时间戳格式，可直接覆盖安装旧版

## 构建

需要 JDK 17、Android SDK Platform 36。

```bash
git clone <this-repo>
cd FunctionBuilder
export ANDROID_HOME=/path/to/Android/Sdk     # 或写进 local.properties
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
```

也可以直接用 Android Studio 打开。依赖版本集中在 `gradle/libs.versions.toml`。

技术栈：Kotlin 2.2.21 · AGP 8.13.0 · Gradle 8.14.5 · Compose BOM 2024.12.01 ·
minSdk 24 / targetSdk 36 · 无第三方运行时依赖（图片用 `BitmapFactory` 采样解码，JSON 用 `org.json`）。

## 签名

仓库**不包含**签名私钥。`signing/` 下的 `.jks` 与 `keystore.properties` 已在 `.gitignore` 中排除。

- 自己构建：复制 `signing/keystore.properties.example` 为 `signing/keystore.properties` 并填入自己的密钥；
- 不配置也能编译：`assembleRelease` 会自动退回 debug 签名，产物仍可安装（只是不能覆盖安装别人的版本）。

原版 APK 的证书 SHA-1 是 `AE:77:39:17:CC:7A:75:23:B4:1E:1E:B9:5B:ED:61:CF:0A:A8:E3:B0`（`CN=ipuser`）。
用同一把私钥签名时，`versionCode` 需要大于旧版的 `100005`，本项目取 `100006`，因此可以正常覆盖安装。

## 目录结构

```
app/src/main/java/com/functionbuilder/
├── MainActivity.kt
├── data/                  # 业务逻辑，不依赖 Compose，可直接 JVM 测试
│   ├── PackRepository.kt      # 作品列表 / 新建（解压模板）/ 删除
│   ├── FnRepository.kt        # .mcfunction 增删改查
│   ├── PackManifest.kt        # manifest.json 与 world_behavior_packs.json
│   ├── Installer.kt           # 装载到存档
│   ├── MinecraftAccess.kt     # 国际版/中国版存档定位（含 SAF 兜底）
│   ├── BatchEngine.kt         # 批量生成 + 表达式求值
│   └── Prefs.kt               # 原版格式的设置文件
├── ui/                    # Compose 界面
│   ├── AppRoot.kt             # 导航、权限申请、Snackbar
│   ├── FbViewModel.kt         # 全局状态
│   ├── components/            # 标题栏、对话框、图片解码
│   ├── screens/               # 8 个页面
│   └── theme/
└── util/                  # 文件系统抽象（File / SAF 双实现）、路径、Intent
```

`util/Fs.kt` 用 `FsNode` 接口把普通文件和 SAF 的 `DocumentFile` 统一起来，装载逻辑只需写一遍。

## 与原版的差异

1. **权限与路径**：Android 11+ 无法直接访问 `Android/data/<其它应用>`，原版的绝对路径写法在新系统上必然失败。
   新版在直接路径不可用时提供 SAF 目录选择（支持选 `com.mojang`、`minecraftWorlds` 或单个存档），
   并在旧共享目录被上一次安装占用时自动降级到应用私有目录 + 界面提示。
2. **`world_behavior_packs.json` 合并**：原版把 JSON 字符串拼到数组末尾（对空白敏感、容易写坏）；
   新版用 `JSONArray` 解析、按 `pack_id` 去重后重新序列化。
3. **重命名不再丢数据**：先落盘再改名，失败时保留原文件（原版 0.50 更新日志里提过“重命名变成删除”的 bug）。
4. **命令高亮**：原版把整段内容拼成 HTML 塞回 `EditText`，会破坏光标；新版用 `VisualTransformation`，
   高亮的同时保持正常编辑。并修掉了原版 `telltellraw` 的拼接笔误，补上 `title`/`tell`/`tellraw` 等。
5. **批量生成**：原版 `many.iyu` 的算法是让占位符列表反复自我替换，结果不稳定；
   新版语义明确——第 i 条输出把占位符替换成「代数式在 `x = 初始值 + i` 处的值」。
6. **界面**：沉浸式标题栏（状态栏同色、图标转白）、抽屉宽度与头部图片对齐、空状态重做。
7. **更新检查**：原版通过简书文章解析版本号，链接早已失效；新版保留设置项以兼容旧配置，不再发起该请求。

## 兼容性

| 数据 | 路径 / 格式 |
| --- | --- |
| 作品根目录 | `/sdcard/Android/functiontested` |
| 解压临时目录 | `/sdcard/Android/functiontestin` |
| 设置文件 | `/sdcard/Android/functionbuilderin`，格式 `0.50#1#0#1` |
| 修改时间 | 每个作品的 `test/time`，`yyyy-MM-dd HH:mm:ss` |
| 行为包结构 | `manifest.json` + `pack_icon.png` + `functions/*.mcfunction` |
| 装载目标 | 存档 `behavior_packs/functiontestin`，并合并 `world_behavior_packs.json` |

有「所有文件访问权限」时沿用旧路径；没有则退回 `/sdcard/Android/data/com.functionbuilder/files/functiontested`。

## 测试

```bash
./gradlew testDebugUnitTest          # 13 条 JVM 单元测试
./gradlew connectedDebugAndroidTest  # 4 条真机测试，需要连接设备
```

单元测试覆盖模板解压（含 zip-slip 防护）、manifest 读写、文件名校验与重命名、递归删除、批量生成表达式。
真机测试覆盖新建作品、装载进存档与索引合并的具体文件操作。

已在 Android 14（OPPO PERM10）上实测：覆盖安装、新建作品、编辑保存回读、批量生成、
作品信息回填、装载流程的存档列表与 SAF 兜底。

## 说明

- 本项目是对「function制作助手」的重写，指令模板与图片素材来自原作者的作品；
  如需二次分发请遵循原作者的意愿。
- 仓库未附许可证，作者确定后可在 GitHub 上选择。
