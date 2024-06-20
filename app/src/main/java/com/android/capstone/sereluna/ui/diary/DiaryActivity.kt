package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.adapter.DiaryAdapter
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ActivityDiaryBinding

class DiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryBinding
    private val diaryAdapter = DiaryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvDiary.adapter = diaryAdapter

        binding.rvDiary.apply {
            layoutManager = LinearLayoutManager(this@DiaryActivity)
            setHasFixedSize(true)
            adapter = diaryAdapter
        }

        val diaryList = getMockData()
        if (diaryList.isEmpty()) {
            // Jika daftar diary kosong, arahkan ke DiaryEmptyActivity
            val intent = Intent(this, DiaryEmptyActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            diaryAdapter.submitList(diaryList)
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getMockData(): List<Diary> {
        // Return an empty list for demonstration
        return listOf()
    }
}
