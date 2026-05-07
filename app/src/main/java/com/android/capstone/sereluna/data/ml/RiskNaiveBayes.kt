package com.android.capstone.sereluna.data.ml

import kotlin.math.ln

/**
 * Naive Bayes ringan dengan vocab statis (tanpa training runtime).
 * Kelas: low / medium / high.
 * Ini contoh sederhana untuk menunjukkan komponen ML konvensional.
 */
class RiskNaiveBayes {
    // Vocab dan log-prob (contoh kecil; bisa diperluas)
    private val vocab = listOf(
        "takut", "cemas", "panik", "depresi", "sedih", "putus asa",
        "tidur", "mimpi buruk", "lelah", "stress", "marah", "frustrasi",
        "bunuh", "mati", "melukai", "self harm"
    )
    private val classes = listOf("low", "medium", "high")

    // log prior (simetris)
    private val logPrior = mapOf(
        "low" to ln(1.0 / 3),
        "medium" to ln(1.0 / 3),
        "high" to ln(1.0 / 3)
    )

    // log likelihood per class (contoh manual)
    private val logLikelihood: Map<String, DoubleArray> = mapOf(
        "low" to doubleArrayOf(
            lnProb(0.2), lnProb(0.2), lnProb(0.1), lnProb(0.05), lnProb(0.2), lnProb(0.05),
            lnProb(0.2), lnProb(0.05), lnProb(0.2), lnProb(0.2), lnProb(0.1), lnProb(0.1),
            lnProb(0.01), lnProb(0.01), lnProb(0.01), lnProb(0.01)
        ),
        "medium" to doubleArrayOf(
            lnProb(0.3), lnProb(0.4), lnProb(0.3), lnProb(0.2), lnProb(0.3), lnProb(0.2),
            lnProb(0.2), lnProb(0.2), lnProb(0.2), lnProb(0.4), lnProb(0.3), lnProb(0.3),
            lnProb(0.05), lnProb(0.05), lnProb(0.05), lnProb(0.05)
        ),
        "high" to doubleArrayOf(
            lnProb(0.4), lnProb(0.5), lnProb(0.5), lnProb(0.5), lnProb(0.4), lnProb(0.5),
            lnProb(0.1), lnProb(0.1), lnProb(0.2), lnProb(0.5), lnProb(0.4), lnProb(0.4),
            lnProb(0.4), lnProb(0.4), lnProb(0.4), lnProb(0.4)
        )
    )

    private fun lnProb(p: Double): Double = ln(p)

    fun classify(text: String): String {
        val tokens = tokenize(text)
        val counts = IntArray(vocab.size)
        tokens.forEach { t ->
            val idx = vocab.indexOf(t)
            if (idx >= 0) counts[idx]++
        }
        var bestClass = "low"
        var bestScore = Double.NEGATIVE_INFINITY
        for (cls in classes) {
            val ll = logLikelihood[cls] ?: continue
            var score = logPrior[cls] ?: Double.NEGATIVE_INFINITY
            for (i in counts.indices) {
                if (counts[i] > 0) {
                    score += counts[i] * ll[i]
    }
}
            if (score > bestScore) {
                bestScore = score
                bestClass = cls
            }
        }
        return bestClass
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
}
