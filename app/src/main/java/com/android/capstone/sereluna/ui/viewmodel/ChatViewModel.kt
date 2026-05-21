package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.model.Chat
import com.android.capstone.sereluna.data.ml.SentimentAnalyzer
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = SerelunaRepository()
    private val sentimentAnalyzer = SentimentAnalyzer()

    private val _chatMessages = MutableLiveData<MutableList<Chat>>(mutableListOf())
    val chatMessages: LiveData<MutableList<Chat>> = _chatMessages

    private val _errorState = MutableLiveData<String?>()
    val errorState: LiveData<String?> = _errorState

    private val _sessionClosed = MutableLiveData(false)
    val sessionClosed: LiveData<Boolean> = _sessionClosed

    private val _previousSummary = MutableLiveData<String?>()
    val previousSummary: LiveData<String?> = _previousSummary

    private var activeRoomId: String? = null
    private var activeSessionId: String? = null
    private var previousSessionSummary: String? = null
    private var userMessageCount: Int = 0
    private val maxMessagesPerSession = 15
    private var sessionClosedFlag: Boolean = false

    init {
        startNewChatSession()
    }

    private fun startNewChatSession() {
        activeRoomId = null
        activeSessionId = null
        previousSessionSummary = null
        _previousSummary.value = null
        userMessageCount = 0
        sessionClosedFlag = false
        _sessionClosed.value = false
    }

    private val thinkingStates = listOf(
        "Sedang berpikir...",
        "Mengambil konteks jurnal...",
        "Menganalisis suasana hati...",
        "Mempersiapkan saran terbaik..."
    )

    fun sendMessage(userMessage: String) {
        if (sessionClosedFlag) {
            _errorState.value = "Sesi ini sudah ditutup. Mari mulai sesi baru."
            return
        }
        if (userMessageCount >= maxMessagesPerSession) {
            _errorState.value = "Batas percakapan tercapai. Silakan selesaikan sesi ini."
            return
        }
        addMessageToList(Chat(userMessage, "user", false))
        userMessageCount++

        val typingChat = Chat(thinkingStates[0], "bot", true)
        addMessageToList(typingChat)

        val mood = sentimentAnalyzer.analyze(userMessage).first

        viewModelScope.launch {
            // Background loop to rotate thinking messages if it takes a long time
            val typingJob = launch {
                var stateIndex = 0
                while (true) {
                    kotlinx.coroutines.delay(3000)
                    stateIndex = (stateIndex + 1) % thinkingStates.size
                    updateLastMessage(thinkingStates[stateIndex])
                }
            }

            try {
                val response = repository.sendChat(
                    text = userMessage,
                    roomId = activeRoomId,
                    sessionId = activeSessionId,
                    moodSignal = mood
                )
                typingJob.cancel()
                removeTypingIndicator()
                activeRoomId = response.room_id ?: activeRoomId
                activeSessionId = response.session_id ?: activeSessionId
                previousSessionSummary = response.session_summary
                _previousSummary.value = response.session_summary

                if (response.reply.isNotBlank()) {
                    addMessageToList(Chat(response.reply, "bot", true))
                } else {
                    _errorState.value = "Maaf, Sereluna sedang kehilangan fokus. Coba lagi ya."
                }
            } catch (e: Exception) {
                typingJob.cancel()
                removeTypingIndicator()
                _errorState.value = "Koneksi terganggu. Pastikan backend Sereluna aktif."
            }
        }
    }

    fun finishSession() {
        sessionClosedFlag = true
        viewModelScope.launch {
            try {
                val roomId = activeRoomId
                val sessionId = activeSessionId
                if (roomId.isNullOrBlank() || sessionId.isNullOrBlank()) {
                    _sessionClosed.value = true
                    return@launch
                }

                val response = repository.finishChat(roomId, sessionId)
                previousSessionSummary = response.session_summary
                _previousSummary.value = response.session_summary
                _sessionClosed.value = true
            } catch (e: Exception) {
                _errorState.value = "Gagal menutup sesi. Coba lagi saat koneksi stabil."
                sessionClosedFlag = false
            }
        }
    }

    private fun addMessageToList(chat: Chat) {
        val list = _chatMessages.value ?: mutableListOf()
        list.add(chat)
        _chatMessages.value = list
    }

    private fun updateLastMessage(newMessage: String) {
        val list = _chatMessages.value ?: mutableListOf()
        if (list.isNotEmpty() && list.last().isBot) {
            list[list.size - 1] = list.last().copy(message = newMessage)
            _chatMessages.value = list
        }
    }

    private fun removeTypingIndicator() {
        val list = _chatMessages.value ?: mutableListOf()
        if (list.isNotEmpty() && list.last().isBot && thinkingStates.any { it == list.last().message || list.last().message == "Typing..." }) {
            list.removeAt(list.size - 1)
            _chatMessages.value = list
        }
    }
}
