package com.android.capstone.sereluna.data.repository

import com.android.capstone.sereluna.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository(auth, firestore)
) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun getOrCreateDiaryForDate(date: String): String {
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val diaryRef = firestore.collection("users").document(userId).collection("diaries")
        val querySnapshot = diaryRef.whereEqualTo("date", date).get().await()

        return if (querySnapshot.isEmpty) {
            val newDiary = hashMapOf("date" to date, "createdAt" to FieldValue.serverTimestamp())
            val documentReference = diaryRef.add(newDiary).await()
            documentReference.id
        } else {
            querySnapshot.documents.first().id
        }
    }

    suspend fun startChatSession(diaryId: String, modelName: String): String {
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val sessionRef = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
            .collection("sessions")

        val newSession = hashMapOf(
            "model" to modelName,
            "startTime" to FieldValue.serverTimestamp()
        )
        val documentReference = sessionRef.add(newSession).await()
        return documentReference.id
    }

    suspend fun addMessageToHistory(diaryId: String, sessionId: String, message: ChatMessage) {
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val messagesRef = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
            .collection("sessions").document(sessionId)
            .collection("messages")

        val messageData = hashMapOf(
            "role" to message.role,
            "text" to message.text,
            "createdAt" to (message.createdAt ?: Date())
        )
        messagesRef.add(messageData).await()
    }

    suspend fun saveSessionSummary(diaryId: String, sessionId: String, summary: String) {
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val sessionRef = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
            .collection("sessions").document(sessionId)

        val data = hashMapOf(
            "summary" to summary,
            "endTime" to FieldValue.serverTimestamp()
        )
        sessionRef.set(data, SetOptions.merge()).await()

        // also store summary to diary root for listing
        val diaryRef = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
        diaryRef.set(mapOf("chatSummary" to summary), SetOptions.merge()).await()
        savePersonalContext(summary)
        notificationRepository.addNotification(
            title = "Diary AI tersimpan",
            body = "Curhatanmu sudah diringkas dan disimpan sebagai konteks personal.",
            type = "diary"
        )
    }

    suspend fun updateRollingSummary(diaryId: String, sessionId: String, summary: String) {
        if (summary.isBlank()) return
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val sessionRef = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
            .collection("sessions").document(sessionId)

        val data = mapOf(
            "summary" to summary,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        sessionRef.set(data, SetOptions.merge()).await()

        val diaryRef = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
        diaryRef.set(
            mapOf(
                "chatSummary" to summary,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        savePersonalContext(summary)
    }

    suspend fun getLatestSessionSummary(diaryId: String): String? {
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val sessions = firestore.collection("users").document(userId)
            .collection("diaries").document(diaryId)
            .collection("sessions")
            .orderBy("endTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        val doc = sessions.documents.firstOrNull() ?: return null
        return doc.getString("summary")
    }

    private suspend fun savePersonalContext(summary: String) {
        if (summary.isBlank()) return
        val userId = getUserId() ?: throw IllegalStateException("User not logged in")
        val userRef = firestore.collection("users").document(userId)
        val data = mapOf(
            "latestDiarySummary" to summary,
            "personalContext" to summary,
            "personalContextUpdatedAt" to FieldValue.serverTimestamp()
        )
        userRef.set(data, SetOptions.merge()).await()

        userRef.collection("personalContexts")
            .add(
                mapOf(
                    "type" to "diary_summary",
                    "summary" to summary,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    fun appendSleepDailyMetric(userId: String, date: String, sleepQuality: String, hours: Long) {
        val dateKey = date.ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        val docRef = firestore.collection("users")
            .document(userId)
            .collection("analytics")
            .document("dailyMetrics")
            .collection("dates")
            .document(dateKey)

        docRef.set(
            mapOf(
                "sleepQuality" to sleepQuality,
                "totalSleepHours" to hours,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }
}
