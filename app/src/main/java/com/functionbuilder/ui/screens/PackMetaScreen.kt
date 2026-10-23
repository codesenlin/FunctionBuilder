package com.functionbuilder.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.R
import com.functionbuilder.data.PackManifest
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.Routes
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbTopBar
import com.functionbuilder.ui.components.decodeSampled

/**
 * 作品信息页。
 * 对应旧版 `endin.iyu`：换封面、改名称/简介/作者、保存、装载到游戏。
 */
@Composable
fun PackMetaScreen(vm: FbViewModel, nav: NavController) {
    val context = LocalContext.current
    val pack = vm.currentPack

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var showInstallChoice by remember { mutableStateOf(false) }

    LaunchedEffect(pack?.dir?.absolutePath) {
        vm.loadManifest()
        preview = pack?.icon?.let { decodeSampled(it.absolutePath, 512) }
    }

    LaunchedEffect(vm.manifest) {
        vm.manifest?.let { obj ->
            name = PackManifest.headerName(obj)
            description = PackManifest.headerDescription(obj)
            author = PackManifest.moduleDescription(obj)
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null) {
                vm.toast("读取图片失败")
            } else {
                val bitmap = runCatching {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }.getOrNull()
                if (bitmap == null) {
                    vm.toast("错误：这不是图片")
                } else {
                    preview = bitmap
                    vm.copyCoverImage(bytes) { err -> vm.toast(err ?: "更换封面成功") }
                }
            }
        }
    }

    if (pack == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    fun save(onSaved: () -> Unit) {
        if (name.isBlank()) {
            vm.toast("请输入名称")
            return
        }
        if (description.isBlank()) {
            vm.toast("请输入简介")
            return
        }
        if (author.isBlank()) {
            vm.toast("请输入作者")
            return
        }
        vm.saveManifest(name, description, author) { err ->
            if (err != null) {
                vm.toast(err)
            } else {
                vm.toast("保存成功")
                onSaved()
            }
        }
    }

    Scaffold(
        topBar = {
            FbTopBar(
                title = "作品信息",
                onBack = { nav.popBackStack() },
                actions = {
                    TextButton(onClick = {
                        save { nav.popBackStack() }
                    }) {
                        Text("保存", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F7F7))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── 封面 ────────────────────────────────────
            Box(
                Modifier
                    .size(120.dp)
                    .background(Color(0xFFE8E8E8), RoundedCornerShape(16.dp))
                    .clickable { pickImage.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview!!,
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent, RoundedCornerShape(16.dp)),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.pack_icon),
                        contentDescription = "封面",
                        modifier = Modifier.size(96.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("点击上方更改封面", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("建议 256×256 的 PNG", fontSize = 10.sp, color = Color(0xFF999999))

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("function 名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("function 简介") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("function 作者") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { save { } },
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
                Button(
                    onClick = { save { showInstallChoice = true } },
                    modifier = Modifier.weight(1f),
                ) { Text("装载到游戏") }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "装载会把 functions/、manifest.json、pack_icon.png 复制进存档的 " +
                    "behavior_packs/functiontestin，并把 pack_id 合并进 world_behavior_packs.json。",
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = Color(0xFF999999),
            )
        }
    }

    if (showInstallChoice) {
        FbDialog(
            title = "请选择装载到哪里",
            message = "也可以手动装载：作品目录里就是完整的 behavior pack。",
            buttons = listOf(
                DlgButton("国际版") {
                    showInstallChoice = false
                    vm.prepareWorldPicker(true)
                    nav.navigate(Routes.WORLDS)
                },
                DlgButton("中国版") {
                    showInstallChoice = false
                    vm.prepareWorldPicker(false)
                    nav.navigate(Routes.WORLDS)
                },
                DlgButton("取消") { showInstallChoice = false },
            ),
            onDismiss = { showInstallChoice = false },
        )
    }
}
