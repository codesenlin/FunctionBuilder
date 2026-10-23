package com.functionbuilder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 原 iApp 版本的主色：标题栏 #FF008AFF、强调色 #FFFFA800 / #FFFFF785 */
val FbBlue = Color(0xFF008AFF)
val FbBlueDark = Color(0xFF0066CC)
val FbYellow = Color(0xFFFFF785)
val FbOrange = Color(0xFFFFA800)
val FbPaper = Color(0xFFFFEC00)
val FbPink = Color(0xFFFF0080)
val FbGreen = Color(0xFF00FFD3)

private val LightColors = lightColorScheme(
    primary = FbBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = FbOrange,
    onSecondary = Color.Black,
    tertiary = FbGreen,
    background = Color(0xFFF6F6F6),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF444444),
    error = Color(0xFFD32F2F),
)

@Composable
fun FunctionBuilderTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // 原版是纯浅色界面，这里保持一致，避免深色模式下截图与原版差异过大。
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content,
    )
}
