package com.android.capstone.sereluna.ui.chatbot

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.ChatAdapter
import com.android.capstone.sereluna.data.api.ChatRequest
import com.android.capstone.sereluna.data.api.ChatResponse
import com.android.capstone.sereluna.data.api.ChatbotApiService
import com.android.capstone.sereluna.data.model.Chat
import com.android.capstone.sereluna.databinding.ActivityChatbotBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadProfileData()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.submitFab.setOnClickListener {
            val userMessage = binding.diaryEditText.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessageToChatList(Chat(userMessage, "user", false))
                sendMessageToChatbot(userMessage)
                binding.diaryEditText.text.clear()
            } else {
                Toast.makeText(this, "Please write something...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadProfileData() {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)

            userRef.get().addOnSuccessListener { document: DocumentSnapshot? ->
                if (document != null && document.exists()) {
                    val photoUrl = document.getString("photoUrl") ?: ""
                    if (photoUrl.isNotEmpty()) {
                        Picasso.get().load(photoUrl).into(binding.profileImageDiary)
                    }
                }
            }.addOnFailureListener { exception: Exception ->
                Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun addMessageToChatList(message: Chat) {
        chatList.add(message)
        chatAdapter.notifyItemInserted(chatList.size - 1)
        binding.recyclerView.scrollToPosition(chatList.size - 1)
    }

    private fun sendMessageToChatbot(message: String) {
        val apiService = ChatbotApiService.create()
        val chatRequest = ChatRequest(message)

        apiService.sendMessage(chatRequest).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        val botMessage = it.journal.suggestion
                        addMessageToChatList(Chat(botMessage, "bot", true))
                    }
                } else {
                    Toast.makeText(this@ChatbotActivity, "Failed to get response from bot", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Toast.makeText(this@ChatbotActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
