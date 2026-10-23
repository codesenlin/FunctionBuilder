package com.functionbuilder.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.functionbuilder.data.BatchEngine
import com.functionbuilder.ui.FbViewModel
import com.functionbuilder.ui.components.DlgButton
import com.functionbuilder.ui.components.FbDialog
import com.functionbuilder.ui.components.FbTopBar

private data class VarRow(
    val placeholder: String,
    val expression: String,
    val initial: String,
)

/**
 * 批量生成。
 * 对应旧版 `many.iyu` 的“添加变量 + 尝试运行”，算法语义见 [BatchEngine]。
 */
@Composable
fun BatchScreen(vm: FbViewModel, nav: NavController) {
    var body by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("10") }
    val rows = remember { mutableStateListOf<VarRow>() }
    var result by remember { mutableStateOf<BatchEngine.BatchResult?>(null) }
    var showResult by remember { mutableStateOf(false) }

    fun usedPlaceholders() = rows.map { it.placeholder }.toSet()

    fun nextPlaceholder(): String? =
        BatchEngine.PLACEHOLDERS.firstOrNull { it !in usedPlaceholders() }

    Scaffold(
        topBar = {
            FbTopBar(
                title = "批量生成",
                onBack = { nav.popBackStack() },
                actions = {
                    IconButton(onClick = {
                        nextPlaceholder()?.let { rows.add(VarRow(it, "x", "0")) }
                            ?: vm.toast("大哥，拼音不够用啦")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "添加变量")
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
                .padding(12.dp),
        ) {
            Text(
                "命令体里用下面这些元音当占位符，第 i 条会把每个占位符替换成\n" +
                    "「代数式」在 x = 初始值 + i 处的取值。",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFF777777),
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("命令体（不写 /）") },
                placeholder = { Text("say 第ā条") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                minLines = 2,
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = count,
                onValueChange = { count = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("生成条数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("变量", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text("共 ${rows.size} 个", fontSize = 11.sp, color = Color(0xFF999999))
            }
            Spacer(Modifier.height(4.dp))

            if (rows.isEmpty()) {
                Text(
                    "还没有变量，点右上角的 + 添加一个",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            rows.forEachIndexed { index, row ->
                VariableCard(
                    row = row,
                    onExpressionChange = { value -> rows[index] = rows[index].copy(expression = value) },
                    onInitialChange = { value ->
                        rows[index] = rows[index].copy(initial = value.filter { it.isDigit() || it == '-' })
                    },
                    onRemove = { rows.removeAt(index) },
                    onCopyPlaceholder = { vm.toast("已复制到剪贴板：${rows[index].placeholder}") },
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val parsed = rows.map {
                        BatchEngine.Variable(
                            placeholder = it.placeholder,
                            expression = it.expression.ifBlank { "x" },
                            initial = it.initial.toIntOrNull() ?: 0,
                        )
                    }
                    result = BatchEngine.generate(body, count.toIntOrNull() ?: 0, parsed)
                    showResult = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("尝试运行")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showResult) {
        val current = result
        val ok = current?.ok == true && current.lines.isNotEmpty()
        FbDialog(
            title = if (ok) "运行结果（${current.lines.size} 条）" else "运行结果",
            message = if (ok) current.lines.joinToString("\n") else (current?.error ?: "没有结果"),
            buttons = buildList {
                add(DlgButton("关闭") { showResult = false })
                if (ok) {
                    add(
                        DlgButton("插入到编辑器", emphasis = true) {
                            val generated = current.lines.joinToString("\n")
                            val merged = if (vm.editorText.isBlank()) generated
                            else vm.editorText.trimEnd() + "\n" + generated
                            vm.updateEditor(merged)
                            showResult = false
                            nav.popBackStack()
                        },
                    )
                }
            },
            onDismiss = { showResult = false },
        )
    }
}

@Composable
private fun VariableCard(
    row: VarRow,
    onExpressionChange: (String) -> Unit,
    onInitialChange: (String) -> Unit,
    onRemove: () -> Unit,
    onCopyPlaceholder: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                Box(
                    Modifier
                        .background(Color(0xFFFFF9C4), RoundedCornerShape(6.dp))
                        .clickable {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(row.placeholder))
                            onCopyPlaceholder()
                        }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "占位符 ${row.placeholder}（点击复制）",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "移除",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF999999),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.expression,
                    onValueChange = onExpressionChange,
                    label = { Text("代数式") },
                    placeholder = { Text("x*2") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = row.initial,
                    onValueChange = onInitialChange,
                    label = { Text("初始值") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp),
                )
            }
        }
    }
}
