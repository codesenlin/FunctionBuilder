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

国内网络下载慢的话：

```bash
# Maven 依赖走阿里云镜像（默认关闭，只在你本地生效）
./gradlew -DcnMirror=true assembleDebug
```

Gradle 本体由 `gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 决定，
国内可以把官方地址临时换成腾讯云镜像（首次下载约 130MB）：

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-8.14.5-bin.zip
```

技术栈：Kotlin 2.2.21 · AGP 8.13.0 · Gradle 8.14.5 · Compose BOM 2024.12.01 ·
minSdk 24 / targetSdk 36 · 无第三方运行时依赖（图片用 `BitmapFactory` 采样解码，JSON 用 `org.json`）。

## 签名与 V3 密钥轮换

iApp 导出 APK 时默认用 **AOSP 公开的 testkey**（`CN=Android`）签名；0.50b 这一版是用
**`ipuser`** 重新签的。本仓库把这条轮换关系正式写进 APK 的 v3 签名块，于是：

| 方案 | 签名证书 | 生效范围 |
| --- | --- | --- |
| v1（JAR signing） | AOSP testkey | API < 24（本项目 minSdk 24，属兼容性冗余） |
| v2 | AOSP testkey | API 24 – 27 |
| v3 | `ipuser` + 轮换链 | API 28 及以上 |

关键参数是 `--rotation-min-sdk-version 28`：不指定时 apksigner 默认把轮换放到 API 33+ 的
v3.1 块里，Android 9–12 上仍会被认成 AOSP；指定 28 之后 28+ 全部走 ipuser，
代价是 v3.1 块不再生成（`v3.1: false`，不影响 28+ 的升级）。

相关文件：

| 文件 | 说明 |
| --- | --- |
| `signing/aosp-testkey.jks` | AOSP testkey 转成的 keystore，链上**最老**的证书，签 v1/v2 |
| `signing/ipuser.jks` | 链上**最新**的证书，签 v3 / debug |
| `signing/lineage` | 轮换链，`apksigner rotate` 生成 |
| `signing/rotation.properties` | 最老签名者 + lineage 配置 |
| `signing/keystore.properties` | 最新签名者（ipuser）配置 |

证书指纹：

- AOSP `CN=Android`：SHA-1 `61:ED:37:7E:85:D3:86:A8:DF:EE:6B:86:4B:D8:5B:0B:FA:A5:AF:81`
- `CN=ipuser`：SHA-1 `AE:77:39:17:CC:7A:75:23:B4:1E:1E:B9:5B:ED:61:CF:0A:A8:E3:B0`

构建方式：`assembleRelease` 先产出 `app-release-unsigned.apk`，再由 `signReleaseV3` 任务调用
apksigner 签出 `app-release.apk`（AOSP 签 v1/v2 + ipuser 签 v3 + lineage）。debug 包直接用 ipuser 签，
方便在装过旧版的机器上反复覆盖调试。

## 升级兼容性

| 已装版本 | Android 7–8.1（API 24–27） | Android 9+（API 28+） |
| --- | --- | --- |
| AOSP testkey 签名（iApp 直接导出） | ✅ 可升级 | ✅ 可升级（凭 lineage） |
| ipuser 签名（0.50b 及以前的正式版） | ❌ 不可升级 | ✅ 可升级 |

也就是说 Android 9 以下的老设备只有 AOSP 签名的旧包能升级；这些机型现在很少，属于可接受的取舍。

再次轮换到新证书 B 时：

```bash
apksigner rotate --in signing/lineage --out signing/lineage2 \
  --old-signer --ks signing/ipuser.jks --ks-pass pass:*** --ks-key-alias ipuser \
  --new-signer --ks signing/next.jks    --ks-pass pass:*** --ks-key-alias next
```

然后把 `signing/rotation.properties` 的 `lineage` 指向 `lineage2`，
`signing/keystore.properties` 指向 `next.jks`，并把 `--rotation-min-sdk-version` 调到当时的
Android 版本。**v1/v2 永远用最老的 AOSP，不要改 `oldStoreFile`。**

**关于密钥公开**：AOSP testkey 本来就是公开的（AOSP 源码里就有），`ipuser` 是 iApp 那套包里带的，
同样公开，所以两把都放进了仓库，clone 下来即可构建出可覆盖安装的包。如果要做正式分发（应用商店等），
请换一把只在自己机器上的私钥，并打开 `.gitignore` 里对应的两行。

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
