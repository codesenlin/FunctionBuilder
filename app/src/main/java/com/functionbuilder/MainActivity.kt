package com.functionbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.functionbuilder.ui.AppRoot
import com.functionbuilder.ui.theme.FunctionBuilderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 标题栏是蓝底，状态栏图标必须用浅色，否则看不见
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            FunctionBuilderTheme {
                AppRoot()
            }
        }
    }
}
