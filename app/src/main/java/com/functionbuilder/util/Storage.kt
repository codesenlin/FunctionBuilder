package com.functionbuilder.util

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * 路径与权限。
 *
 * 原 iApp 版本的路径约定（`%` 代表 `/sdcard`）：
 *  - 作品根目录：`/sdcard/Android/functiontested`
 *  - 设置文件   ：`/sdcard/Android/functionbuilderin`（格式 `0.50#1#0#1`）
 *  - 国际版存档 ：`/sdcard/games/com.mojang/minecraftWorlds`
 *  - 中国版存档 ：`/sdcard/Android/data/<中国版包名>/files/minecraftWorlds`
 *
 * 为了和旧版数据互通，只要能拿到“所有文件访问权限”，就继续沿用旧路径；
 * 否则退回到应用自己的外部目录。
 */
object Storage {

    const val SETTINGS_VERSION = "0.50"

    fun externalRoot(): File = Environment.getExternalStorageDirectory()

    /** Android 11+ 是否已授予“所有文件访问权限”。 */
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * 旧版共享目录是否可直接读写。
     * API 29 及以下靠 READ/WRITE_EXTERNAL_STORAGE + requestLegacyExternalStorage；
     * API 30+ 必须要有 MANAGE_EXTERNAL_STORAGE。
     */
    fun canUseLegacyDirs(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return hasAllFilesAccess(context)
        // API 24~29：真正试着建一次目录，比只看版本号可靠（用户可能拒绝了存储权限）。
        return runCatching {
            val probe = File(externalRoot(), "Android/functiontested")
            (probe.isDirectory || probe.mkdirs()) && probe.canWrite()
        }.getOrDefault(false)
    }

    /**
     * 旧版共享目录能不能真的用。
     *
     * 光有 MANAGE_EXTERNAL_STORAGE 还不够：如果 `/sdcard/Android/functiontested`
     * 是**另一次安装**（别的 UID）创建的，目录权限是 `drwxrws---`，
     * 新安装的 UID 依然会被 POSIX 权限挡住——强行用只会得到一个永远为空的列表。
     * 所以这里实际探测一次可读可写。
     */
    private fun legacyUsable(legacy: File): Boolean = runCatching {
        (legacy.isDirectory || legacy.mkdirs()) && legacy.canRead() && legacy.canWrite() &&
            legacy.listFiles() != null
    }.getOrDefault(false)

    /** 作品根目录。 */
    fun packRoot(context: Context): File {
        val legacy = File(externalRoot(), "Android/functiontested")
        if (canUseLegacyDirs(context) && legacyUsable(legacy)) return legacy

        val fallback = File(context.getExternalFilesDir(null) ?: context.filesDir, "functiontested")
        if (!fallback.isDirectory) fallback.mkdirs()
        return fallback
    }

    /**
     * 旧的共享目录存在但当前安装没有权限（通常是被上一次安装的 UID 占着）。
     * 界面用它给用户一句解释，而不是默默显示空列表。
     */
    fun legacyBlocked(context: Context): Boolean {
        if (!canUseLegacyDirs(context)) return false
        val legacy = File(externalRoot(), "Android/functiontested")
        return legacy.exists() && !legacyUsable(legacy)
    }

    /** 设置文件（`0.50#1#0#1`）。 */
    fun settingsFile(context: Context): File {
        val legacy = File(externalRoot(), "Android/functionbuilderin")
        return if (canUseLegacyDirs(context)) legacy
        else File(context.filesDir, "functionbuilderin")
    }

    /** 与旧版一致的解压临时目录：/sdcard/Android/functiontestin */
    fun unzipStagingDir(context: Context): File =
        File(if (canUseLegacyDirs(context)) File(externalRoot(), "Android")
        else context.cacheDir, "functiontestin")

    /**
     * 国际版 com.mojang 目录的候选位置（按优先级）。
     * 注意：API 30+ 下 `Android/data/com.mojang.minecraftpe` 即使有全盘权限也读不到，
     * 需要在界面上引导用户用 SAF 选目录。
     */
    fun internationalCandidates(): List<File> = listOf(
        File(externalRoot(), "games/com.mojang"),
        File(externalRoot(), "Android/data/com.mojang.minecraftpe/files/games/com.mojang"),
    )

    /**
     * 扫描中国版（网易）存档根目录：`.../files/minecraftWorlds` 存在的那些包。
     * 旧版是遍历已安装应用列表按名字匹配，这里直接扫 Android/data 更稳。
     */
    fun chineseCandidates(): List<File> {
        val dataDir = File(externalRoot(), "Android/data")
        val packages = dataDir.listFiles() ?: return emptyList()
        return packages.mapNotNull { pkg ->
            val worlds = File(pkg, "files/minecraftWorlds")
            if (worlds.isDirectory) worlds else null
        }.sortedBy { it.parentFile?.name ?: "" }
    }

    /** 世界（存档）目录列表，用于“装载到游戏”。 */
    fun worldsIn(mojangDir: File): File = File(mojangDir, "minecraftWorlds")
}
