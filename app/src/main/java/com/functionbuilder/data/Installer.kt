package com.functionbuilder.data

import com.functionbuilder.util.FileFs
import com.functionbuilder.util.FsNode
import com.functionbuilder.util.copyTree
import java.io.File

/**
 * 把作品装载到 Minecraft。
 *
 * 对应旧版 `endna.iyu`（国际版）、`endch.iyu`（中国版）以及 `json.myu` 的
 * `json.header()`（合并 world_behavior_packs.json）。
 */
object Installer {

    /** 行为包在存档里的固定文件夹名，和旧版保持一致。 */
    private const val PACK_DIR_NAME = "functiontestin"

    /** 需要复制进存档的文件/目录；`test/` 是本地时间戳，不往外拷。 */
    private val PACK_ENTRIES = listOf("manifest.json", "pack_icon.png", "functions")

    data class Result(val ok: Boolean, val message: String) {
        companion object {
            fun ok(message: String) = Result(true, message)
            fun fail(message: String) = Result(false, message)
        }
    }

    /** 装载进某个存档的 behavior_packs。 */
    fun installIntoWorld(packDir: File, world: FsNode): Result {
        val behaviorPacks = world.child("behavior_packs")
        if (!behaviorPacks.mkdirs()) {
            return Result.fail("无法创建 behavior_packs 目录（可能是没有写入权限）")
        }

        val uuid = ensurePackUuid(packDir)
            ?: return Result.fail("manifest.json 读取失败，无法装载")

        val dest = behaviorPacks.child(PACK_DIR_NAME)
        if (dest.exists()) dest.delete()
        if (!copyPack(packDir, dest)) {
            return Result.fail("复制行为包失败")
        }

        val merged = mergeWorldIndex(world, uuid)
        return if (merged) {
            Result.ok("装载成功！\n在游戏里输入 /function 就能看到你的指令了")
        } else {
            Result.fail("行为包已复制，但 world_behavior_packs.json 写入失败；\n请到存档设置里手动启用该行为包")
        }
    }

    /** 直接放进游戏的全局行为包列表（仅国际版有这个入口）。 */
    fun installIntoGlobalBehaviorPacks(packDir: File, mojang: FsNode): Result {
        val behaviorPacks = mojang.child("behavior_packs")
        if (!behaviorPacks.mkdirs()) {
            return Result.fail("无法创建 behavior_packs 目录（可能是没有写入权限）")
        }
        ensurePackUuid(packDir) ?: return Result.fail("manifest.json 读取失败，无法装载")

        val dest = behaviorPacks.child(PACK_DIR_NAME)
        if (dest.exists()) dest.delete()
        if (!copyPack(packDir, dest)) return Result.fail("复制行为包失败")

        return Result.ok("装载成功！\n请记得在“世界设置－行为包”里启用它")
    }

    /** 只复制行为包本体，不合并任何索引。 */
    fun exportTo(packDir: File, destParent: FsNode): Result {
        ensurePackUuid(packDir) ?: return Result.fail("manifest.json 读取失败")
        val dest = destParent.child(PACK_DIR_NAME)
        if (dest.exists()) dest.delete()
        return if (copyPack(packDir, dest)) Result.ok("已导出到 ${dest.name}")
        else Result.fail("复制行为包失败")
    }

    private fun copyPack(packDir: File, dest: FsNode): Boolean {
        if (!dest.mkdirs()) return false
        var ok = true
        for (entry in PACK_ENTRIES) {
            val srcFile = File(packDir, entry)
            if (!srcFile.exists()) continue
            ok = copyTree(FileFs(srcFile), dest.child(entry), overwrite = true) && ok
        }
        return ok
    }

    /**
     * 读取（并在缺失时补齐）manifest.json 的 header.uuid。
     * 缺 uuid 的旧作品装载后游戏会忽略它，所以这里顺手补上。
     */
    fun ensurePackUuid(packDir: File): String? {
        val manifestFile = File(packDir, "manifest.json")
        if (!manifestFile.isFile) return null
        val obj = PackManifest.parse(runCatching { manifestFile.readText() }.getOrDefault("")) ?: return null
        PackManifest.withUuidIfMissing(obj)
        runCatching {
            manifestFile.writeText(PackManifest.pretty(obj))
        }
        return PackManifest.headerUuid(obj).ifBlank { null }
    }

    private fun mergeWorldIndex(world: FsNode, packUuid: String): Boolean {
        val index = world.child(WorldPackIndex.fileName())
        val existing = index.readText()
        val merged = WorldPackIndex.merge(existing, packUuid)
        return index.writeText(merged)
    }
}
