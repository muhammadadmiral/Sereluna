package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.android.capstone.sereluna.data.adapter.DiaryAdapter
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ActivityDiaryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var diaryAdapter: DiaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please login first.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(userId).collection("diaries")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents != null) {
                    val diaryList = documents.map { doc ->
                        val d = doc.toObject(Diary::class.java)
                        d.id = doc.id
                        d
                    }
                    diaryAdapter.submitList(diaryList)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load diaries: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
