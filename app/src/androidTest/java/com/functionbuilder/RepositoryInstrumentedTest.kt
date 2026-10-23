package com.functionbuilder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.functionbuilder.data.FnRepository
import com.functionbuilder.data.Installer
import com.functionbuilder.data.PackManifest
import com.functionbuilder.data.PackRepository
import com.functionbuilder.data.WorldPackIndex
import com.functionbuilder.util.FileFs
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 在真机上验证最关键的几条链路：新建作品（解压模板）、写 manifest、
 * 建 .mcfunction、装载进存档、合并 world_behavior_packs.json。
 *
 * 这些测试不碰界面，所以不会干扰正在使用手机的人。
 */
@RunWith(AndroidJUnit4::class)
class RepositoryInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val packRepo = PackRepository(context)
    private val fnRepo = FnRepository()

    @Test
    fun createPack_extractsTemplateAndWritesManifest() {
        val result = packRepo.createPack("测试作品")
        assertTrue("createPack 失败：${result.exceptionOrNull()}", result.isSuccess)
        val pack = result.getOrThrow()

        try {
            assertTrue("作品目录不存在", pack.dir.isDirectory)
            assertTrue("manifest.json 缺失", File(pack.dir, "manifest.json").isFile)
            assertTrue("pack_icon.png 缺失", File(pack.dir, "pack_icon.png").isFile)
            assertTrue("functions 目录缺失", File(pack.dir, "functions").isDirectory)
            assertTrue("test/time 缺失", File(pack.dir, "test/time").isFile)

            val manifest = PackManifest.parse(File(pack.dir, "manifest.json").readText())
            assertNotNull("manifest.json 解析失败", manifest)
            assertEquals("测试作品", PackManifest.headerName(manifest))
            assertTrue("header.uuid 为空", PackManifest.headerUuid(manifest).isNotBlank())

            // 模板里自带的示例 function 应该被解压出来
            assertTrue("模板 functions 没有解压", fnRepo.listFunctions(pack.dir).isNotEmpty())
        } finally {
            packRepo.deletePack(pack.dir)
        }
    }

    @Test
    fun createPack_secondTimeGetsUniqueDirectory() {
        val first = packRepo.createPack("A").getOrThrow()
        val second = packRepo.createPack("B").getOrThrow()
        try {
            assertTrue(first.dir.absolutePath != second.dir.absolutePath)
        } finally {
            packRepo.deletePack(first.dir)
            packRepo.deletePack(second.dir)
        }
    }

    @Test
    fun functionFile_create_rename_delete_roundTrip() {
        val pack = packRepo.createPack("函数测试").getOrThrow()
        try {
            val created = fnRepo.create(pack.dir, "hello")
            assertTrue("新建 function 失败：${created.exceptionOrNull()}", created.isSuccess)
            val file = created.getOrThrow()
            assertTrue(file.isFile)

            assertTrue("写入失败", fnRepo.write(file, "say hi\nsay bye"))
            assertEquals("say hi\nsay bye", fnRepo.read(file))

            val duplicated = fnRepo.create(pack.dir, "hello")
            assertTrue("重名应该失败", duplicated.isFailure)

            val renamed = fnRepo.rename(pack.dir, file, "world")
            assertTrue("重命名失败：${renamed.exceptionOrNull()}", renamed.isSuccess)
            val renamedFile = renamed.getOrThrow()
            assertEquals("world.mcfunction", renamedFile.name)
            assertFalse("旧文件应已不存在", file.exists())
            assertEquals("say hi\nsay bye", fnRepo.read(renamedFile))

            // 非法名字要被挡住
            assertTrue(fnRepo.create(pack.dir, "有中文名").isFailure)
            assertTrue(fnRepo.create(pack.dir, "has space").isFailure)

            assertTrue(fnRepo.delete(renamedFile))
            assertFalse(renamedFile.exists())
        } finally {
            packRepo.deletePack(pack.dir)
        }
    }

    @Test
    fun installer_copiesPackIntoWorldAndMergesIndex() {
        val pack = packRepo.createPack("装载测试").getOrThrow()
        val world = kotlin.io.path.createTempDirectory("world").toFile()
        try {
            fnRepo.write(File(pack.dir, "functions/test.mcfunction"), "say hello")

            val result = Installer.installIntoWorld(pack.dir, FileFs(world))
            assertTrue("装载失败：${result.message}", result.ok)

            val installed = File(world, "behavior_packs/functiontestin")
            assertTrue("行为包目录不存在", installed.isDirectory)
            assertTrue(File(installed, "manifest.json").isFile)
            assertTrue(File(installed, "pack_icon.png").isFile)
            assertTrue(File(installed, "functions/test.mcfunction").isFile)
            assertFalse("test/ 不应该被复制进存档", File(installed, "test").exists())

            val index = File(world, WorldPackIndex.fileName())
            assertTrue("world_behavior_packs.json 未生成", index.isFile)
            val array = JSONArray(index.readText())
            assertEquals(1, array.length())
            val uuid = PackManifest.headerUuid(
                PackManifest.parse(File(pack.dir, "manifest.json").readText()),
            )
            assertEquals(uuid, array.getJSONObject(0).getString("pack_id"))

            // 再装一次不能产生重复条目
            Installer.installIntoWorld(pack.dir, FileFs(world))
            assertEquals(1, JSONArray(index.readText()).length())
        } finally {
            packRepo.deletePack(pack.dir)
            world.deleteRecursively()
        }
    }
}
