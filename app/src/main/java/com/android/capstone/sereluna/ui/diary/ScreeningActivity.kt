package com.android.capstone.sereluna.ui.diary

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import com.android.capstone.sereluna.data.repository.DassScore
import com.android.capstone.sereluna.data.repository.ScreeningRepository
import com.android.capstone.sereluna.databinding.ActivityScreeningBinding
import kotlinx.coroutines.launch

class ScreeningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreeningBinding
    private val screeningRepository = ScreeningRepository()
    private lateinit var adapter: DassAdapter
    private val answers = IntArray(21) { -1 }
    private val questions = listOf(
        "Saya merasa sulit untuk tenang.",
        "Mulut saya terasa kering.",
        "Saya sepertinya tidak bisa merasakan perasaan positif.",
        "Saya mengalami kesulitan bernafas (misalnya napas cepat, tersengal-sengal).",
        "Saya merasa sulit untuk memulai aktivitas.",
        "Saya cenderung bereaksi berlebihan terhadap situasi.",
        "Saya mengalami tremor (misalnya di tangan).",
        "Saya merasa menggunakan banyak energi karena gugup.",
        "Saya khawatir tentang situasi di mana saya mungkin panik dan mempermalukan diri sendiri.",
        "Saya merasa tidak ada yang bisa diharapkan.",
        "Saya merasa gelisah.",
        "Saya merasa sulit untuk bersantai.",
        "Saya merasa sedih dan tertekan.",
        "Saya tidak toleran terhadap sesuatu yang menghalangi saya untuk menyelesaikan sesuatu.",
        "Saya merasa saya hampir panik.",
        "Saya tidak bisa merasa antusias terhadap apa pun.",
        "Saya merasa saya tidak berharga.",
        "Saya merasa mudah tersinggung.",
        "Saya merasa berdebar-debar (misalnya jantung berdetak kencang).",
        "Saya merasa takut tanpa alasan yang baik.",
        "Saya merasa hidup tidak berarti."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScreeningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupListeners()
        checkToday()
    }

    private fun setupRecycler() {
        val dassQuestions = questions.mapIndexed { index, text ->
            DassQuestion(index + 1, text)
        }
        adapter = DassAdapter(dassQuestions) { position, value ->
            answers[position] = value
        }
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { submitScreening() }
    }

    private fun checkToday() {
        lifecycleScope.launch {
            try {
                if (screeningRepository.hasTodayScreening()) {
                    Toast.makeText(this@ScreeningActivity, "Skrining hari ini sudah dilakukan.", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                // jika gagal cek, biarkan user lanjut
            }
        }
    }

    private fun submitScreening() {
        if (answers.any { it == -1 }) {
            Toast.makeText(this, "Lengkapi semua pertanyaan.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val scores = calculateDassScores(answers.toList())
                screeningRepository.saveDailyScreening(
                    answers = answers.toList(),
                    scores = scores,
                    note = null
                )
                showResultDialog(scores)
            } catch (e: Exception) {
                Toast.makeText(this@ScreeningActivity, "Gagal simpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun calculateDassScores(ans: List<Int>): DassScore {
        val depressionIdx = listOf(3, 5, 10, 13, 16, 17, 21).map { it - 1 }
        val anxietyIdx = listOf(2, 4, 7, 9, 15, 19, 20).map { it - 1 }
        val stressIdx = listOf(1, 6, 8, 11, 12, 14, 18).map { it - 1 }

        val dep = depressionIdx.sumOf { ans[it] } * 2
        val anx = anxietyIdx.sumOf { ans[it] } * 2
        val str = stressIdx.sumOf { ans[it] } * 2

        return DassScore(
            depression = dep,
            anxiety = anx,
            stress = str,
            severityDepression = severityDepression(dep),
            severityAnxiety = severityAnxiety(anx),
            severityStress = severityStress(str)
        )
    }

    private fun severityDepression(score: Int) = when {
        score >= 28 -> "Ekstrem"
        score >= 21 -> "Berat"
        score >= 14 -> "Sedang"
        score >= 10 -> "Ringan"
        else -> "Normal"
    }

    private fun severityAnxiety(score: Int) = when {
        score >= 20 -> "Ekstrem"
        score >= 15 -> "Berat"
        score >= 10 -> "Sedang"
        score >= 8 -> "Ringan"
        else -> "Normal"
    }

    private fun severityStress(score: Int) = when {
        score >= 34 -> "Ekstrem"
        score >= 26 -> "Berat"
        score >= 19 -> "Sedang"
        score >= 15 -> "Ringan"
        else -> "Normal"
    }

    private fun showResultDialog(scores: DassScore) {
        val msg = """
            Depresi: ${scores.depression} (${scores.severityDepression})
            Kecemasan: ${scores.anxiety} (${scores.severityAnxiety})
            Stres: ${scores.stress} (${scores.severityStress})
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Hasil Skrining")
            .setMessage(msg)
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
                finish()
            }
            .show()
    }
}
