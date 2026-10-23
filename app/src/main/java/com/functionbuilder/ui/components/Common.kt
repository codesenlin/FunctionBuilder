package com.functionbuilder.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.functionbuilder.ui.theme.FbBlue
import java.io.File

/**
 * 统一的蓝色标题栏，沿用原版 `#FF008AFF`。
 *
 * 没有用 Material3 的 TopAppBar：它固定 64dp，加上状态栏内边距会显得又高又空。
 * 这里自己排，蓝色一直铺到状态栏后面（沉浸式），条体压到 54dp。
 */
@Composable
fun FbTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Surface(
        color = FbBlue,
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(54.dp)
                .padding(horizontal = 4.dp),
        ) {
            // 左侧：返回键或抽屉键
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    onBack != null -> IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    leading != null -> leading()
                }
            }

            // 右侧操作
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }

            // 标题：相对整条栏居中，两侧留出图标宽度避免压字
            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
            )
        }
    }
}

data class DlgButton(
    val label: String,
    val emphasis: Boolean = false,
    val onClick: () -> Unit,
)

/** 通用对话框：支持 1~3 个按钮，替代旧版的 `utw()`。 */
@Composable
fun FbDialog(
    title: String,
    message: String? = null,
    buttons: List<DlgButton>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            if (message.isNullOrBlank()) {
                Text(" ", fontSize = 1.sp)
            } else {
                Text(message, lineHeight = 20.sp)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                buttons.forEach { btn ->
                    TextButton(onClick = btn.onClick) {
                        Text(
                            btn.label,
                            color = if (btn.emphasis) FbBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (btn.emphasis) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
    )
}

/** 单输入框对话框。 */
@Composable
fun FbInputDialog(
    title: String,
    label: String,
    initial: String = "",
    confirmLabel: String = "确定",
    dismissLabel: String = "取消",
    keyboardType: KeyboardType = KeyboardType.Text,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(dismissLabel) }
                    TextButton(onClick = { onConfirm(value.trim()) }) {
                        Text(confirmLabel, color = FbBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 解码本地图片并按需缩放，避免直接把几 MB 的封面解成超大 Bitmap。
 * 原版封面就是包目录下的 `pack_icon.png`。
 */
fun decodeSampled(path: String, targetPx: Int = 256): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0) return@runCatching null

    var sample = 1
    while (bounds.outWidth / sample > targetPx * 2 && bounds.outHeight / sample > targetPx * 2) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
}.getOrNull()

@Composable
fun rememberImage(file: File?): ImageBitmap? =
    remember(file?.absolutePath, file?.lastModified()) {
        if (file != null && file.isFile) decodeSampled(file.absolutePath) else null
    }

/** 作品封面：文件存在就用文件，否则用传入的默认图标。 */
@Composable
fun PackIcon(
    icon: File?,
    fallbackRes: Int,
    sizeDp: Int = 44,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberImage(icon)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(sizeDp.dp),
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(sizeDp.dp),
        )
    }
}

/** 全屏背景图 + 半透明白色蒙层，保持原版“世界截图当背景”的观感。 */
@Composable
fun BackgroundImage(resId: Int, scrimAlpha: Float = 0.82f, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().then(Modifier)) {
            Surface(
                color = Color.White.copy(alpha = scrimAlpha),
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
        }
    }
}

val MonoStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 14.sp,
)
