package com.android.capstone.sereluna.ui.viewmodel

import android.net.Uri
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.model.Chat
import com.android.capstone.sereluna.data.ml.SentimentAnalyzer
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.data.utils.FileUtil
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

    private val _isThinking = MutableLiveData(false)
    val isThinking: LiveData<Boolean> = _isThinking

    private val _pendingImageUri = MutableLiveData<Uri?>(null)
    val pendingImageUri: LiveData<Uri?> = _pendingImageUri

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
        _pendingImageUri.value = null
    }

    fun setPendingImage(uri: Uri?) {
        _pendingImageUri.value = uri
    }

    private val understandingContextMessages = listOf(
        "Memahami konteks...",
        "Menganalisis pesanmu...",
        "Meresapi ceritamu...",
        "Menyelami perasaanmu...",
        "Membedah curhatanmu...",
        "Mengingat memori sebelumnya...",
        "Menelaah kata-katamu..."
    )

    private val preparingResponseMessages = listOf(
        "Menyusun respon...",
        "Menyiapkan pesan...",
        "Merangkai kata-kata...",
        "Sedang mengetik...",
        "Mencari saran terbaik...",
        "Hampir selesai...",
        "Menghaluskan balasan...",
        "Menyusun kalimat penyemangat...",
        "Menyesuaikan nada bicara...",
        "Mencocokkan dengan perasaanmu...",
        "Sedang merenungkan balasan...",
        "Mempersiapkan dukungan untukmu..."
    )

    fun sendMessage(userMessage: String, context: Context? = null) {
        if (sessionClosedFlag) {
            _errorState.value = "Sesi ini sudah ditutup. Mari mulai sesi baru."
            return
        }
        if (userMessageCount >= maxMessagesPerSession) {
            _errorState.value = "Batas percakapan tercapai. Silakan selesaikan sesi ini."
            return
        }
        
        val imageUriToUpload = _pendingImageUri.value
        _pendingImageUri.value = null // clear early so UI updates
        
        // MIGHT DO: send image URI to Chat model to display locally
        addMessageToList(Chat(userMessage, "user", false, null, imageUriToUpload))
        userMessageCount++

        val mood = sentimentAnalyzer.analyze(userMessage).first

        _isThinking.value = true
        viewModelScope.launch {
            addMessageToList(Chat("Typing...", "bot", true))
            
            val typingJob = launch {
                if (imageUriToUpload != null) {
                    updateLastMessageStatus("Mengunggah gambar...")
                    kotlinx.coroutines.delay(3000)
                }
                
                // 0-3s: bubble animation only (status is null)
                kotlinx.coroutines.delay(3000)
                
                // 3-6s: understanding context (randomize once)
                val understandingStatus = understandingContextMessages.random()
                updateLastMessageStatus(understandingStatus)
                kotlinx.coroutines.delay(3000)
                
                // >6s: preparing response (randomize every 3 seconds)
                while (true) {
                    val preparingStatus = preparingResponseMessages.random()
                    updateLastMessageStatus(preparingStatus)
                    kotlinx.coroutines.delay(3000)
                }
            }

            try {
                var mediaIds: List<String>? = null
                var hasImage = false
                
                if (imageUriToUpload != null && context != null) {
                    val file = FileUtil.uriToFile(context, imageUriToUpload)
                    if (file != null) {
                        val mediaResponse = repository.uploadMediaImage(file)
                        if (mediaResponse.media_id.isNotEmpty()) {
                            mediaIds = listOf(mediaResponse.media_id)
                            hasImage = true
                        }
                    }
                }

                val response = repository.sendChat(
                    text = userMessage,
                    roomId = activeRoomId,
                    sessionId = activeSessionId,
                    moodSignal = mood,
                    hasImage = hasImage,
                    mediaIds = mediaIds
                )
                typingJob.cancel()
                removeTypingIndicator()
                
                activeRoomId = response.room_id ?: activeRoomId
                activeSessionId = response.session_id ?: activeSessionId
                previousSessionSummary = response.session_summary
                _previousSummary.value = response.session_summary

                if (response.reply.isNotBlank()) {
                    addMessageToList(Chat(
                        message = response.reply,
                        senderId = "bot",
                        isBot = true,
                        riskLevel = response.clinical_insight.risk_level,
                        suggestedAction = response.ui_metadata.suggested_action
                    ))
                } else {
                    _errorState.value = "Maaf, Sereluna sedang kehilangan fokus. Coba lagi ya."
                }
            } catch (e: Exception) {
                typingJob.cancel()
                removeTypingIndicator()
                _errorState.value = "Koneksi terganggu. Pastikan backend Sereluna aktif."
            } finally {
                _isThinking.value = false
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

    private fun updateLastMessageStatus(status: String) {
        val list = _chatMessages.value ?: mutableListOf()
        if (list.isNotEmpty() && list.last().isBot && list.last().message == "Typing...") {
            list[list.size - 1] = list.last().copy(status = status)
            _chatMessages.value = list
        }
    }

    private fun removeTypingIndicator() {
        val list = _chatMessages.value ?: mutableListOf()
        if (list.isNotEmpty() && list.last().isBot && list.last().message == "Typing...") {
            list.removeAt(list.size - 1)
            _chatMessages.value = list
        }
    }
}
