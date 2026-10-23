package com.functionbuilder.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.util.Intents

/**
 * 帮助对话框。
 * 对应旧版 `left.iyu` 里“帮助”那一项的 `utw()` 文案。
 */
@Composable
fun HelpDialog(context: Context, onDismiss: () -> Unit) {
    var showMore by remember { mutableStateOf(false) }

    if (!showMore) {
        FbDialog(
            title = "/function",
            message = "function 原意为函数，其实就是自定义指令。利用它，你可以自定义一条指令，" +
                "然后通过 /function <你自定义的名称> 来使用，也可以用命令方块来执行。\n\n" +
                "怎么制作呢？十分简单，一行一行地把指令写下来，它们会在同一瞬间执行，" +
                "最多可达 10000 行。快来试试吧！\n      ——小傻羊驼",
            buttons = listOf(
                DlgButton("确定") { onDismiss() },
                DlgButton("更多帮助", emphasis = true) { showMore = true },
            ),
            onDismiss = onDismiss,
        )
    } else {
        FbDialog(
            title = "通往其他地方的大门",
            message = "function 视频教程：\nhttps://www.bilibili.com/video/av34418736\n\n" +
                "Mojang 的 Addons 官网：\nhttps://minecraft.net/en-us/addons\n\n" +
                "Addons Wiki（英文词条）：\nhttps://minecraft.gamepedia.com/Add-on\n" +
                "—— yangzii 提供",
            buttons = listOf(
                DlgButton("视频") {
                    Intents.openUrl(context, "https://www.bilibili.com/video/av34418736")
                },
                DlgButton("官网") {
                    Intents.openUrl(context, "https://minecraft.net/en-us/addons")
                },
                DlgButton("字条", emphasis = true) {
                    Intents.openUrl(context, "https://minecraft.gamepedia.com/Add-on")
                },
            ),
            onDismiss = onDismiss,
        )
    }
}
