package com.functionbuilder.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.R
import com.functionbuilder.data.InstallRoot
import com.functionbuilder.data.Installer
import com.functionbuilder.data.WorldEntry
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.Routes
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbTopBar
import com.functionbuilder.ui.components.rememberImage

/**
 * 选择存档并装载。
 * 对应旧版 `endna.iyu`（国际版）和 `endch.iyu`（中国版）。
 */
@Composable
fun WorldPickerScreen(vm: FbViewModel, nav: NavController) {
    var result by remember { mutableStateOf<Installer.Result?>(null) }

    LaunchedEffect(Unit) {
        vm.prepareWorldPicker(vm.installInternational)
    }

    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) vm.attachSafTree(uri) }

    val roots: List<InstallRoot> = buildList {
        addAll(vm.installRoots)
        vm.safRoot?.let { saf -> if (none { it.worldsDir.uriString == saf.worldsDir.uriString }) add(saf) }
    }

    Scaffold(
        topBar = {
            FbTopBar(
                title = if (vm.installInternational) "国际版世界列表（选择载入哪里）" else "中国版世界列表（选择载入哪里）",
                onBack = { nav.popBackStack() },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F7F7)),
        ) {
            if (vm.installing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (vm.installInternational) {
                    items(roots.size) { index ->
                        val root = roots[index]
                        if (root.mojangDir != null) {
                            OutlinedButton(
                                onClick = {
                                    vm.installIntoGlobalBehaviorPacks(root) { r -> result = r }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("直接放到行为包列表（全局）")
                            }
                        }
                    }
                }

                if (roots.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (vm.installInternational) "未发现国际版存档目录" else "未发现中国版存档目录",
                                color = Color(0xFF666666),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Android 11 以上系统不允许直接读取 Android/data，\n" +
                                    "可以用下面的按钮手动选择目录。",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF999999),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                roots.forEach { root ->
                    item(key = "label-${root.label}") {
                        Text(
                            root.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        )
                    }
                    val worlds = root.worlds()
                    if (worlds.isEmpty()) {
                        item(key = "empty-${root.label}") {
                            Text(
                                "这个目录下没有存档",
                                fontSize = 11.sp,
                                color = Color(0xFF999999),
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    } else {
                        items(worlds.size, key = { "${root.label}-${worlds[it].id}" }) { wi ->
                            WorldRow(world = worlds[wi]) {
                                vm.installIntoWorld(worlds[wi]) { r -> result = r }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { treeLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("手动选择目录（SAF）")
                    }
                    Text(
                        "在系统文件选择器里选 com.mojang、minecraftWorlds 或某个存档文件夹都行。",
                        fontSize = 10.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    result?.let { r ->
        FbDialog(
            title = if (r.ok) "装载成功" else "装载失败",
            message = r.message,
            buttons = listOf(
                DlgButton("确定", emphasis = true) {
                    result = null
                    if (r.ok) {
                        // 回到 function 列表，方便继续编辑
                        nav.popBackStack(Routes.FUNCTIONS, inclusive = false)
                    }
                },
            ),
            onDismiss = { result = null },
        )
    }
}

@Composable
private fun WorldRow(world: WorldEntry, onClick: () -> Unit) {
    val icon: ImageBitmap? = rememberImage(world.iconFile)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(Color(0xFFEDEDED), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.world_icon),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    world.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(world.id, fontSize = 10.sp, color = Color(0xFF999999))
            }
            Text("装载 →", fontSize = 12.sp, color = Color(0xFF1565C0))
        }
    }
}
