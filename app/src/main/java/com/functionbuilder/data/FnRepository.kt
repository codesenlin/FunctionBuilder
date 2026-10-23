package com.functionbuilder.data

import java.io.File

/**
 * `.mcfunction` 文件的增删改查。
 * 对应旧版 `functions.looking()` / `add.iyu` / `name.iyu` 的行为。
 */
class FnRepository {

    companion object {
        const val EXT = ".mcfunction"

        /** /function 名字允许的字符（旧版输入框用 digits 限制成纯字母）。 */
        private val NAME_REGEX = Regex("^[A-Za-z0-9_]+$")

        fun isValidName(raw: String): Boolean = NAME_REGEX.matches(raw.trim())
    }

    fun functionsDir(packDir: File): File = File(packDir, "functions").also { it.mkdirs() }

    fun listFunctions(packDir: File): List<File> =
        functionsDir(packDir).listFiles()
            ?.filter { it.isFile && it.name.endsWith(EXT, ignoreCase = true) }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    /** 去掉 `.mcfunction` 后缀的显示名。 */
    fun displayName(file: File): String = file.name.removeSuffix(EXT).removeSuffix(EXP_ALT).ifBlank { file.name }

    /** 新建空文件；重名时返回失败。 */
    fun create(packDir: File, rawName: String): Result<File> {
        val name = rawName.trim()
        if (name.isEmpty()) return Result.failure(IllegalArgumentException("亲，你什么都没写"))
        if (!isValidName(name)) return Result.failure(IllegalArgumentException("只能使用字母、数字和下划线"))
        val file = File(functionsDir(packDir), name + EXT)
        if (file.exists()) return Result.failure(IllegalStateException("这个名字重名了"))
        return runCatching {
            file.createNewFile()
            file
        }
    }

    /** 重命名；已存在同名文件时失败（旧版的“重命名变成删除”bug 已修掉）。 */
    fun rename(packDir: File, file: File, rawName: String): Result<File> {
        val name = rawName.trim()
        if (name.isEmpty()) return Result.failure(IllegalArgumentException("亲，你什么都没写"))
        if (!isValidName(name)) return Result.failure(IllegalArgumentException("只能使用字母、数字和下划线"))
        val target = File(functionsDir(packDir), name + EXT)
        if (target.absolutePath == file.absolutePath) {
            return Result.failure(IllegalStateException("这个名字和其他 function 指令重名了\n或者没有更改"))
        }
        if (target.exists()) {
            return Result.failure(IllegalStateException("这个名字和其他 function 指令重名了\n或者没有更改"))
        }
        return runCatching {
            if (!file.renameTo(target)) {
                target.writeText(file.readText())
                file.delete()
            }
            target
        }
    }

    fun delete(file: File): Boolean {
        if (!file.exists()) return true
        return file.delete()
    }

    fun read(file: File): String =
        if (file.isFile) runCatching { file.readText() }.getOrDefault("") else ""

    fun write(file: File, text: String): Boolean = runCatching {
        file.parentFile?.mkdirs()
        file.writeText(text)
        true
    }.getOrDefault(false)

    private val EXP_ALT = ".mcfunction"
}
