package com.android.capstone.sereluna.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
        val docRef = firestore.collection("users").document(uid)
            .collection("screenings").document(today)

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
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
    }

    suspend fun hasTodayScreening(): Boolean {
        val uid = userId()
        val today = dateFormatter.format(Date())
        val doc = firestore.collection("users").document(uid)
            .collection("screenings").document(today)
            .get()
            .await()
        return doc.exists()
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
}
