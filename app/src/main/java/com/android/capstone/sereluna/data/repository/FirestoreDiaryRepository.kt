package com.android.capstone.sereluna.data.repository

import com.android.capstone.sereluna.data.model.ChatMessage
import com.android.capstone.sereluna.data.model.ChatSession
import com.android.capstone.sereluna.data.model.Diary
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreDiaryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userDiaries(userId: String) =
        firestore.collection("users").document(userId).collection("diaries")

    private fun diaryChatSessions(userId: String, diaryId: String) =
        userDiaries(userId).document(diaryId).collection("chatSessions")

    private fun sessionMessages(userId: String, diaryId: String, sessionId: String) =
        diaryChatSessions(userId, diaryId).document(sessionId).collection("messages")

    fun getOrCreateDiaryForDate(
        userId: String,
        date: String,
        onResult: (Result<String>) -> Unit
    ) {
        userDiaries(userId)
            .whereEqualTo("date", date)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    onResult(Result.success(doc.id))
                } else {
                    val now = Date()
                    val diary = Diary(
                        date = date,
                        title = "Diary $date",
                        content = "",
                        createdAt = now,
                        updatedAt = now
                    )
                    userDiaries(userId)
                        .add(diary)
                        .addOnSuccessListener { ref -> onResult(Result.success(ref.id)) }
                        .addOnFailureListener { e -> onResult(Result.failure(e)) }
                }
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun startChatSession(
        userId: String,
        diaryId: String,
        model: String,
        onResult: (Result<String>) -> Unit
    ) {
        val session = ChatSession(
            diaryId = diaryId,
            model = model,
            startedAt = Date()
        )
        diaryChatSessions(userId, diaryId)
            .add(session)
            .addOnSuccessListener { ref -> onResult(Result.success(ref.id)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun addMessage(
        userId: String,
        diaryId: String,
        sessionId: String,
        message: ChatMessage,
        onComplete: (Result<Unit>) -> Unit
    ) {
        sessionMessages(userId, diaryId, sessionId)
            .add(message.copy(createdAt = message.createdAt ?: Date()))
            .addOnSuccessListener { onComplete(Result.success(Unit)) }
            .addOnFailureListener { e -> onComplete(Result.failure(e)) }
    }

    fun saveSummary(
        userId: String,
        diaryId: String,
        sessionId: String,
        summary: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val updates = mapOf(
            "summary" to summary,
            "endedAt" to Date()
        )
        diaryChatSessions(userId, diaryId)
            .document(sessionId)
            .update(updates)
            .addOnSuccessListener {
                userDiaries(userId).document(diaryId)
                    .update(
                        mapOf(
                            "chatSummary" to summary,
                            "updatedAt" to Date()
                        )
                    )
                    .addOnSuccessListener { onComplete(Result.success(Unit)) }
                    .addOnFailureListener { e -> onComplete(Result.failure(e)) }
            }
            .addOnFailureListener { e -> onComplete(Result.failure(e)) }
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
            com.google.firebase.firestore.SetOptions.merge()
        )
    }
}
