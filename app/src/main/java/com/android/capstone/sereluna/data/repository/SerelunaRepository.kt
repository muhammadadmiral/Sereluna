package com.android.capstone.sereluna.data.repository

import com.android.capstone.sereluna.data.api.ChatFinishRequestDto
import com.android.capstone.sereluna.data.api.ChatRequestDto
import com.android.capstone.sereluna.data.api.ChatResponseDto
import com.android.capstone.sereluna.data.api.ScreeningRequestDto
import com.android.capstone.sereluna.data.api.ScreeningResponseDto
import com.android.capstone.sereluna.data.api.SerelunaApi
import com.android.capstone.sereluna.data.api.SerelunaApiClient
import com.android.capstone.sereluna.data.api.UserContextResponseDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class SerelunaRepository(
    private val api: SerelunaApi = SerelunaApiClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private suspend fun authHeader(): String {
        val user = auth.currentUser ?: error("User belum login")
        val token = user.getIdToken(false).await().token ?: error("Firebase ID token kosong")
        return "Bearer $token"
    }

    suspend fun sendChat(
        text: String,
        roomId: String?,
        sessionId: String?,
        moodSignal: String?
    ): ChatResponseDto {
        return api.chat(
            authorization = authHeader(),
            request = ChatRequestDto(
                text = text,
                room_id = roomId,
                session_id = sessionId,
                mood_signal = moodSignal,
                mode = "chat"
            )
        )
    }

    suspend fun finishChat(roomId: String, sessionId: String): ChatResponseDto {
        return api.finishChat(
            authorization = authHeader(),
            request = ChatFinishRequestDto(
                room_id = roomId,
                session_id = sessionId
            )
        )
    }

    suspend fun submitScreening(answers: List<Int>, note: String?): ScreeningResponseDto {
        return api.submitScreening(
            authorization = authHeader(),
            request = ScreeningRequestDto(
                answers = answers,
                note = note
            )
        )
    }

    suspend fun getContext(): UserContextResponseDto {
        return api.getContext(authHeader())
    }
}
