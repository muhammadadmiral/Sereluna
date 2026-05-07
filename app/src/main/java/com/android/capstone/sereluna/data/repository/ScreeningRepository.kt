package com.android.capstone.sereluna.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DassScore(
    val depression: Int,
    val anxiety: Int,
    val stress: Int,
    val severityDepression: String,
    val severityAnxiety: String,
    val severityStress: String
)

data class ScreeningResult(
    val answers: List<Int>,
    val scores: DassScore,
    val note: String?,
    val date: String,
    val createdAt: Date
)

class ScreeningRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository(auth, firestore)
) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun userId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User not logged in")

    suspend fun saveDailyScreening(
        answers: List<Int>,
        scores: DassScore,
        note: String?
    ) {
        val uid = userId()
        val today = dateFormatter.format(Date())
        val userRef = firestore.collection("users").document(uid)
        val docRef = userRef
            .collection("screenings").document(today)

        if (docRef.get().await().exists()) {
            throw IllegalStateException("Skrining hari ini sudah dilakukan")
        }

        val aiContext = buildAiContext(today, scores, note)

        val data = hashMapOf(
            "date" to today,
            "answers" to answers,
            "depressionScore" to scores.depression,
            "anxietyScore" to scores.anxiety,
            "stressScore" to scores.stress,
            "severityDepression" to scores.severityDepression,
            "severityAnxiety" to scores.severityAnxiety,
            "severityStress" to scores.severityStress,
            "note" to note,
            "aiContext" to aiContext,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        userRef.collection("medicalRecords").document(today)
            .set(
                data + mapOf(
                    "type" to "daily_screening",
                    "source" to "DASS-21"
                )
            )
            .await()
        userRef.set(
            mapOf(
                "hasScreening" to true,
                "hasScreeningToday" to true,
                "lastScreeningDate" to today,
                "lastScreeningAt" to FieldValue.serverTimestamp(),
                "latestScreeningSummary" to aiContext,
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        ).await()
        notificationRepository.addNotification(
            title = "Skrining harian tersimpan",
            body = "Hasil DASS-21 hari ini sudah masuk rekam medis dan konteks AI.",
            type = "screening"
        )
    }

    suspend fun hasTodayScreening(): Boolean {
        val uid = userId()
        val today = dateFormatter.format(Date())
        val doc = firestore.collection("users").document(uid)
            .collection("screenings").document(today)
            .get()
            .await()
        val exists = doc.exists()
        firestore.collection("users").document(uid)
            .set(
                mapOf(
                    "hasScreeningToday" to exists,
                    "screeningStatusDate" to today
                ),
                SetOptions.merge()
            )
            .await()
        return exists
    }

    suspend fun getLatestScreeningSummary(): String? {
        val uid = userId()
        val screenings = firestore.collection("users").document(uid)
            .collection("screenings")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = screenings.documents.firstOrNull() ?: return null
        val date = doc.getString("date") ?: ""
        val dep = doc.getLong("depressionScore") ?: return null
        val anx = doc.getLong("anxietyScore") ?: 0
        val str = doc.getLong("stressScore") ?: 0
        val sevDep = doc.getString("severityDepression") ?: ""
        val sevAnx = doc.getString("severityAnxiety") ?: ""
        val sevStr = doc.getString("severityStress") ?: ""
        val note = doc.getString("note") ?: ""

        return "Skrining terakhir ($date): Depresi $dep ($sevDep), Kecemasan $anx ($sevAnx), Stres $str ($sevStr). Catatan: $note"
    }

    suspend fun getScreeningHabitContext(limit: Long = 7): String? {
        val uid = userId()
        val screenings = firestore.collection("users").document(uid)
            .collection("screenings")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()

        if (screenings.isEmpty) return null

        val items = screenings.documents.mapNotNull { doc ->
            val date = doc.getString("date") ?: return@mapNotNull null
            val dep = doc.getLong("depressionScore") ?: 0
            val anx = doc.getLong("anxietyScore") ?: 0
            val stress = doc.getLong("stressScore") ?: 0
            val sevDep = doc.getString("severityDepression") ?: ""
            val sevAnx = doc.getString("severityAnxiety") ?: ""
            val sevStress = doc.getString("severityStress") ?: ""
            "$date: depresi $dep/$sevDep, kecemasan $anx/$sevAnx, stres $stress/$sevStress"
        }

        if (items.isEmpty()) return null
        return "Riwayat skrining DASS-21 terbaru untuk konteks kebiasaan user: ${items.joinToString(" | ")}"
    }

    private fun buildAiContext(date: String, scores: DassScore, note: String?): String {
        val noteText = note?.takeIf { it.isNotBlank() } ?: "tidak ada catatan tambahan"
        return "Skrining DASS-21 $date: depresi ${scores.depression} (${scores.severityDepression}), " +
            "kecemasan ${scores.anxiety} (${scores.severityAnxiety}), stres ${scores.stress} (${scores.severityStress}). " +
            "Catatan user: $noteText."
    }
}
