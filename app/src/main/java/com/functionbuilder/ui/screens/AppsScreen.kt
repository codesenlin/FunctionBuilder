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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.R
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbTopBar
import com.functionbuilder.util.Intents

private data class RecommendedApp(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val primaryLabel: String,
    val primaryAction: (android.content.Context) -> Unit,
    val secondaryLabel: String? = null,
    val secondaryAction: ((android.content.Context) -> Unit)? = null,
)

/**
 * 推荐应用列表。
 * 对应旧版 `appget.iyu`（从抽屉的“更多实用应用”进入）。
 */
@Composable
fun AppsScreen(nav: NavController) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<RecommendedApp?>(null) }

    val apps = listOf(
        RecommendedApp(
            iconRes = R.drawable.apps_commander,
            title = "命令助手",
            subtitle = "作者：Xero · QQ 814518615",
            description = "可以帮助你学习和补全指令。",
            primaryLabel = "前往下载网站",
            primaryAction = { ctx -> Intents.openUrl(ctx, "https://projectxero.gitee.io/ca/") },
            secondaryLabel = "反馈群",
            secondaryAction = { ctx -> Intents.openQqGroup(ctx, "303697689") },
        ),
        RecommendedApp(
            iconRes = R.drawable.apps_btr,
            title = "Blocktopograph R",
            subtitle = "作者：_LaserBeamCat_",
            description = "可以修改世界 NBT。",
            primaryLabel = "直接下载",
            primaryAction = { ctx -> Intents.openUrl(ctx, "https://www.lanzous.com/i3ug8ob") },
        ),
    )

    Scaffold(
        topBar = { FbTopBar(title = "推荐应用", onBack = { nav.popBackStack() }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F7F7)),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            apps.forEach { app ->
                item(key = app.title) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = app },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(app.iconRes),
                                    contentDescription = app.title,
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(app.subtitle, fontSize = 11.sp, color = Color(0xFF888888))
                                Spacer(Modifier.height(2.dp))
                                Text(app.description, fontSize = 12.sp, color = Color(0xFF555555))
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { app ->
        FbDialog(
            title = app.title,
            message = "${app.subtitle}\n\n${app.description}",
            buttons = buildList {
                if (app.secondaryLabel != null && app.secondaryAction != null) {
                    add(DlgButton(app.secondaryLabel) {
                        selected = null
                        app.secondaryAction.invoke(context)
                    })
                }
                add(DlgButton(app.primaryLabel, emphasis = true) {
                    selected = null
                    app.primaryAction(context)
                })
                add(DlgButton("取消") { selected = null })
            },
            onDismiss = { selected = null },
        )
    }
}
