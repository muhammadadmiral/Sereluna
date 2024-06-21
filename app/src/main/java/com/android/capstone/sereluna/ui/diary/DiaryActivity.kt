package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.DiaryAdapter
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ActivityDiaryBinding
import com.android.capstone.sereluna.ui.chatbot.ChatbotActivity
import com.google.firebase.firestore.FirebaseFirestore

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

        loadDiaries()

        binding.fab.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDiaries() {
        val db = FirebaseFirestore.getInstance()
        db.collection("diaries").get()
            .addOnSuccessListener { result ->
                val diaryList = mutableListOf<Diary>()
                for (document in result) {
                    val diary = document.toObject(Diary::class.java)
                    diary.id = document.id // Atur ID dokumen secara manual
                    diaryList.add(diary)
                }
                diaryAdapter.submitList(diaryList)
            }
            .addOnFailureListener { exception ->
                // Tangani kegagalan
            }
    }
}
