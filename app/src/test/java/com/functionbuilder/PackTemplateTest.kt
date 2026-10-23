package com.functionbuilder

import com.functionbuilder.data.FnRepository
import com.functionbuilder.data.PackManifest
import com.functionbuilder.data.PackTemplate
import com.functionbuilder.util.FileFs
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 作品模板解压 / manifest 读写 / function 文件操作的 JVM 单元测试。
 * 用真实的 assets/functiontestin.zip，不依赖真机。
 */
class PackTemplateTest {

    private lateinit var tmp: File

    @Before
    fun setUp() {
        tmp = Files.createTempDirectory("fbtest").toFile()
    }

    @After
    fun tearDown() {
        tmp.deleteRecursively()
    }

    private fun templateStream() = File("src/main/assets/functiontestin.zip").inputStream()

    @Test
    fun `模板能被正确解压出行为包结构`() {
        assertTrue("解压失败", PackTemplate.extract(templateStream(), tmp))

        val root = File(tmp, PackTemplate.ROOT_DIR)
        assertTrue(root.isDirectory)
        assertTrue(File(root, "manifest.json").isFile)
        assertTrue(File(root, "pack_icon.png").isFile)
        assertTrue(File(root, "functions").isDirectory)
        assertTrue(File(root, "functions/test.mcfunction").isFile)

        // 解压出来的 manifest 必须是合法 JSON，且带 uuid
        val manifest = PackManifest.parse(File(root, "manifest.json").readText())
        assertNotNull(manifest)
        assertTrue(PackManifest.headerUuid(manifest).isNotBlank())
        assertEquals("data", manifest!!.getJSONArray("modules").getJSONObject(0).getString("type"))
    }

    @Test
    fun `解压不会把文件写到目标目录之外`() {
        // 构造一个带 ../ 的恶意 zip
        val evil = File(tmp, "evil.zip")
        java.util.zip.ZipOutputStream(evil.outputStream()).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("functiontestin/ok.txt"))
            zip.write("ok".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("../escaped.txt"))
            zip.write("bad".toByteArray())
            zip.closeEntry()
        }
        val dest = File(tmp, "dest").apply { mkdirs() }
        PackTemplate.extract(evil.inputStream(), dest)

        assertTrue(File(dest, "functiontestin/ok.txt").isFile)
        assertFalse("zip slip 没被拦住", File(tmp, "escaped.txt").exists())
    }

    @Test
    fun `manifest 的读写字段正确`() {
        val manifest = JSONObject()
            .put("format_version", 1)
            .put("header", JSONObject().put("name", "").put("description", ""))
            .put("modules", org.json.JSONArray().put(JSONObject().put("description", "")))

        PackManifest.withName(manifest, "我的作品")
        PackManifest.withDescription(manifest, "简介")
        PackManifest.withModuleDescription(manifest, "作者")
        PackManifest.withNewUuids(manifest)

        val reparsed = PackManifest.parse(PackManifest.pretty(manifest))
        assertNotNull(reparsed)
        assertEquals("我的作品", PackManifest.headerName(reparsed))
        assertEquals("简介", PackManifest.headerDescription(reparsed))
        assertEquals("作者", PackManifest.moduleDescription(reparsed))
        assertTrue(PackManifest.headerUuid(reparsed).isNotBlank())
    }

    @Test
    fun `缺失 uuid 时会被补上`() {
        val manifest = JSONObject().put("header", JSONObject()).put("modules", org.json.JSONArray())
        PackManifest.withUuidIfMissing(manifest)
        assertTrue(PackManifest.headerUuid(manifest).isNotBlank())
        assertNotNull(manifest.getJSONArray("modules").optJSONObject(0))
    }

    @Test
    fun `function 文件名校验与重命名不会误删`() {
        val repo = FnRepository()
        val pack = File(tmp, "pack").apply { mkdirs() }

        assertTrue(FnRepository.isValidName("hello_world2"))
        assertFalse(FnRepository.isValidName("带中文"))
        assertFalse(FnRepository.isValidName("has space"))
        assertFalse(FnRepository.isValidName(""))

        val a = repo.create(pack, "a").getOrThrow()
        val b = repo.create(pack, "b").getOrThrow()
        repo.write(a, "say A")

        // 重名必须失败，且原文件内容不能丢
        assertTrue(repo.rename(pack, a, "b").isFailure)
        assertTrue(a.isFile)
        assertEquals("say A", repo.read(a))

        val renamed = repo.rename(pack, a, "c")
        assertTrue(renamed.isSuccess)
        assertFalse(a.exists())
        assertTrue(File(pack, "functions/c.mcfunction").isFile)
        assertEquals("say A", repo.read(File(pack, "functions/c.mcfunction")))
        assertTrue(b.isFile)
    }

    @Test
    fun `递归删除能清空嵌套目录`() {
        val root = File(tmp, "deep/pack/functions/sub").apply { mkdirs() }
        File(root, "x.mcfunction").writeText("say x")
        File(tmp, "deep/pack/manifest.json").writeText("{}")

        FileFs.deleteRecursively(File(tmp, "deep"))
        assertFalse(File(tmp, "deep").exists())
    }
}
