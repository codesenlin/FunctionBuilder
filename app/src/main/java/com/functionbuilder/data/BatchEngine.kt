package com.functionbuilder.data

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * 批量生成指令。
 *
 * 旧版 `many.iyu` 的这套算法很难看懂（把变量占位符列表反复对着自身做替换，
 * 实际行为是“所有变量的代数式被当成同一张映射表套来套去”），跑出来的结果
 * 也不稳定。这里按它原本想做的事重写成明确语义：
 *
 *   命令体里写占位符（ā á ǎ à … 这些用拼音输入法打出来的元音），
 *   每个占位符配一条含 `x` 的代数式和初始值；
 *   第 i 条输出 = 用 x = 初始值 + i 求出每个代数式的值，替换掉对应占位符。
 *
 * 次数 = 生成的条数。
 */
object BatchEngine {

    /** 占位符候选，顺序沿用旧版 `vtari` 字符串。 */
    val PLACEHOLDERS: List<String> = listOf(
        "ā", "á", "ǎ", "à",
        "ō", "ó", "ǒ", "ò",
        "ē", "é", "ě", "è",
        "ī", "í", "ǐ", "ì",
        "ū", "ú", "ǔ", "ù",
        "ǖ", "ǘ", "ǚ", "ǜ", "ü",
    )

    data class Variable(
        val placeholder: String,
        val expression: String,
        val initial: Int = 0,
    )

    data class BatchResult(
        val lines: List<String>,
        val error: String? = null,
    ) {
        val ok: Boolean get() = error == null
    }

    fun generate(body: String, count: Int, variables: List<Variable>): BatchResult {
        if (body.isBlank()) return BatchResult(emptyList(), "请输入命令体")
        if (count <= 0) return BatchResult(emptyList(), "次数要大于 0")
        if (variables.isEmpty()) return BatchResult(emptyList(), "请至少添加一个变量")

        val out = ArrayList<String>(count)
        for (i in 0 until count) {
            var line = body
            for (v in variables) {
                val value = try {
                    Evaluator(v.expression, (v.initial + i).toDouble()).evaluate()
                } catch (e: Exception) {
                    return BatchResult(emptyList(), "代数式「${v.expression}」无法计算：${e.message}")
                }
                line = line.replace(v.placeholder, formatNumber(value))
            }
            out.add(line)
        }
        return BatchResult(out)
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()
        return if (abs(value - floor(value)) < 1e-9 && abs(value) < 1e15) {
            value.toLong().toString()
        } else {
            val rounded = kotlin.math.round(value * 10000.0) / 10000.0
            rounded.toString().removeSuffix(".0")
        }
    }

    /** 极小的表达式求值器：支持 + - * / % ^、括号、一元负号和变量 x。 */
    private class Evaluator(private val src: String, private val x: Double) {
        private var pos = 0

        fun evaluate(): Double {
            val value = parseExpression()
            skipSpaces()
            if (pos < src.length) error("多余的字符 '${src[pos]}'")
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                when {
                    take('+') -> value += parseTerm()
                    take('-') -> value -= parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                skipSpaces()
                when {
                    take('*') -> value *= parsePower()
                    take('/') -> value /= parsePower()
                    take('%') -> value %= parsePower()
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            skipSpaces()
            if (take('^')) return base.pow(parsePower())
            return base
        }

        private fun parseUnary(): Double {
            skipSpaces()
            if (take('-')) return -parseUnary()
            if (take('+')) return parseUnary()
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            if (take('(')) {
                val value = parseExpression()
                skipSpaces()
                if (!take(')')) error("缺少右括号")
                return value
            }
            if (pos < src.length && (src[pos] == 'x' || src[pos] == 'X')) {
                pos++
                return x
            }
            val start = pos
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
            if (start == pos) {
                error(if (pos < src.length) "无法识别 '${src[pos]}'" else "表达式不完整")
            }
            return src.substring(start, pos).toDoubleOrNull() ?: error("数字格式不对")
        }

        private fun skipSpaces() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun take(c: Char): Boolean {
            if (pos < src.length && src[pos] == c) {
                pos++
                return true
            }
            return false
        }
    }
}
