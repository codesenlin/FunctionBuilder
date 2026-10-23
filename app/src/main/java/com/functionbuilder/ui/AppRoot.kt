package com.functionbuilder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.functionbuilder.data.MinecraftAccess
import com.functionbuilder.ui.screens.AppsScreen
import com.functionbuilder.ui.screens.BatchScreen
import com.functionbuilder.ui.screens.EditorScreen
import com.functionbuilder.ui.screens.FunctionsScreen
import com.functionbuilder.ui.screens.PackListScreen
import com.functionbuilder.ui.screens.PackMetaScreen
import com.functionbuilder.ui.screens.SettingsScreen
import com.functionbuilder.ui.screens.WorldPickerScreen

@Composable
fun AppRoot() {
    val vm: FbViewModel = viewModel()
    val nav = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { DocFsHolder.init(context) }

    // Android 10 及以下需要运行时存储权限，拿到之后再读作品目录。
    var ready by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { ready = true }

    LaunchedEffect(Unit) {
        val needed = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ).filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        } else {
            emptyList()
        }
        if (needed.isEmpty()) ready = true else permissionLauncher.launch(needed.toTypedArray())
    }

    LaunchedEffect(ready) {
        if (ready) vm.bootstrap()
    }

    val message = vm.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // 关键：内边距交给每个页面自己的 FbTopBar / Scaffold 处理，
        // 这里再来一次就会变成“白色状态栏 + 一坨蓝”。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.PACKS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.PACKS) { PackListScreen(vm, nav) }
            composable(Routes.FUNCTIONS) { FunctionsScreen(vm, nav) }
            composable(Routes.EDITOR) { EditorScreen(vm, nav) }
            composable(Routes.PACK_META) { PackMetaScreen(vm, nav) }
            composable(Routes.WORLDS) { WorldPickerScreen(vm, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
            composable(Routes.BATCH) { BatchScreen(vm, nav) }
            composable(Routes.APPS) { AppsScreen(nav) }
        }
    }
}

/** 把 Application Context 交给 DocumentFile 封装使用。 */
private object DocFsHolder {
    fun init(context: android.content.Context) {
        com.functionbuilder.util.DocFs.appContext = context.applicationContext
        // 顺手预热一次中国版目录扫描，避免第一次点“装载”时卡一下
        runCatching { MinecraftAccess.chineseWorldDirs() }
    }
}
