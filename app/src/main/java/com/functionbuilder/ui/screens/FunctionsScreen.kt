package com.functionbuilder.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.R
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.Routes
import com.functionbuilder.ui.components.FbInputDialog
import com.functionbuilder.ui.components.FbTopBar

/**
 * 某个作品的 function 列表。
 * 对应旧版 `look.iyu` + `functions.looking()`。
 */
@Composable
fun FunctionsScreen(vm: FbViewModel, nav: NavController) {
    val pack = vm.currentPack
    var showNewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pack?.dir?.absolutePath) { vm.refreshFunctions() }

    if (pack == null) {
        // 正常情况下进不来，兜底避免空指针
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    Scaffold(
        topBar = {
            FbTopBar(
                title = "${pack.name}的function列表",
                onBack = { nav.popBackStack() },
                actions = {
                    IconButton(onClick = { showNewDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.logo_add),
                            contentDescription = "新建 function",
                            tint = Color.Unspecified,
                        )
                    }
                    IconButton(onClick = { nav.navigate(Routes.PACK_META) }) {
                        Icon(
                            painter = painterResource(R.drawable.logo_seting),
                            contentDescription = "设置和装载",
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
            if (vm.functions.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("这个作品还没有 function 指令", color = Color(0xFF666666))
                    Spacer(Modifier.height(6.dp))
                    Text("点击右上角的 + 新建一条", fontSize = 12.sp, color = Color(0xFF999999))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(vm.functions, key = { _, f -> f.absolutePath }) { index, file ->
                        FunctionRow(
                            index = index + 1,
                            name = vm.fnRepo.displayName(file),
                            onClick = {
                                vm.openFile(file)
                                nav.navigate(Routes.EDITOR)
                            },
                        )
                    }
                    item {
                        Text(
                            "共 ${vm.functions.size} 条 · 游戏里用 /function <名字> 执行",
                            fontSize = 10.sp,
                            color = Color(0xFF9A9A9A),
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        FbInputDialog(
            title = "命名",
            label = "你使用 function 指令启动的名字",
            confirmLabel = "新建",
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                if (name.isEmpty()) {
                    vm.toast("亲，你什么都没写")
                } else {
                    showNewDialog = false
                    vm.createFunction(name) { error ->
                        if (error != null) vm.toast(error) else vm.toast("新建成功")
                    }
                }
            },
        )
    }
}

@Composable
private fun FunctionRow(index: Int, name: String, onClick: () -> Unit) {
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
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 原版的条目文字就是 “/function 名字”，保留这个观感
            Text(
                text = "/function $name",
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1565C0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text("#$index", fontSize = 10.sp, color = Color(0xFFBBBBBB))
        }
    }
}
