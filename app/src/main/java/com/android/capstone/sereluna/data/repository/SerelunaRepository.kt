package com.android.capstone.sereluna.data.repository

import android.net.Uri
import com.android.capstone.sereluna.data.api.ChatFinishRequestDto
import com.android.capstone.sereluna.data.api.ChatRequestDto
import com.android.capstone.sereluna.data.api.ChatResponseDto
import com.android.capstone.sereluna.data.api.DeviceTokenRequestDto
import com.android.capstone.sereluna.data.api.DiaryDetailDto
import com.android.capstone.sereluna.data.api.DiaryItemDto
import com.android.capstone.sereluna.data.api.DiaryMessagesResponseDto
import com.android.capstone.sereluna.data.api.NotificationItemDto
import com.android.capstone.sereluna.data.api.ScreeningRequestDto
import com.android.capstone.sereluna.data.api.ScreeningResponseDto
import com.android.capstone.sereluna.data.api.SerelunaApi
import com.android.capstone.sereluna.data.api.SerelunaApiClient
import com.android.capstone.sereluna.data.api.SleepDailyItemDto
import com.android.capstone.sereluna.data.api.SleepDailyRequestDto
import com.android.capstone.sereluna.data.api.SuccessResponseDto
import com.android.capstone.sereluna.data.api.UserContextResponseDto
import com.android.capstone.sereluna.data.api.UserProfileResponseDto
import com.android.capstone.sereluna.data.api.UserProfileUpdateRequestDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SerelunaRepository(
    private val api: SerelunaApi = SerelunaApiClient.api,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
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

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun getProfile(): UserProfileResponseDto {
        return api.getProfile(authHeader())
    }

    suspend fun getProfileAsMap(): Map<String, Any> {
        val profile = getProfile()
        return mapOf(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "photoUrl" to profile.photo_url,
            "photo_url" to profile.photo_url,
            "provider" to profile.provider,
            "latestScreeningSummary" to profile.latest_screening_summary,
            "latestDiarySummary" to profile.latest_diary_summary,
            "personalContext" to profile.personal_context,
            "hasScreeningToday" to profile.has_screening_today
        )
    }

    suspend fun updateProfile(name: String, newImageUri: Uri?): UserProfileResponseDto {
        val photoUrl = if (newImageUri != null) uploadProfileImage(newImageUri) else null
        return api.updateProfile(
            authorization = authHeader(),
            request = UserProfileUpdateRequestDto(
                name = name,
                photo_url = photoUrl
            )
        )
    }

    suspend fun getDiaries(limit: Int = 30): List<DiaryItemDto> {
        return api.getDiaries(authHeader(), limit).items
    }

    suspend fun getDiaryDetail(diaryId: String): DiaryDetailDto {
        return api.getDiaryDetail(authHeader(), diaryId)
    }

    suspend fun getDiaryMessages(diaryId: String, sessionId: String): DiaryMessagesResponseDto {
        return api.getDiaryMessages(authHeader(), diaryId, sessionId)
    }

    suspend fun getNotifications(limit: Int = 30): List<NotificationItemDto> {
        return api.getNotifications(authHeader(), limit).items
    }

    suspend fun markNotificationRead(notificationId: String): SuccessResponseDto {
        return api.markNotificationRead(authHeader(), notificationId)
    }

    suspend fun submitDeviceToken(token: String): SuccessResponseDto {
        return api.submitDeviceToken(
            authorization = authHeader(),
            request = DeviceTokenRequestDto(token)
        )
    }

    suspend fun submitSleepDaily(
        date: String,
        sleepQuality: String,
        totalSleepHours: Long
    ) {
        api.submitSleepDaily(
            authorization = authHeader(),
            request = SleepDailyRequestDto(
                date = date,
                sleep_quality = sleepQuality,
                total_sleep_hours = totalSleepHours
            )
        )
    }

    suspend fun getSleepDaily(limit: Int = 14): List<SleepDailyItemDto> {
        return api.getSleepDaily(authHeader(), limit).items
    }

    private suspend fun uploadProfileImage(imageUri: Uri): String {
        val userId = getCurrentUserId() ?: error("User belum login")
        val storageRef = storage.reference.child("profile_images/$userId")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    companion object {
        fun todayString(date: Date = Date()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }
}
