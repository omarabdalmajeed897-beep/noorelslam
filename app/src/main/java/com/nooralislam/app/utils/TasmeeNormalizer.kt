package com.nooralislam.app.utils

import com.nooralislam.app.data.QuranRepository
import kotlin.math.max
import kotlin.math.roundToInt

data class TasmeeMatchResult(
    val accuracyPercent: Int,
    val matchedWords: Int,
    val totalWords: Int,
    val missedWords: List<String>
)

object TasmeeNormalizer {
    fun compare(targetText: String, spokenText: String): TasmeeMatchResult {
        val rawTarget = targetText.split(Regex("\s+")).filter {
            QuranRepository.normalizeArabic(it).isNotEmpty()
        }
        val normTarget = rawTarget.map { QuranRepository.normalizeArabic(it) }
        val normSpoken = QuranRepository.normalizeArabic(spokenText)
            .split(Regex("\s+"))
            .filter { it.isNotEmpty() }

        val n = normTarget.size
        val m = normSpoken.size
        if (n == 0) return TasmeeMatchResult(0, 0, 0, emptyList())

        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) {
            for (j in 1..m) {
                dp[i][j] = if (normTarget[i - 1] == normSpoken[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    max(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val matched = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            if (normTarget[i - 1] == normSpoken[j - 1]) {
                matched[i - 1] = true
                i--; j--
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }

        val count = matched.count { it }
        val missed = rawTarget.filterIndexed { idx, _ -> !matched[idx] }
        val pct = ((count.toDouble() / n) * 100.0).roundToInt()
        return TasmeeMatchResult(pct, count, n, missed)
    }
}
