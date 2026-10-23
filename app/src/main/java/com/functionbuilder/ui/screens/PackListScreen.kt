package com.functionbuilder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.BuildConfig
import com.functionbuilder.R
import com.functionbuilder.data.PackInfo
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.Routes
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbInputDialog
import com.functionbuilder.ui.components.FbTopBar
import com.functionbuilder.ui.components.PackIcon
import kotlinx.coroutines.launch

/**
 * 主页：作品列表。
 * 对应旧版 `funall.iyu`（列表 + 新建）和 `left.iyu`（左侧抽屉）。
 */
@Composable
fun PackListScreen(vm: FbViewModel, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var showNewDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PackInfo?>(null) }
    var showChangelog by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(vm.showChangelogOnStart) {
        if (vm.showChangelogOnStart) showChangelog = true
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // 宽度和头部图片一致（图片高 200dp、本身是方图，这样两边正好对齐且不被裁）
            ModalDrawerSheet(modifier = Modifier.width(200.dp)) {
                DrawerPanel(
                    onAbout = {
                        scope.launch { drawerState.close() }
                        showAbout = true
                    },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        nav.navigate(Routes.SETTINGS)
                    },
                    onHelp = {
                        scope.launch { drawerState.close() }
                        showHelp = true
                    },
                    onApps = {
                        scope.launch { drawerState.close() }
                        nav.navigate(Routes.APPS)
                    },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                FbTopBar(
                    title = "function制作助手 <v0.50>",
                    leading = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.logo_menu),
                                contentDescription = "菜单",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNewDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.logo_add),
                                contentDescription = "新建",
                                tint = Color.Unspecified,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF2F4F7)),
            ) {
                // 原版用世界截图当背景，这里保留但压得很淡，保证文字可读。
                Image(
                    painter = painterResource(R.drawable.world_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.18f,
                    modifier = Modifier.fillMaxSize(),
                )

                Column(Modifier.fillMaxSize()) {
                if (vm.legacyDirBlocked()) {
                    LegacyBlockedBanner(onSettings = { nav.navigate(Routes.SETTINGS) })
                }
                if (vm.packs.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.pack_icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(88.dp)
                                .alpha(0.85f),
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "还没有作品",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF444444),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "一个作品就是一个 Minecraft 行为包，\n里面放你想一次执行的一串指令。",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF888888),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { showNewDialog = true }) {
                            Text("新建作品", fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "作品目录：${vm.packRoot().absolutePath}",
                            fontSize = 10.sp,
                            color = Color(0xFFAAAAAA),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(vm.packs, key = { it.dir.absolutePath }) { pack ->
                            PackRow(
                                pack = pack,
                                onClick = {
                                    vm.openPack(pack)
                                    nav.navigate(Routes.FUNCTIONS)
                                },
                                onDelete = { pendingDelete = pack },
                            )
                        }
                        item {
                            Text(
                                text = "共 ${vm.packs.size} 个作品 · ${vm.packRoot().absolutePath}",
                                fontSize = 10.sp,
                                color = Color(0xFF9A9A9A),
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
                }
            }
        }
    }

    // ── 新建作品 ──────────────────────────────────────────
    if (showNewDialog) {
        FbInputDialog(
            title = "新建，请命名",
            label = "点击命名这个包",
            confirmLabel = "新建",
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                if (name.isEmpty()) {
                    vm.toast("亲，你什么都没写")
                } else {
                    showNewDialog = false
                    vm.createPack(name) { error ->
                        if (error != null) vm.toast(error) else vm.toast("新建成功")
                    }
                }
            },
        )
    }

    // ── 删除确认 ──────────────────────────────────────────
    pendingDelete?.let { pack ->
        FbDialog(
            title = "你真的要删除吗",
            message = "作品「${pack.name}」及其所有 function 都会被删除，无法恢复。",
            buttons = listOf(
                DlgButton("取消") { pendingDelete = null },
                DlgButton("确定", emphasis = true) {
                    val target = pack
                    pendingDelete = null
                    vm.deletePack(target) { error ->
                        if (error != null) vm.toast(error) else vm.toast("删除成功")
                    }
                },
            ),
            onDismiss = { pendingDelete = null },
        )
    }

    if (showChangelog) {
        ChangelogDialog(
            onDismiss = {
                showChangelog = false
                vm.dismissChangelog()
            },
        )
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })
    if (showHelp) HelpDialog(context = context, onDismiss = { showHelp = false })
}

