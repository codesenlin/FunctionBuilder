package com.functionbuilder.data

import android.content.Context
import com.functionbuilder.util.Storage
import java.io.File

/**
 * 设置项的持久化。
 *
 * 完全沿用旧版格式：`版本#检查更新#html高亮#显示行数`，例如 `0.50#1#0#1`。
 * 这样旧版写的 `/sdcard/Android/functionbuilderin` 可以直接读出来。
 */
data class AppSettings(
    val checkUpdate: Boolean = true,
    val htmlHighlight: Boolean = false,
    val showLineNumbers: Boolean = true,
) {
    fun serialize(version: String = Storage.SETTINGS_VERSION): String = listOf(
        version,
        if (checkUpdate) "1" else "0",
        if (htmlHighlight) "1" else "0",
        if (showLineNumbers) "1" else "0",
    ).joinToString("#")
}

object SettingsStore {

    data class Loaded(val settings: AppSettings, val firstRun: Boolean, val rawVersion: String)

    fun load(context: Context): Loaded {
        val file: File = Storage.settingsFile(context)
        if (!file.exists()) {
            val defaults = AppSettings()
            writeRaw(context, defaults.serialize())
            return Loaded(defaults, firstRun = true, rawVersion = Storage.SETTINGS_VERSION)
        }

        val raw = runCatching { file.readText() }.getOrDefault("")
        val parts = raw.split("#")
        if (parts.size < 4) {
            val defaults = AppSettings()
            writeRaw(context, defaults.serialize())
            return Loaded(defaults, firstRun = false, rawVersion = Storage.SETTINGS_VERSION)
        }

        val settings = AppSettings(
            checkUpdate = parts[1] == "1",
            htmlHighlight = parts[2] == "1",
            showLineNumbers = parts[3] != "0",
        )
        val version = parts[0]

        // 旧版逻辑：版本号不一致就写回新版本号并再次弹更新日志。
        if (version != Storage.SETTINGS_VERSION) {
            writeRaw(context, settings.serialize())
            return Loaded(settings, firstRun = true, rawVersion = version)
        }
        return Loaded(settings, firstRun = false, rawVersion = version)
    }

    fun save(context: Context, settings: AppSettings) {
        writeRaw(context, settings.serialize())
    }

    private fun writeRaw(context: Context, text: String) {
        runCatching {
            val file = Storage.settingsFile(context)
            file.parentFile?.mkdirs()
            file.writeText(text)
        }
    }
}
