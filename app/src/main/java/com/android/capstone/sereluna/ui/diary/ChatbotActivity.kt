package com.android.capstone.sereluna.ui.diary

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.ChatAdapter
import com.android.capstone.sereluna.databinding.ActivityChatbotBinding
import com.android.capstone.sereluna.ui.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupListeners()
        setupObservers()
        loadProfileData()
    }

    private fun setupRecyclerView() {
        // The adapter is now initialized with an empty list, it will be updated by LiveData
        chatAdapter = ChatAdapter(emptyList())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.submitFab.setOnClickListener {
            val userMessage = binding.diaryEditText.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                viewModel.sendMessage(userMessage)
                binding.diaryEditText.text?.clear()
            } else {
                Toast.makeText(this, "Please write something...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            viewModel.finishSession()
            finish()
        }

        // TODO: Re-implement finish and summarize logic
        binding.btnFinish.setOnClickListener {
            viewModel.finishSession()
            Toast.makeText(this, "Meringkas sesi dan menyimpan ke diary...", Toast.LENGTH_SHORT).show()
            // jangan langsung finish untuk memberi waktu summary tersimpan
        }
        binding.btnSummarize.visibility = View.GONE
    }

    private fun setupObservers() {
        viewModel.chatMessages.observe(this) { messages ->
            chatAdapter.updateMessages(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }

        viewModel.errorState.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.sessionClosed.observe(this) { closed ->
            if (closed) {
                binding.diaryEditText.isEnabled = false
                binding.submitFab.isEnabled = false
                Toast.makeText(this, "Sesi ditutup dan diringkas ke diary.", Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun loadProfileData() {
        val user = auth.currentUser
        if (user != null) {
            val userRef = firestore.collection("users").document(user.uid)
            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val photoUrl = document.getString("photoUrl") ?: ""
                    if (photoUrl.isNotEmpty()) {
                        Picasso.get().load(photoUrl).into(binding.profileImageDiary)
                    }
                }
            }
        }
    }
}
