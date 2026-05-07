package com.android.capstone.sereluna.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository(auth, firestore)
) {

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getUserData(userId: String): Map<String, Any>? {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            document.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProfileContext(userId: String): String? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val name = doc.getString("name") ?: ""
            val email = doc.getString("email") ?: ""
            val provider = doc.getString("provider") ?: ""
            val joined = doc.getDate("createdAt")?.toString() ?: ""
            val note = doc.getString("note") ?: ""
            val latestScreening = doc.getString("latestScreeningSummary") ?: ""
            val personalContext = doc.getString("personalContext") ?: ""
            val pieces = listOf(
                if (name.isNotEmpty()) "Nama: $name" else "",
                if (email.isNotEmpty()) "Email: $email" else "",
                if (provider.isNotEmpty()) "Provider: $provider" else "",
                if (joined.isNotEmpty()) "Bergabung: $joined" else "",
                if (note.isNotEmpty()) "Catatan: $note" else "",
                if (latestScreening.isNotEmpty()) "Skrining terbaru: $latestScreening" else "",
                if (personalContext.isNotEmpty()) "Konteks personal dari diary: $personalContext" else ""
            ).filter { it.isNotEmpty() }
            if (pieces.isEmpty()) null else pieces.joinToString("; ")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateProfile(userId: String, name: String, newImageUri: Uri?): Result<Unit> {
        return try {
            val photoUrl = if (newImageUri != null) {
                uploadProfileImage(userId, newImageUri)
            } else {
                // Get the existing photoUrl if not changing the image
                val existingData = getUserData(userId)
                existingData?.get("photoUrl") as? String
            }

            val userData = hashMapOf(
                "name" to name,
                "photoUrl" to (photoUrl ?: "")
            )

            firestore.collection("users").document(userId).update(userData as Map<String, Any>).await()
            notificationRepository.addNotification(
                title = "Profil diperbarui",
                body = "Data profilmu sudah tersimpan.",
                type = "profile"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        val storageRef = storage.reference.child("profile_images/$userId")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }
}
