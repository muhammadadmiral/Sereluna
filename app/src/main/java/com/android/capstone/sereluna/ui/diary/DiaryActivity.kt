package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.adapter.DiaryAdapter
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ActivityDiaryBinding

class DiaryActivity: AppCompatActivity() {

    private lateinit var binding:ActivityDiaryBinding
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

        diaryAdapter.submitList(getMockData())

        binding.fab.setOnClickListener{
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
    }


    }

    private fun getMockData(): List<Diary> {
        return listOf(
            Diary("1", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short) ),
            Diary("2", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short), ),
            Diary("3", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short), ),
            Diary("4", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short), ),
            Diary("5", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short), ),
            Diary("6", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short), ),
            Diary("7", "2024-06-03T05:32:39.038Z", getString(R.string.lorem_ipsum_short), ),

            )
    }

}