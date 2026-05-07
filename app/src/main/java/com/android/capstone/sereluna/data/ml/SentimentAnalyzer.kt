package com.android.capstone.sereluna.data.ml


class SentimentAnalyzer {
    private val positive = listOf("senang", "lega", "baik", "bahagia", "semangat", "optimis", "tenang")
    private val negative = listOf("sedih", "takut", "cemas", "khawatir", "lelah", "marah", "kesal", "frustrasi", "hampa")
    fun analyze(text: String): Pair<String, Float> {
        val tokens = tokenize(text)
        var pos = 0
        var neg = 0
        tokens.forEach { t ->
            if (t in positive) pos++
            if (t in negative) neg++
        }
        val label: String
        val score: Float
        if (pos == 0 && neg == 0) {
            label = "neutral"
            score = 0.5f
        } else if (neg >= pos) {
            label = "negative"
            score = (neg.toFloat() / (pos + neg)).coerceIn(0f, 1f)
        } else {
            label = "positive"
            score = (pos.toFloat() / (pos + neg)).coerceIn(0f, 1f)
        }
        return label to score
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
}
