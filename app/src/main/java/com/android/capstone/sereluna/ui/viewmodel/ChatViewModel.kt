package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.api.ChatRequest
import com.android.capstone.sereluna.data.api.ChatbotApiService
import com.android.capstone.sereluna.data.model.Chat
import com.android.capstone.sereluna.data.repository.ChatMessage
import com.android.capstone.sereluna.data.repository.ChatRepository
import com.android.capstone.sereluna.data.repository.ScreeningRepository
import com.android.capstone.sereluna.data.repository.UserRepository
import com.android.capstone.sereluna.data.ml.SentimentAnalyzer
import com.android.capstone.sereluna.data.ml.RiskNaiveBayes
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()
    private val screeningRepository = ScreeningRepository()
    private val userRepository = UserRepository()
    private val apiService = ChatbotApiService.create()
    private val sentimentAnalyzer = SentimentAnalyzer()
    private val riskNaiveBayes = RiskNaiveBayes()

    private val _chatMessages = MutableLiveData<MutableList<Chat>>(mutableListOf())
    val chatMessages: LiveData<MutableList<Chat>> = _chatMessages

    private val _errorState = MutableLiveData<String?>()
    val errorState: LiveData<String?> = _errorState

    private val _sessionClosed = MutableLiveData(false)
    val sessionClosed: LiveData<Boolean> = _sessionClosed

    private val _previousSummary = MutableLiveData<String?>()
    val previousSummary: LiveData<String?> = _previousSummary
    private var profileContext: String? = null

    private var diaryId: String? = null
    private var sessionId: String? = null
    private val modelName = "gemini-1.5-flash-dass21-hybrid-v8"
    private var latestScreeningSummary: String? = null
    private var previousSessionSummary: String? = null
    private var userName: String? = null
    private var userMessageCount: Int = 0
    private val maxMessagesPerSession = 10
    private var sessionClosedFlag: Boolean = false

    init {
        startNewChatSession()
        fetchScreeningContext()
    }

    private fun startNewChatSession() {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                diaryId = chatRepository.getOrCreateDiaryForDate(today)
                previousSessionSummary = chatRepository.getLatestSessionSummary(diaryId!!)
                _previousSummary.value = previousSessionSummary
                sessionId = chatRepository.startChatSession(diaryId!!, modelName)
                userMessageCount = 0
                sessionClosedFlag = false
                _sessionClosed.value = false
                fetchUserName()
            } catch (e: Exception) {
                _errorState.value = "Error starting session: ${e.message}"
            }
        }
    }

    private fun fetchUserName() {
        viewModelScope.launch {
            try {
                val uid = userRepository.getCurrentUserId()
                if (uid != null) {
                    val data = userRepository.getUserData(uid)
                    userName = data?.get("name") as? String
                    profileContext = userRepository.getProfileContext(uid)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun fetchScreeningContext() {
        viewModelScope.launch {
            try {
                latestScreeningSummary =
                    screeningRepository.getScreeningHabitContext()
                        ?: screeningRepository.getLatestScreeningSummary()
            } catch (e: Exception) {
                // ignore, context is optional
            }
        }
    }

    fun sendMessage(userMessage: String) {
        if (sessionClosedFlag) {
            _errorState.value = "Sesi ini sudah ditutup. Mulai lagi besok setelah skrining."
            return
        }
        if (userMessageCount >= maxMessagesPerSession) {
            _errorState.value = "Batas 10 pesan per sesi tercapai. Akhiri sesi."
            return
        }
        // Add user message to UI immediately
        addMessageToList(Chat(userMessage, "user", false))
        persistMessage("user", userMessage)
        userMessageCount++

        // Show typing indicator
        addMessageToList(Chat("Typing...", "bot", true))

        val mood = sentimentAnalyzer.analyze(userMessage).first
        val nbRisk = riskNaiveBayes.classify(userMessage)

        // Make API call
        apiService.sendMessage(
            ChatbotApiService.FULL_SCRIPT_URL,
            ChatRequest(
                text = userMessage,
                room_id = diaryId,
                screening_context = latestScreeningSummary,
                session_summary = previousSessionSummary,
                risk_level = nbRisk,
                mood_signal = mood,
                user_name = userName,
                profile_context = profileContext
            )
        )
            .enqueue(object : Callback<com.android.capstone.sereluna.data.api.ChatResponse> {
                override fun onResponse(
                    call: Call<com.android.capstone.sereluna.data.api.ChatResponse>,
                    response: Response<com.android.capstone.sereluna.data.api.ChatResponse>
                ) {
                    // Remove typing indicator
                    removeTypingIndicator()

                    if (response.isSuccessful) {
                        val body = response.body()
                        val botReply = body?.reply
                        if (!botReply.isNullOrEmpty()) {
                            addMessageToList(Chat(botReply, "bot", true))
                            persistMessage("assistant", botReply)
                            val rollingSummary = body.session_summary
                            if (!rollingSummary.isNullOrBlank()) {
                                previousSessionSummary = rollingSummary
                                persistRollingSummary(rollingSummary)
                            }
                        } else {
                            val errorDetails = body?.details ?: "Empty or invalid response."
                            _errorState.value = errorDetails
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        _errorState.value = "API Error ${response.code()}: $errorBody"
                    }
                }

                override fun onFailure(call: Call<com.android.capstone.sereluna.data.api.ChatResponse>, t: Throwable) {
                    removeTypingIndicator()
                    _errorState.value = "Network Failure: ${t.message}"
                }
            })
    }

    fun finishSession() {
        sessionClosedFlag = true
        _sessionClosed.value = true
        viewModelScope.launch {
            try {
                val raw = buildSessionRaw()
                val finalSummary = when {
                    !previousSessionSummary.isNullOrBlank() -> previousSessionSummary!!
                    else -> {
                        val summaryText = requestSummaryFromLlm(raw)
                        if (summaryText.isNotBlank()) summaryText else buildSessionSummary()
                    }
                }
                if (diaryId != null && sessionId != null && finalSummary.isNotBlank()) {
                    chatRepository.saveSessionSummary(diaryId!!, sessionId!!, finalSummary)
                    previousSessionSummary = finalSummary
                    addMessageToList(Chat(finalSummary, "bot", true))
                    _sessionClosed.value = true
                }
            } catch (_: Exception) {
                // ignore summary failure
            }
        }
    }

    private suspend fun requestSummaryFromLlm(raw: String): String {
        return try {
            withContext(Dispatchers.IO) {
                val response = apiService.sendMessage(
                    ChatbotApiService.FULL_SCRIPT_URL,
                    ChatRequest(
                        text = raw,
                        room_id = diaryId,
                        screening_context = latestScreeningSummary,
                        session_summary = previousSessionSummary,
                        risk_level = null,
                        mood_signal = null,
                        mode = "summary",
                        session_raw = raw,
                        user_name = userName,
                        profile_context = profileContext
                    )
                ).execute()
                if (response.isSuccessful) {
                    response.body()?.reply ?: ""
                } else ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun buildSessionSummary(): String {
        val messages = _chatMessages.value.orEmpty()
        val userTexts = messages.filter { it.senderId == "user" }.takeLast(5).joinToString("; ") { it.message }
        val botTexts = messages.filter { it.senderId == "bot" }.takeLast(3).joinToString("; ") { it.message }
        val screen = latestScreeningSummary ?: ""
        return "Screening: $screen | User: $userTexts | Bot: $botTexts"
    }

    private fun buildSessionRaw(): String {
        val messages = _chatMessages.value.orEmpty()
        return messages.joinToString("\n") { msg ->
            val role = if (msg.senderId == "user") "User" else "Bot"
            "$role: ${msg.message}"
        }
    }

    private fun persistMessage(role: String, text: String) {
        if (diaryId == null || sessionId == null) return
        viewModelScope.launch {
            try {
                val chatMessage = ChatMessage(role = role, text = text, createdAt = Date())
                chatRepository.addMessageToHistory(diaryId!!, sessionId!!, chatMessage)
            } catch (e: Exception) {
                // Optionally handle persistence error, though UI is already updated
            }
        }
    }

    private fun persistRollingSummary(summary: String) {
        if (diaryId == null || sessionId == null) return
        viewModelScope.launch {
            try {
                chatRepository.updateRollingSummary(diaryId!!, sessionId!!, summary)
            } catch (_: Exception) {
            }
        }
    }

    private fun addMessageToList(chat: Chat) {
        val list = _chatMessages.value ?: mutableListOf()
        list.add(chat)
        _chatMessages.value = list
    }

    private fun removeTypingIndicator() {
        val list = _chatMessages.value ?: mutableListOf()
        if (list.isNotEmpty() && list.last().isBot && list.last().message == "Typing...") {
            list.removeAt(list.size - 1)
            _chatMessages.value = list
        }
    }
}
