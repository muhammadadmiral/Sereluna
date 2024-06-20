package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityDiaryEmptyBinding
import com.android.capstone.sereluna.ui.diary.ChatbotActivity

class DiaryEmptyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryEmptyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryEmptyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAddDiary.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
    }
}
