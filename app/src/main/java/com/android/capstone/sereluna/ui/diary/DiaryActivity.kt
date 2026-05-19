package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.DiaryGroupAdapter
import com.android.capstone.sereluna.data.model.DiaryDayGroup
import com.android.capstone.sereluna.data.model.DiaryFeedItem
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityDiaryBinding
import kotlinx.coroutines.launch

class DiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryBinding
    private val repository = SerelunaRepository()
    private lateinit var diaryAdapter: DiaryGroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadDiaries()
    }

    private fun setupRecyclerView() {
        diaryAdapter = DiaryGroupAdapter()
        diaryAdapter.onSeeMoreClick = { item ->
            startActivity(
                Intent(this, DiaryDetailActivity::class.java).apply {
                    putExtra(DiaryDetailActivity.EXTRA_DIARY_ID, item.diaryId)
                    putExtra(DiaryDetailActivity.EXTRA_SESSION_ID, item.sessionId)
                    putExtra(DiaryDetailActivity.EXTRA_DATE, item.date)
                    putExtra(DiaryDetailActivity.EXTRA_SUMMARY, item.summary)
                    putExtra(DiaryDetailActivity.EXTRA_PREVIEW, item.preview)
                    putExtra(DiaryDetailActivity.EXTRA_STATUS, item.status)
                    putExtra(DiaryDetailActivity.EXTRA_MODEL, item.model)
                    putExtra(DiaryDetailActivity.EXTRA_START_TIME, item.startTime)
                    putExtra(DiaryDetailActivity.EXTRA_END_TIME, item.endTime)
                    putExtra(DiaryDetailActivity.EXTRA_UPDATED_AT, item.updatedAt)
                }
            )
        }
        binding.rvDiary.apply {
            layoutManager = LinearLayoutManager(this@DiaryActivity)
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
                val grouped = repository.getDiaryEntries()
                    .map { item ->
                        DiaryFeedItem(
                            id = item.id.ifBlank { "${item.diary_id}:${item.session_id}" },
                            diaryId = item.diary_id,
                            sessionId = item.session_id,
                            date = item.date,
                            summary = item.summary,
                            preview = item.preview,
                            status = item.status,
                            model = item.model,
                            startTime = item.start_time,
                            endTime = item.end_time,
                            updatedAt = item.updated_at
                        )
                    }
                    .sortedWith(
                        compareByDescending<DiaryFeedItem> { it.date }
                            .thenByDescending { it.updatedAt.orEmpty() }
                    )
                    .groupBy { it.date }
                    .map { (date, entries) ->
                        DiaryDayGroup(
                            date = date,
                            entries = entries
                        )
                    }

                diaryAdapter.submitList(grouped)
            } catch (e: Exception) {
                Toast.makeText(this@DiaryActivity, "Failed to load diaries: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
