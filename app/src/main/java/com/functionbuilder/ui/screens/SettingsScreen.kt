package com.functionbuilder.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.functionbuilder.BuildConfig
import com.functionbuilder.data.MinecraftAccess
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbTopBar

/**
 * 设置页。
 * 对应旧版 `seting.iyu`：中国版存档点、三个开关、版本信息。
 */
@Composable
fun SettingsScreen(vm: FbViewModel, nav: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings = vm.settings

    var hasAllFiles by remember { mutableStateOf(vm.hasAllFilesAccess()) }
    var showChinaDirs by remember { mutableStateOf(false) }
    var chinaDirs by remember { mutableStateOf<List<String>>(emptyList()) }

    val allFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { hasAllFiles = vm.hasAllFilesAccess() }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    // 从系统设置页返回后刷新权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllFiles = vm.hasAllFilesAccess()
                vm.refreshPacks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { FbTopBar(title = "设置", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F7F7))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── 存储与权限 ──────────────────────────────
            SettingCard("存储位置") {
                KeyValue("作品目录", vm.packRoot().absolutePath)
                KeyValue(
                    "全盘访问权限",
                    if (hasAllFiles) "已获得" else "未获得（只能写应用私有目录）",
                )
                if (!hasAllFiles) {
                    Text(
                        "要让作品目录保持在 /sdcard/Android/functiontested 并直接写进 Minecraft，" +
                            "需要授予“所有文件访问权限”。",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF888888),
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                runCatching {
                                    allFilesLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                }.onFailure {
                                    runCatching {
                                        allFilesLauncher.launch(
                                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                                        )
                                    }
                                }
                            } else {
                                legacyPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    ),
                                )
                                hasAllFiles = vm.hasAllFilesAccess()
                            }
                        },
                    ) {
                        Text("去授权")
                    }
                }
            }

            // ── 中国版存档点 ────────────────────────────
            SettingCard("中国版存档") {
                Text(
                    "点击查看中国版存档点",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1565C0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val dirs = MinecraftAccess.chineseWorldDirs().map { it.absolutePath }
                            chinaDirs = if (dirs.isEmpty()) {
                                listOf("未发现中国版目录，请确认已安装并启动过中国版 Minecraft")
                            } else {
                                dirs
                            }
                            showChinaDirs = true
                        }
                        .padding(vertical = 6.dp),
                )
                Text(
                    "路径形如 /sdcard/Android/data/<包名>/files/minecraftWorlds",
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                )
            }

            // ── 开关 ────────────────────────────────────
            SettingCard("选项") {
                CheckRow(
                    label = "打开软件检查版本更新",
                    checked = settings.checkUpdate,
                    onChange = { vm.updateSettings(settings.copy(checkUpdate = it)) },
                )
                Text(
                    "原版通过简书文章检查更新，该链接已失效；此开关仅为兼容旧设置而保留。",
                    fontSize = 10.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
                CheckRow(
                    label = "启用命令高亮（特殊命令颜色）",
                    checked = settings.htmlHighlight,
                    onChange = { vm.updateSettings(settings.copy(htmlHighlight = it)) },
                )
                CheckRow(
                    label = "显示行数",
                    checked = settings.showLineNumbers,
                    onChange = { vm.updateSettings(settings.copy(showLineNumbers = it)) },
                )
            }

            // ── 关于 ────────────────────────────────────
            SettingCard("关于") {
                KeyValue("版本", BuildConfig.VERSION_NAME)
                KeyValue("原始版本", "iApp V5.0.1012 / 0.50b")
                KeyValue("包名", BuildConfig.APPLICATION_ID)
                Text(
                    "已从 iApp（裕语言）原生重写为 Kotlin + Jetpack Compose。",
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showChinaDirs) {
        FbDialog(
            title = "中国版存档点",
            message = chinaDirs.joinToString("\n\n"),
            buttons = listOf(DlgButton("确定", emphasis = true) { showChinaDirs = false }),
            onDismiss = { showChinaDirs = false },
        )
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1565C0))
            Spacer(Modifier.height(8.dp))
            Column { content() }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(key, fontSize = 12.sp, color = Color(0xFF888888), modifier = Modifier.padding(end = 8.dp))
        Text(value, fontSize = 12.sp, color = Color(0xFF333333))
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onChange(it) })
        Text(label, fontSize = 14.sp, color = Color(0xFF333333))
    }
}
