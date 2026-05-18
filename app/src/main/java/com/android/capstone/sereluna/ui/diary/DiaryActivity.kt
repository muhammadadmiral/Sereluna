package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.android.capstone.sereluna.data.adapter.DiaryAdapter
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityDiaryBinding
import kotlinx.coroutines.launch

class DiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryBinding
    private val repository = SerelunaRepository()
    private lateinit var diaryAdapter: DiaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadDiaries()
    }

    private fun setupRecyclerView() {
        diaryAdapter = DiaryAdapter()
        binding.rvDiary.apply {
            layoutManager = GridLayoutManager(this@DiaryActivity, 2)
            adapter = diaryAdapter
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.fab.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun loadDiaries() {
        lifecycleScope.launch {
            try {
                val diaryList = repository.getDiaries().map { item ->
                    Diary(
                        id = item.id,
                        date = item.date,
                        chatSummary = item.chat_summary,
                        content = item.chat_summary
                    )
                }
                diaryAdapter.submitList(diaryList)
            } catch (e: Exception) {
                Toast.makeText(this@DiaryActivity, "Failed to load diaries: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
