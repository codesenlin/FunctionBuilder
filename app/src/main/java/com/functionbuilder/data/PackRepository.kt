package com.functionbuilder.data

import android.content.Context
import com.functionbuilder.util.FileFs
import com.functionbuilder.util.Storage
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

/** 作品（行为包）信息。 */
data class PackInfo(
    val dir: File,
    val name: String,
    val icon: File?,
    val modifiedRaw: String,
) {
    val dirName: String get() = dir.name

    /** 卡片上显示的“上次修改时间”，旧版会把日期和时间的空格换成换行。 */
    val modifiedDisplay: String
        get() = modifiedRaw.replaceFirst(" ", "\n")
}

/**
 * 行为包模板（assets/functiontestin.zip）的解压。
 * 抽出来是为了能在 JVM 单元测试里直接验证（不需要真机）。
 */
object PackTemplate {
    const val ASSET_NAME = "functiontestin.zip"
    const val ROOT_DIR = "functiontestin"

    /** 解压到 [destParent]，最终会得到 destParent/functiontestin/... */
    fun extract(input: java.io.InputStream, destParent: File): Boolean = runCatching {
        destParent.mkdirs()
        java.util.zip.ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val outFile = File(destParent, entry.name)
                // 防 zip slip：条目必须落在目标目录内
                if (!outFile.canonicalPath.startsWith(destParent.canonicalPath + File.separator) &&
                    outFile.canonicalPath != destParent.canonicalPath
                ) {
                    zip.closeEntry()
                    continue
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { os -> zip.copyTo(os) }
                }
                zip.closeEntry()
            }
        }
        File(destParent, ROOT_DIR).isDirectory
    }.getOrDefault(false)
}

/**
 * 作品仓库：对应旧版 `src/functions.myu` 里的 `funall()` / 新建 / 删除逻辑。
 * 数据目录仍然是 `Android/functiontested`，和旧版完全兼容。
 */
class PackRepository(private val context: Context) {

    val root: File get() = Storage.packRoot(context)

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun listPacks(): List<PackInfo> {
        val dirs = root.listFiles()?.filter { it.isDirectory } ?: emptyList()
        return dirs.map { dir ->
            val manifest = readManifest(dir)
            PackInfo(
                dir = dir,
                name = PackManifest.headerName(manifest).ifBlank { dir.name },
                icon = File(dir, "pack_icon.png").takeIf { it.isFile },
                modifiedRaw = modifiedText(dir),
            )
        }.sortedByDescending { it.dir.lastModified() }
    }

    fun readManifest(packDir: File): JSONObject? {
        val file = File(packDir, "manifest.json")
        if (!file.isFile) return null
        return PackManifest.parse(runCatching { file.readText() }.getOrDefault(""))
    }

    fun writeManifest(packDir: File, manifest: JSONObject): Boolean = runCatching {
        PackManifest.withUuidIfMissing(manifest)
        File(packDir, "manifest.json").writeText(PackManifest.pretty(manifest))
        true
    }.getOrDefault(false)

    fun modifiedText(packDir: File): String {
        val file = File(packDir, "test/time")
        val raw = runCatching { file.readText().trim() }.getOrNull()
        if (!raw.isNullOrBlank()) return raw
        return timeFormat.format(Date(packDir.lastModified()))
    }

    fun touch(packDir: File) {
        runCatching {
            val file = File(packDir, "test/time")
            file.parentFile?.mkdirs()
            file.writeText(timeFormat.format(Date()))
        }
    }

    /**
     * 新建作品：把 assets 里的 `functiontestin.zip` 模板解压到作品目录。
     * 对应旧版 `funall.iyu` 中“新建”按钮的逻辑。
     */
    fun createPack(displayName: String): Result<PackInfo> = runCatching {
        val staging = Storage.unzipStagingDir(context)
        FileFs.deleteRecursively(staging)

        val stagingParent = staging.parentFile ?: error("无法确定解压目录")
        val extracted = context.assets.open(PackTemplate.ASSET_NAME).use { input ->
            PackTemplate.extract(input, stagingParent)
        }
        if (!extracted || !staging.isDirectory) error("模板解压失败，请检查存储权限")

        val target = uniquePackDir()
        if (!staging.renameTo(target)) {
            // 跨分区或权限问题导致 rename 失败时退化为复制。
            staging.copyRecursively(target, overwrite = true)
            FileFs.deleteRecursively(staging)
        }

        File(target, "functions").mkdirs()
        touch(target)

        val manifest = readManifest(target) ?: JSONObject().also {
            it.put("format_version", 1)
            it.put("header", JSONObject())
            it.put("modules", org.json.JSONArray())
        }
        PackManifest.withName(manifest, displayName)
        PackManifest.withUuidIfMissing(manifest)
        writeManifest(target, manifest)

        PackInfo(
            dir = target,
            name = displayName,
            icon = File(target, "pack_icon.png").takeIf { it.isFile },
            modifiedRaw = modifiedText(target),
        )
    }

    /** 删除作品（递归删除子目录），对应 `src/got.myu` 的 `got.kill()`。 */
    fun deletePack(packDir: File): Boolean {
        if (!packDir.exists()) return true
        FileFs.deleteRecursively(packDir)
        return !packDir.exists()
    }

    fun uniquePackDir(): File {
        var candidate = File(root, "functiontestin")
        if (!candidate.exists()) return candidate
        var i = 1
        while (true) {
            val suffix = if (i == 1) "_co" else "_co$i"
            candidate = File(root, "functiontestin$suffix")
            if (!candidate.exists()) return candidate
            i++
        }
    }

}
