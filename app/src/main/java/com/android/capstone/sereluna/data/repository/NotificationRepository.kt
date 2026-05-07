package com.android.capstone.sereluna.data.repository

import com.android.capstone.sereluna.data.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun userId(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("User not logged in")

    fun observeNotifications(
        onChanged: (List<Notification>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val uid = userId()
        return firestore.collection("users").document(uid)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().map { doc ->
                    Notification(
                        id = doc.id,
                        title = doc.getString("title").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        notifStatus = doc.getString("type").orEmpty(),
                        createdAt = (doc.get("createdAt") as? Timestamp)?.toDate()
                    )
                }
                onChanged(items)
            }
    }

    suspend fun addNotification(title: String, body: String, type: String) {
        val uid = userId()
        val data = mapOf(
            "title" to title,
            "body" to body,
            "type" to type,
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("users").document(uid)
            .collection("notifications")
            .add(data)
            .await()
    }
}
