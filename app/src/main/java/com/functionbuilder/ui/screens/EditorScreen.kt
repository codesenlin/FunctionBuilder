package com.functionbuilder.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.Routes
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbInputDialog
import com.functionbuilder.ui.components.FbTopBar
import com.functionbuilder.ui.theme.FbPink

/**
 * 指令编辑器。
 * 对应旧版 `add.iyu`：保存 / 重命名 / 删除 / 行号 / 命令高亮 / 批量生成入口。
 */
@Composable
fun EditorScreen(vm: FbViewModel, nav: NavController) {
    val file = vm.currentFile
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val settings = vm.settings

    if (file == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    BackHandler(enabled = vm.editorDirty) { showExit = true }

    val lineCount = remember(vm.editorText) { vm.editorText.count { it == '\n' } + 1 }

    Scaffold(
        topBar = {
            FbTopBar(
                title = vm.fnRepo.displayName(file) + if (vm.editorDirty) " *" else "",
                onBack = {
                    if (vm.editorDirty) showExit = true else nav.popBackStack()
                },
                actions = {
                    IconButton(onClick = {
                        vm.saveEditor { err -> vm.toast(err ?: "保存成功") }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = {
                                    menuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除", color = Color(0xFFD32F2F)) },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF7F7F7)),
        ) {
            // ── 工具箱 / 显示选项 ──────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFDE7))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { nav.navigate(Routes.BATCH) }) {
                    Text("批量生成", fontSize = 13.sp, color = Color(0xFF1565C0))
                }
                ToggleChip("命令高亮", settings.htmlHighlight) {
                    vm.updateSettings(settings.copy(htmlHighlight = it))
                }
                ToggleChip("行号", settings.showLineNumbers) {
                    vm.updateSettings(settings.copy(showLineNumbers = it))
                }
            }
            HorizontalDivider()

            Row(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = 6.dp),
            ) {
                if (settings.showLineNumbers) {
                    LineNumberGutter(lineCount)
                }
                BasicTextField(
                    value = vm.editorText,
                    onValueChange = { vm.updateEditor(it) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF212121),
                    ),
                    cursorBrush = SolidColor(Color(0xFF1565C0)),
                    visualTransformation = if (settings.htmlHighlight) {
                        CommandHighlightTransformation
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    decorationBox = { inner ->
                        if (vm.editorText.isEmpty()) {
                            Text(
                                "在这里一行写一条指令，不用写 /",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = Color(0xFFAAAAAA),
                            )
                        }
                        inner()
                    },
                )
            }
        }
    }

    // ── 对话框 ────────────────────────────────────────────
    if (showRename) {
        FbInputDialog(
            title = "重命名",
            label = "新的 function 名字",
            initial = vm.fnRepo.displayName(file),
            confirmLabel = "保存",
            onDismiss = { showRename = false },
            onConfirm = { name ->
                showRename = false
                vm.renameCurrentFile(
                    rawName = name,
                    onRenamed = { vm.toast("修改成功") },
                    onError = { vm.toast(it) },
                )
            },
        )
    }

    if (showDelete) {
        FbDialog(
            title = "你真的要删除吗",
            message = "function 这么可爱 QAQ\n删除后无法恢复。",
            buttons = listOf(
                DlgButton("算了QWQ") { showDelete = false },
                DlgButton("朕意已决", emphasis = true) {
                    showDelete = false
                    vm.deleteCurrentFile { err ->
                        if (err != null) {
                            vm.toast(err)
                        } else {
                            vm.toast("删除成功嘤嘤嘤")
                            nav.popBackStack()
                        }
                    }
                },
            ),
            onDismiss = { showDelete = false },
        )
    }

    if (showExit) {
        FbDialog(
            title = "还没保存",
            message = "这个 function 有未保存的修改。",
            buttons = listOf(
                DlgButton("继续编辑") { showExit = false },
                DlgButton("放弃修改") {
                    showExit = false
                    nav.popBackStack()
                },
                DlgButton("保存并退出", emphasis = true) {
                    vm.saveEditor { err ->
                        showExit = false
                        if (err != null) vm.toast(err) else nav.popBackStack()
                    }
                },
            ),
            onDismiss = { showExit = false },
        )
    }
}

/** 行号栏，行高必须和正文完全一致（20sp）。 */
@Composable
private fun LineNumberGutter(lineCount: Int) {
    Column(
        Modifier
            .width(34.dp)
            .padding(end = 4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = Color(0xFF9E9E9E),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
            )
        }
    }
}

@Composable
private fun ToggleChip(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .background(
                color = if (checked) Color(0xFFD6E9FF) else Color(0xFFEDEDED),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = "$label ${if (checked) "开" else "关"}",
            fontSize = 11.sp,
            color = if (checked) Color(0xFF1565C0) else Color(0xFF888888),
        )
    }
}

/**
 * 命令高亮：把每行第一个单词（指令名）染成原版的粉色 #FF0080。
 * 旧版是把整段文字拼成 HTML 再塞回 EditText，这里用 VisualTransformation 实现，
 * 既能高亮又不会破坏光标位置。
 */
private object CommandHighlightTransformation : VisualTransformation {

    /** 原版 `functions.bl()` 里那段指令表。 */
    private val COMMANDS: Set<String> = setOf(
        "alwaysday", "clear", "clone", "connect", "daylock", "deop", "difficulty", "difficultly",
        "effect", "enchant", "execute", "fill", "function", "gamemode", "gamerule", "give",
        "help", "kill", "list", "locate", "me", "mixer", "msg", "op", "particle", "playsound",
        "replaceitem", "say", "scoreboard", "setblock", "setmaxplayers", "setworldspawn",
        "spawnpoint", "spreadplayers", "stopsound", "summon", "tag", "teleport", "tell",
        "tellraw", "testfor", "testforblock",
        // 原版列表里 "telltellraw" 是笔误，这里补齐；title 原版漏了，但确实是指令
        "title", "titleraw", "time", "weather", "xp",
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        val raw = text.text
        var offset = 0
        for (line in raw.split("\n")) {
            val trimmed = line.trimStart()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val word = trimmed.takeWhile { it.isLetter() }
                if (word.isNotEmpty() && COMMANDS.contains(word.lowercase())) {
                    val start = offset + (line.length - trimmed.length)
                    builder.addStyle(
                        SpanStyle(color = FbPink, fontWeight = FontWeight.Bold),
                        start,
                        start + word.length,
                    )
                }
            }
            offset += line.length + 1
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
