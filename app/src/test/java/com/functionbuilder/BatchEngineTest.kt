package com.functionbuilder

import com.functionbuilder.data.BatchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchEngineTest {

    @Test
    fun `单变量按 x 递增替换占位符`() {
        val result = BatchEngine.generate(
            body = "say 第ā个",
            count = 3,
            variables = listOf(BatchEngine.Variable("ā", "x", 0)),
        )
        assertTrue(result.ok)
        assertEquals(listOf("say 第0个", "say 第1个", "say 第2个"), result.lines)
    }

    @Test
    fun `初始值参与计算`() {
        val result = BatchEngine.generate(
            body = "scoreboard players set pā 分数 ā",
            count = 2,
            variables = listOf(BatchEngine.Variable("ā", "x*10+1", 5)),
        )
        assertTrue(result.ok)
        assertEquals(listOf("scoreboard players set p51 分数 51", "scoreboard players set p61 分数 61"), result.lines)
    }

    @Test
    fun `多个变量同时替换`() {
        val result = BatchEngine.generate(
            body = "tp @e[tag=%s] %s 64 0".format("ā", "á"),
            count = 2,
            variables = listOf(
                BatchEngine.Variable("ā", "x", 1),
                BatchEngine.Variable("á", "x*2", 10),
            ),
        )
        assertTrue(result.ok)
        assertEquals(listOf("tp @e[tag=1] 20 64 0", "tp @e[tag=2] 22 64 0"), result.lines)
    }

    @Test
    fun `除法与括号`() {
        val result = BatchEngine.generate(
            "say %s".format("ā"),
            3,
            listOf(BatchEngine.Variable("ā", "(x+4)/2", 0)),
        )
        assertTrue(result.ok)
        assertEquals(listOf("say 2", "say 2.5", "say 3"), result.lines)
    }

    @Test
    fun `非法表达式返回错误而不是崩溃`() {
        val result = BatchEngine.generate("say %s".format("ā"), 1, listOf(BatchEngine.Variable("ā", "x++", 0)))
        assertFalse(result.ok)
        assertTrue(result.error!!.contains("无法计算"))
    }

    @Test
    fun `空命令体或零次数被拒绝`() {
        assertFalse(BatchEngine.generate("", 3, listOf(BatchEngine.Variable("ā", "x", 0))).ok)
        assertFalse(BatchEngine.generate("say a", 0, listOf(BatchEngine.Variable("ā", "x", 0))).ok)
    }

    @Test
    fun `数字格式化去掉多余小数`() {
        assertEquals("3", BatchEngine.formatNumber(3.0))
        assertEquals("2.5", BatchEngine.formatNumber(2.5))
        assertEquals("-1", BatchEngine.formatNumber(-1.0))
    }
}
