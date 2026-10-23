package com.functionbuilder.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.functionbuilder.util.DocFs
import com.functionbuilder.util.FileFs
import com.functionbuilder.util.FsNode
import com.functionbuilder.util.Storage
import com.functionbuilder.util.findChild
import java.io.File

/** 一个存档（世界）。 */
data class WorldEntry(
    val id: String,
    val displayName: String,
    val node: FsNode,
    val iconFile: File?,
)

/** 一个可以装载目标的根（国际版 / 中国版 / 用户用 SAF 选的目录）。 */
data class InstallRoot(
    val label: String,
    val worldsDir: FsNode,
    val mojangDir: FsNode? = null,
    val source: Source = Source.DIRECT,
) {
    enum class Source { DIRECT, SAF }

    fun worlds(): List<WorldEntry> {
        val children = worldsDir.list().filter { it.isDirectory() }
        return children.map { world ->
            val levelName = world.child("levelname.txt").readText()?.trim().orEmpty()
            WorldEntry(
                id = world.name,
                displayName = levelName.ifBlank { world.name },
                node = world,
                iconFile = (world as? FileFs)?.let { File(it.file, "world_icon.jpeg") }?.takeIf { it.isFile },
            )
        }.sortedBy { it.displayName.lowercase() }
    }
}

/**
 * 找到 Minecraft 的存档目录。
 *
 * 对应旧版 `endin.iyu` → `endna.iyu` / `endch.iyu` 的流程：
 *  - 国际版：`/sdcard/games/com.mojang/minecraftWorlds`
 *  - 中国版：`/sdcard/Android/data/<包名>/files/minecraftWorlds`
 * 另外提供 SAF 兜底（Android 11+ 直接路径读写不了时用）。
 */
object MinecraftAccess {

    fun directRoots(context: Context): List<InstallRoot> {
        val roots = ArrayList<InstallRoot>()

        Storage.internationalCandidates().forEach { mojang ->
            val worlds = Storage.worldsIn(mojang)
            if (worlds.isDirectory && worlds.canRead()) {
                roots += InstallRoot(
                    label = "国际版（${mojang.parentFile?.name ?: mojang.name}）",
                    worldsDir = FileFs(worlds),
                    mojangDir = FileFs(mojang),
                )
            }
        }

        Storage.chineseCandidates().forEach { worlds ->
            val pkg = worlds.parentFile?.parentFile?.name ?: "中国版"
            roots += InstallRoot(
                label = "中国版（$pkg）",
                worldsDir = FileFs(worlds),
                mojangDir = null,
            )
        }
        return roots
    }

    /** 中国版存档点（设置页里“点击查看中国版存档点”用的）。 */
    fun chineseWorldDirs(): List<File> = Storage.chineseCandidates()

    /**
     * 把用户 SAF 选中的目录解释成 [InstallRoot]。
     *
     * 支持三种选法：
     *  - 选了 `com.mojang` 这一层 → 里面有 `minecraftWorlds`
     *  - 选了 `minecraftWorlds` 这一层 → 子目录就是存档
     *  - 直接选了某个存档目录 → 直接把包装进这个存档
     */
    fun fromTreeUri(context: Context, uri: Uri): InstallRoot? {
        val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
        val node = DocFs(tree)

        node.findChild("minecraftWorlds")?.let { worlds ->
            return InstallRoot(
                label = "SAF：${tree.name ?: "已选目录"}",
                worldsDir = worlds,
                mojangDir = node,
                source = InstallRoot.Source.SAF,
            )
        }

        if (node.name == "minecraftWorlds") {
            return InstallRoot("SAF：minecraftWorlds", worldsDir = node, source = InstallRoot.Source.SAF)
        }

        // 当作“单个存档”处理：伪造一个只含自己的 worlds 目录
        val single = SingleWorldDir(node)
        return InstallRoot(
            label = "SAF：${tree.name ?: "已选存档"}",
            worldsDir = single,
            source = InstallRoot.Source.SAF,
        )
    }

    /** 把单个存档目录包装成“只包含一个存档”的目录。 */
    private class SingleWorldDir(private val world: FsNode) : FsNode {
        override val name: String get() = world.name
        override val uriString: String get() = world.uriString
        override fun exists() = true
        override fun isDirectory() = true
        override fun lastModified() = world.lastModified()
        override fun list(): List<FsNode> = listOf(world)
        override fun child(name: String): FsNode = world.child(name)
        override fun mkdirs() = true
        override fun readText(): String? = null
        override fun writeText(text: String) = false
        override fun readBytes(): ByteArray? = null
        override fun writeBytes(bytes: ByteArray) = false
        override fun delete() = false
    }
}
