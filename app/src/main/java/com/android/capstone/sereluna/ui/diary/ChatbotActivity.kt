package com.android.capstone.sereluna.ui.diary

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityChatbotBinding

class ChatbotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish() // Kembali ke aktivitas sebelumnya
        }
    }
}
