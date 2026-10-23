package com.functionbuilder.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * 文件系统抽象。
 *
 * 原 iApp 版本全程使用 `/sdcard/...` 绝对路径。在 Android 11+ 上直接访问
 * `Android/data/<其它应用>` 已经被系统封死，因此装载到 Minecraft 的那部分必须
 * 支持 SAF（[DocFs]），而应用自己的数据目录用 [FileFs] 就够了。
 */
interface FsNode {
    val name: String
    val uriString: String

    fun exists(): Boolean
    fun isDirectory(): Boolean
    fun lastModified(): Long
    fun list(): List<FsNode>
    fun child(name: String): FsNode

    fun mkdirs(): Boolean
    fun readText(): String?
    fun writeText(text: String): Boolean
    fun readBytes(): ByteArray?
    fun writeBytes(bytes: ByteArray): Boolean
    fun delete(): Boolean
}

/** 普通 [File] 实现。 */
class FileFs(val file: File) : FsNode {
    override val name: String get() = file.name
    override val uriString: String get() = file.absolutePath

    override fun exists() = file.exists()
    override fun isDirectory() = file.isDirectory
    override fun lastModified() = file.lastModified()

    override fun list(): List<FsNode> =
        (file.listFiles() ?: emptyArray()).map { FileFs(it) }

    override fun child(name: String) = FileFs(File(file, name))

    override fun mkdirs(): Boolean = file.isDirectory || file.mkdirs()

    override fun readText(): String? = runCatching { file.readText() }.getOrNull()

    override fun writeText(text: String): Boolean = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(text)
        true
    }.getOrDefault(false)

    override fun readBytes(): ByteArray? = runCatching { file.readBytes() }.getOrNull()

    override fun writeBytes(bytes: ByteArray): Boolean = runCatching {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        true
    }.getOrDefault(false)

    override fun delete(): Boolean = runCatching {
        if (file.isDirectory) {
            deleteRecursively(file)
            !file.exists()
        } else {
            file.delete()
        }
    }.getOrDefault(false)

    companion object {
        fun deleteRecursively(f: File) {
            if (!f.exists()) return
            if (f.isDirectory) f.listFiles()?.forEach { deleteRecursively(it) }
            f.delete()
        }
    }
}

/** SAF（DocumentFile）实现，用于 Android 11+ 访问 Minecraft 目录。 */
class DocFs(val doc: DocumentFile) : FsNode {
    override val name: String get() = doc.name ?: "(未命名)"
    override val uriString: String get() = doc.uri.toString()

    override fun exists() = doc.exists()
    override fun isDirectory() = doc.isDirectory
    override fun lastModified() = doc.lastModified()

    override fun list(): List<FsNode> = doc.listFiles().map { DocFs(it) }

    /**
     * 已存在的子项直接返回；不存在的返回 [PendingDocChild]，
     * 真正写入时才决定要创建文件还是目录。
     */
    override fun child(name: String): FsNode {
        doc.findFile(name)?.let { return DocFs(it) }
        return PendingDocChild(doc, name)
    }

    override fun mkdirs(): Boolean = doc.isDirectory

    override fun readText(): String? = runCatching {
        appContext?.contentResolver?.openInputStream(doc.uri)?.use { it.readBytes().decodeToString() }
    }.getOrNull()

    override fun writeText(text: String): Boolean = writeBytes(text.encodeToByteArray())

    override fun readBytes(): ByteArray? = runCatching {
        appContext?.contentResolver?.openInputStream(doc.uri)?.use { it.readBytes() }
    }.getOrNull()

    override fun writeBytes(bytes: ByteArray): Boolean {
        if (doc.isDirectory) return false
        val resolver = appContext?.contentResolver ?: return false
        return runCatching {
            resolver.openOutputStream(doc.uri, "wt")?.use { it.write(bytes) } != null
        }.getOrDefault(false)
    }

    override fun delete(): Boolean = doc.delete()

    companion object {
        /** Application 启动时注入，供 ContentResolver 使用。 */
        @Volatile
        var appContext: Context? = null
    }
}

/**
 * SAF 下“还不存在的子项”。
 *
 * 关键点：`DocumentFile.createDirectory()` 建出来的是目录，绝不能拿它当文件用。
 * 所以这里延迟到写入那一刻再决定创建目录还是文件——否则往存档里写
 * `manifest.json` 会得到一个名叫 manifest.json 的目录，装载必然失败。
 */
private class PendingDocChild(
    private val parent: DocumentFile,
    private val childName: String,
) : FsNode {

    private var created: DocumentFile? = null

    override val name: String get() = childName
    override val uriString: String get() = parent.uri.toString() + "/" + childName

    override fun exists() = created?.exists() ?: false
    override fun isDirectory() = created?.isDirectory ?: false
    override fun lastModified() = created?.lastModified() ?: 0L
    override fun list(): List<FsNode> = emptyList()

    private fun materialize(asDirectory: Boolean): DocumentFile? {
        created?.let { return it }
        val file = if (asDirectory) {
            parent.createDirectory(childName)
        } else {
            parent.createFile("application/octet-stream", childName)
        }
        created = file
        return file
    }

    override fun mkdirs(): Boolean = materialize(true)?.isDirectory == true

    override fun child(name: String): FsNode {
        val dir = materialize(true) ?: return NotFoundNode(name, parent.uri)
        return DocFs(dir).child(name)
    }

    override fun readText(): String? = null
    override fun writeText(text: String): Boolean = writeBytes(text.encodeToByteArray())

    override fun readBytes(): ByteArray? = null

    override fun writeBytes(bytes: ByteArray): Boolean {
        val file = materialize(false) ?: return false
        val resolver = DocFs.appContext?.contentResolver ?: return false
        return runCatching {
            resolver.openOutputStream(file.uri, "wt")?.use { it.write(bytes) } != null
        }.getOrDefault(false)
    }

    override fun delete(): Boolean {
        val file = created ?: return false
        return file.delete()
    }
}

/** 创建失败时的占位节点，所有写操作都返回 false 而不是抛异常。 */
class NotFoundNode(override val name: String, private val parentUri: Uri) : FsNode {
    override val uriString: String get() = "$parentUri/$name"
    override fun exists() = false
    override fun isDirectory() = false
    override fun lastModified() = 0L
    override fun list(): List<FsNode> = emptyList()
    override fun child(name: String): FsNode = NotFoundNode(name, parentUri)
    override fun mkdirs() = false
    override fun readText(): String? = null
    override fun writeText(text: String): Boolean = false
    override fun readBytes(): ByteArray? = null
    override fun writeBytes(bytes: ByteArray): Boolean = false
    override fun delete() = false
}

/** 递归复制目录树，源和目标都可以是 [FileFs] 或 [DocFs]。 */
fun copyTree(src: FsNode, dst: FsNode, overwrite: Boolean = true): Boolean {
    if (!src.isDirectory()) {
        if (!overwrite && dst.exists()) return true
        val bytes = src.readBytes() ?: return false
        return dst.writeBytes(bytes)
    }
    if (!dst.mkdirs()) return false
    var ok = true
    for (childSrc in src.list()) {
        val childDst = dst.child(childSrc.name)
        ok = copyTree(childSrc, childDst, overwrite) && ok
    }
    return ok
}

/** 按文件名查找子节点（不区分大小写兜底）。 */
fun FsNode.findChild(name: String): FsNode? =
    list().firstOrNull { it.name == name } ?: list().firstOrNull { it.name.equals(name, true) }
