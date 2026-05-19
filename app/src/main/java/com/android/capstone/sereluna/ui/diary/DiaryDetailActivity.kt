package com.android.capstone.sereluna.ui.diary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityDiaryDetailBinding

class DiaryDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DIARY_ID = "extra_diary_id"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SUMMARY = "extra_summary"
        const val EXTRA_PREVIEW = "extra_preview"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_MODEL = "extra_model"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_UPDATED_AT = "extra_updated_at"
    }

    private lateinit var binding: ActivityDiaryDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        val diaryId = intent.getStringExtra(EXTRA_DIARY_ID).orEmpty()
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        val date = intent.getStringExtra(EXTRA_DATE).orEmpty()
        val summary = intent.getStringExtra(EXTRA_SUMMARY).orEmpty()
        val preview = intent.getStringExtra(EXTRA_PREVIEW).orEmpty()
        val status = intent.getStringExtra(EXTRA_STATUS).orEmpty()
        val model = intent.getStringExtra(EXTRA_MODEL).orEmpty()
        val updatedAt = intent.getStringExtra(EXTRA_UPDATED_AT).orEmpty()

        binding.tvDetailDate.text = date.ifBlank { "Diary Detail" }
        binding.tvDetailStatus.text = status.ifBlank { "session" }
        binding.tvDetailModel.text = model.ifBlank { "model" }
        binding.tvDetailPreview.text = preview.ifBlank { "Tidak ada preview." }
        binding.tvDetailSummary.text = summary.ifBlank { "Tidak ada ringkasan." }
        binding.tvDetailMeta.text = buildString {
            append("Diary ID: ")
            append(diaryId.ifBlank { "-" })
            append('\n')
            append("Session ID: ")
            append(sessionId.ifBlank { "-" })
            append('\n')
            append("Updated: ")
            append(updatedAt.ifBlank { "-" })
        }
    }
}
