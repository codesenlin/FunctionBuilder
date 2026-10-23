package com.functionbuilder.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.functionbuilder.data.AppSettings
import android.net.Uri
import com.functionbuilder.data.FnRepository
import com.functionbuilder.data.InstallRoot
import com.functionbuilder.data.Installer
import com.functionbuilder.data.MinecraftAccess
import com.functionbuilder.data.WorldEntry
import com.functionbuilder.data.PackInfo
import com.functionbuilder.data.PackManifest
import com.functionbuilder.data.PackRepository
import com.functionbuilder.data.SettingsStore
import com.functionbuilder.util.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * 全局状态：作品列表 / 当前作品 / 当前 function 文件 / 设置。
 *
 * 原版靠 iApp 的全局变量（`sss.xxx`）在页面之间传递状态，这里统一收敛到
 * 一个 ViewModel，页面只负责展示和触发动作。
 */
class FbViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app
    val packRepo = PackRepository(context)
    val fnRepo = FnRepository()

    // ── 设置 ──────────────────────────────────────────────
    var settings by mutableStateOf(AppSettings())
        private set

    /** 启动时是否需要弹更新日志（首装或版本号变化）。 */
    var showChangelogOnStart by mutableStateOf(false)
        private set

    // ── 作品 ──────────────────────────────────────────────
    var packs by mutableStateOf<List<PackInfo>>(emptyList())
        private set

    var currentPack by mutableStateOf<PackInfo?>(null)
        private set

    var loading by mutableStateOf(false)
        private set

    // ── function 文件 ─────────────────────────────────────
    var functions by mutableStateOf<List<File>>(emptyList())
        private set

    var currentFile by mutableStateOf<File?>(null)
        private set

    var editorText by mutableStateOf("")
        private set

    var editorDirty by mutableStateOf(false)
        private set

    // ── 一次性提示（Snackbar） ─────────────────────────────
    var message by mutableStateOf<String?>(null)
        private set

    fun toast(text: String) {
        message = text
    }

    fun consumeMessage() {
        message = null
    }

    // ── 初始化 ────────────────────────────────────────────
    fun bootstrap() {
        val loaded = SettingsStore.load(context)
        settings = loaded.settings
        showChangelogOnStart = loaded.firstRun
        refreshPacks()
    }

    fun dismissChangelog() {
        showChangelogOnStart = false
    }

    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        SettingsStore.save(context, newSettings)
    }

    /** 作品根目录，界面上要显示出来。 */
    fun packRoot(): File = Storage.packRoot(context)

    fun hasAllFilesAccess(): Boolean = Storage.hasAllFilesAccess(context)

    fun legacyDirBlocked(): Boolean = Storage.legacyBlocked(context)

    // ── 作品操作 ──────────────────────────────────────────
    fun refreshPacks() {
        viewModelScope.launch {
            loading = true
            packs = withContext(Dispatchers.IO) { packRepo.listPacks() }
            loading = false
        }
    }

    fun createPack(displayName: String, onDone: (error: String?) -> Unit = {}) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { packRepo.createPack(displayName) }
            result.onSuccess {
                refreshPacks()
                onDone(null)
            }.onFailure {
                onDone(it.message ?: "新建失败")
            }
        }
    }

    fun deletePack(pack: PackInfo, onDone: (error: String?) -> Unit = {}) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { packRepo.deletePack(pack.dir) }
            refreshPacks()
            onDone(if (ok) null else "删除失败，请检查存储权限")
        }
    }

    fun openPack(pack: PackInfo) {
        currentPack = pack
        refreshFunctions()
    }

    // ── function 文件操作 ─────────────────────────────────
    fun refreshFunctions() {
        val pack = currentPack ?: return
        viewModelScope.launch {
            functions = withContext(Dispatchers.IO) { fnRepo.listFunctions(pack.dir) }
        }
    }

    fun createFunction(rawName: String, onDone: (error: String?) -> Unit = {}) {
        val pack = currentPack ?: return onDone("没有选中作品")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                fnRepo.create(pack.dir, rawName).also { if (it.isSuccess) packRepo.touch(pack.dir) }
            }
            if (result.isSuccess) {
                refreshFunctions()
                onDone(null)
            } else {
                onDone(result.exceptionOrNull()?.message ?: "新建失败")
            }
        }
    }

    fun openFile(file: File) {
        currentFile = file
        editorText = fnRepo.read(file)
        editorDirty = false
    }

    fun updateEditor(text: String) {
        editorText = text
        editorDirty = true
    }

    fun saveEditor(onDone: (error: String?) -> Unit = {}) {
        val file = currentFile ?: return onDone("没有打开的文件")
        val pack = currentPack
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val written = fnRepo.write(file, editorText)
                if (written && pack != null) packRepo.touch(pack.dir)
                written
            }
            if (ok) {
                editorDirty = false
                refreshPacks()
                onDone(null)
            } else {
                onDone("保存失败，请检查存储权限")
            }
        }
    }

    fun renameCurrentFile(rawName: String, onRenamed: (File) -> Unit, onError: (String) -> Unit) {
        val file = currentFile ?: return onError("没有打开的文件")
        val pack = currentPack ?: return onError("没有选中作品")
        viewModelScope.launch {
            // 先落盘，避免重命名丢内容
            withContext(Dispatchers.IO) { fnRepo.write(file, editorText) }
            val result = withContext(Dispatchers.IO) {
                fnRepo.rename(pack.dir, file, rawName).also { if (it.isSuccess) packRepo.touch(pack.dir) }
            }
            result.onSuccess { renamed ->
                currentFile = renamed
                editorDirty = false
                refreshFunctions()
                refreshPacks()
                onRenamed(renamed)
            }.onFailure {
                onError(it.message ?: "重命名失败")
            }
        }
    }

    fun deleteCurrentFile(onDone: (error: String?) -> Unit = {}) {
        val file = currentFile ?: return onDone("没有打开的文件")
        val pack = currentPack
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val deleted = fnRepo.delete(file)
                if (deleted && pack != null) packRepo.touch(pack.dir)
                deleted
            }
            if (ok) {
                currentFile = null
                editorDirty = false
                refreshFunctions()
                refreshPacks()
                onDone(null)
            } else {
                onDone("删除失败")
            }
        }
    }

    // ── 作品信息（manifest.json） ──────────────────────────
    var manifest by mutableStateOf<JSONObject?>(null)
        private set

    fun loadManifest() {
        val pack = currentPack ?: return
        viewModelScope.launch {
            manifest = withContext(Dispatchers.IO) { packRepo.readManifest(pack.dir) }
        }
    }

    fun saveManifest(name: String, description: String, author: String, onDone: (error: String?) -> Unit) {
        val pack = currentPack ?: return onDone("没有选中作品")
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) {
                val obj = packRepo.readManifest(pack.dir) ?: JSONObject()
                PackManifest.withName(obj, name)
                PackManifest.withDescription(obj, description)
                PackManifest.withModuleDescription(obj, author)
                PackManifest.withNewUuids(obj)
                if (!packRepo.writeManifest(pack.dir, obj)) "保存失败，请检查存储权限" else null
            }
            if (error == null) {
                packRepo.touch(pack.dir)
                refreshPacks()
            }
            onDone(error)
        }
    }

    fun copyCoverImage(bytes: ByteArray, onDone: (error: String?) -> Unit) {
        val pack = currentPack ?: return onDone("没有选中作品")
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    File(pack.dir, "pack_icon.png").writeBytes(bytes)
                    true
                }.getOrDefault(false)
            }
            refreshPacks()
            onDone(if (ok) null else "更换封面失败")
        }
    }

    // ── 装载到游戏 ─────────────────────────────────────────
    var installInternational by mutableStateOf(true)
        private set

    var installRoots by mutableStateOf<List<InstallRoot>>(emptyList())
        private set

    /** 用户通过 SAF 选中的目录（Android 11+ 直接路径不可写时用）。 */
    var safRoot by mutableStateOf<InstallRoot?>(null)
        private set

    var installing by mutableStateOf(false)
        private set

    fun prepareWorldPicker(international: Boolean) {
        installInternational = international
        viewModelScope.launch {
            installRoots = withContext(Dispatchers.IO) { MinecraftAccess.directRoots(context) }
        }
    }

    fun attachSafTree(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        safRoot = MinecraftAccess.fromTreeUri(context, uri)
        if (safRoot == null) toast("无法读取所选目录")
    }

    fun installIntoWorld(world: WorldEntry, onDone: (Installer.Result) -> Unit) {
        val pack = currentPack ?: return onDone(Installer.Result.fail("没有选中作品"))
        viewModelScope.launch {
            installing = true
            val result = withContext(Dispatchers.IO) {
                Installer.installIntoWorld(pack.dir, world.node)
            }
            installing = false
            onDone(result)
        }
    }

    fun installIntoGlobalBehaviorPacks(root: InstallRoot, onDone: (Installer.Result) -> Unit) {
        val pack = currentPack ?: return onDone(Installer.Result.fail("没有选中作品"))
        val mojang = root.mojangDir
        if (mojang == null) {
            return onDone(Installer.Result.fail("这个目录里找不到 com.mojang"))
        }
        viewModelScope.launch {
            installing = true
            val result = withContext(Dispatchers.IO) {
                Installer.installIntoGlobalBehaviorPacks(pack.dir, mojang)
            }
            installing = false
            onDone(result)
        }
    }
}