@Composable
private fun PackRow(pack: PackInfo, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(shape = RoundedCornerShape(8.dp)) {
                PackIcon(icon = pack.icon, fallbackRes = R.drawable.pack_icon, sizeDp = 46)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "上次修改时间：\n${pack.modifiedDisplay}",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF888888),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.logo_fd),
                    contentDescription = "删除",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** 侧滑抽屉，对应 `left.iyu`。 */
@Composable
private fun DrawerPanel(
    onAbout: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onApps: () -> Unit,
) {
    Column(Modifier.fillMaxHeight()) {
        // 头部只放图，200dp 见方（原图就是 600x600，等比缩放不裁切）
        Image(
            painter = painterResource(R.drawable.writer),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        HorizontalDivider()
        DrawerAvatarItem("关于作者", onAbout)
        DrawerItem(R.drawable.logo_seting, "设置", onSettings)
        DrawerItem(R.drawable.logo_help, "帮助", onHelp)
        DrawerItem(R.drawable.logo_app, "更多实用应用", onApps)
        HorizontalDivider()
        Text(
            text = "v${BuildConfig.VERSION_NAME}\n基于 iApp 0.50 原生重写",
            fontSize = 10.sp,
            lineHeight = 15.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** “关于作者”条目：图标直接用作者头像（圆形），比塞个不相干的图标自然。 */
@Composable
private fun DrawerAvatarItem(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.writer),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
    }
}

@Composable
private fun DrawerItem(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
    }
}

/** 首次启动的更新日志，内容来自原版 `res/loading`。 */
@Composable
private fun ChangelogDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember {
        runCatching {
            context.assets.open("changelog.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("测试版 0.50 更新日志")
    }
    FbDialog(
        title = "测试版0.50更新日志",
        message = text.trim(),
        buttons = listOf(DlgButton("不再显示", emphasis = true, onClick = onDismiss)),
        onDismiss = onDismiss,
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    FbDialog(
        title = "作者：小傻羊驼",
        message = "感谢 iApp 平台的技术支持\n" +
            "感谢 yangzii 大佬的 function 模板\n" +
            "作者 QQ：3485453922\n" +
            "注意这只是测试版 QAQ\n" +
            "抱住 yangzii 的大腿不放 QWQ",
        buttons = listOf(
            DlgButton("加入工匠社") {
                onDismiss()
                if (!com.functionbuilder.util.Intents.openQqGroup(context, "596111192")) {
                    android.widget.Toast.makeText(context, "没有找到 QQ", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            DlgButton("快捷加作者QQ", emphasis = true) {
                onDismiss()
                if (!com.functionbuilder.util.Intents.openQqChat(context, "3485453922")) {
                    android.widget.Toast.makeText(context, "没有找到 QQ", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
        ),
        onDismiss = onDismiss,
    )
}

/** 旧共享目录被上一次安装占用时的提示条。 */
@Composable
private fun LegacyBlockedBanner(onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "检测到 /sdcard/Android/functiontested 被上一次安装占用（当前安装无读写权限），" +
                "已临时改用应用私有目录。用文件管理器删掉那个空目录后重启即可恢复。",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = Color(0xFF8D6E00),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSettings) {
            Text("去设置", fontSize = 12.sp, color = Color(0xFF1565C0))
        }
    }
}
