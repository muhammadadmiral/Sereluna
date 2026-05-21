package com.android.capstone.sereluna.ui.diary

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import com.android.capstone.sereluna.data.api.ScreeningResponseDto
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityScreeningBinding
import kotlinx.coroutines.launch

class ScreeningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScreeningBinding
    private val repository = SerelunaRepository()
    private lateinit var adapter: DassAdapter
    private var answers = IntArray(21) { -1 }
    private val fallbackQuestions = listOf(
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
        loadScreeningStatus()
        loadQuestionnaire()
    }

    private fun setupRecycler(questionTexts: List<String> = fallbackQuestions) {
        answers = IntArray(questionTexts.size) { -1 }
        val dassQuestions = questionTexts.mapIndexed { index, text ->
            DassQuestion(index + 1, text)
        }
        adapter = DassAdapter(dassQuestions) { position, value ->
            answers[position] = value
            updateProgress()
        }
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter
        binding.screeningProgress.max = questionTexts.size
        updateProgress()
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { submitScreening() }
    }

    private fun updateProgress() {
        val answered = answers.count { it in 0..3 }
        binding.screeningProgress.progress = answered
        binding.tvProgressText.text = "$answered dari ${answers.size} terjawab"
        binding.btnSubmit.alpha = if (answered == answers.size) 1f else 0.72f
    }

    private fun loadScreeningStatus() {
        lifecycleScope.launch {
            try {
                val status = repository.getScreeningStatus()
                binding.tvSubtitle.text = status.disclaimer
                if (!status.is_due) {
                    Toast.makeText(
                        this@ScreeningActivity,
                        "Baseline DASS-21 masih aktif. Rekomendasi berikutnya: ${status.next_recommended_date ?: "-"}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                // jika gagal cek, biarkan user lanjut
            }
        }
    }

    private fun loadQuestionnaire() {
        lifecycleScope.launch {
            try {
                val questionnaire = repository.getDass21Questionnaire()
                binding.tvSubtitle.text = listOf(
                    questionnaire.instructions,
                    questionnaire.disclaimer
                ).filter { it.isNotBlank() }.joinToString("\n\n")

                val loadedQuestions = questionnaire.questions
                    .sortedBy { it.id }
                    .map { it.text }
                    .filter { it.isNotBlank() }
                if (loadedQuestions.size == 21) {
                    setupRecycler(loadedQuestions)
                }
            } catch (e: Exception) {
                // fallback pertanyaan lokal tetap dipakai
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
                binding.btnSubmit.isEnabled = false
                val response = repository.submitScreening(
                    answers = answers.toList(),
                    note = null
                )
                showResultDialog(response)
            } catch (e: Exception) {
                binding.btnSubmit.isEnabled = true
                Toast.makeText(this@ScreeningActivity, "Gagal simpan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showResultDialog(response: ScreeningResponseDto) {
        val scores = response.scores
        val severity = response.severity
        val msg = """
            Area yang perlu diperhatikan:
            Depresi: ${scores["depression"] ?: 0} (${severity["depression"] ?: "-"})
            Kecemasan: ${scores["anxiety"] ?: 0} (${severity["anxiety"] ?: "-"})
            Stres: ${scores["stress"] ?: 0} (${severity["stress"] ?: "-"})

            ${response.disclaimer ?: "DASS-21 adalah alat screening, bukan diagnosis medis."}
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Ringkasan Screening")
            .setMessage(msg)
            .setPositiveButton("OK") { d, _ ->
                d.dismiss()
                finish()
            }
            .show()
    }
}
