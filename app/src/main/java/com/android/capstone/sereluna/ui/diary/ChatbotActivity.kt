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
import com.android.capstone.sereluna.data.model.ChatMessage
import com.android.capstone.sereluna.data.repository.FirestoreDiaryRepository
import com.android.capstone.sereluna.databinding.ActivityChatbotBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var diaryRepository: FirestoreDiaryRepository

    private val chatList = mutableListOf<Chat>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var diaryId: String? = null
    private var sessionId: String? = null
    private val modelName = "open-prototype"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        diaryRepository = FirestoreDiaryRepository(firestore)

        setupRecyclerView()
        setupListeners()
        loadProfileData()
        prepareDiarySession()
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
                handleUserMessage(userMessage)
                binding.diaryEditText.text.clear()
            } else {
                Toast.makeText(this, "Please write something...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSummarize.setOnClickListener {
            summarizeChat()
        }

        binding.btnFinish.setOnClickListener {
            summarizeChat()
            finish()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun prepareDiarySession() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }
        val today = dateFormatter.format(Date())
        diaryRepository.getOrCreateDiaryForDate(user.uid, today) { result ->
            result.onSuccess { diary ->
                diaryId = diary
                diaryRepository.startChatSession(user.uid, diary, modelName) { sessionResult ->
                    sessionResult.onSuccess { id -> sessionId = id }
                    sessionResult.onFailure { e ->
                        Toast.makeText(this, "Failed to start chat session: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.onFailure { e ->
                Toast.makeText(this, "Failed to prepare diary: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleUserMessage(userMessage: String) {
        addMessageToChatList(Chat(userMessage, "user", false))
        persistMessage(role = "user", text = userMessage)
        sendMessageToChatbot(userMessage)
    }

    private fun loadProfileData() {
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
            }.addOnFailureListener {
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
                        persistMessage(role = "assistant", text = botMessage)
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

    private fun persistMessage(role: String, text: String) {
        val uid = auth.currentUser?.uid
        val diary = diaryId
        val session = sessionId
        if (uid == null || diary == null || session == null) return

        diaryRepository.addMessage(
            uid,
            diary,
            session,
            ChatMessage(role = role, text = text, createdAt = Date())
        ) {
            // Swallow result; UI already updated
        }
    }

    private fun summarizeChat() {
        val uid = auth.currentUser?.uid
        val diary = diaryId
        val session = sessionId
        if (uid == null || diary == null || session == null) {
            Toast.makeText(this, "Session not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val summary = generateLocalSummary()
        binding.summaryTextView.text = summary

        diaryRepository.saveSummary(uid, diary, session, summary) { result ->
            result.onFailure {
                Toast.makeText(this, "Failed to save summary: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateLocalSummary(): String {
        if (chatList.isEmpty()) return "No conversation yet."
        val userMessages = chatList.filter { !it.isBot }.takeLast(3).joinToString(" ") { it.message }
        val botMessages = chatList.filter { it.isBot }.takeLast(3).joinToString(" ") { it.message }
        return "You shared: $userMessages. Bot suggested: $botMessages"
    }
}
