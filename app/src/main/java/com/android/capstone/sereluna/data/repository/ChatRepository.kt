package com.android.capstone.sereluna.data.repository

import android.util.Log
import com.android.capstone.sereluna.data.model.ChatMessage
import com.android.capstone.sereluna.data.model.ChatRoom
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    companion object {
        private const val TAG = "ChatRepository"
        private const val USERS_COLLECTION = "users"
        private const val ROOMS_COLLECTION = "rooms"
        private const val MESSAGES_COLLECTION = "messages"
    }

    suspend fun createOrGetRoom(userId: String, roomId: String): Result<ChatRoom> {
        return try {
            val roomRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ROOMS_COLLECTION)
                .document(roomId)

            val snapshot = roomRef.get().await()
            if (!snapshot.exists()) {
                val newRoom = ChatRoom(roomId = roomId)
                roomRef.set(newRoom).await()
                Result.success(newRoom)
            } else {
                val existingRoom = snapshot.toObject(ChatRoom::class.java)
                if (existingRoom != null) {
                    Result.success(existingRoom)
                } else {
                    Result.failure(Exception("Failed to parse existing room"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createOrGetRoom", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessageToFirestore(userId: String, roomId: String, message: ChatMessage): Result<Unit> {
        return try {
            val roomRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ROOMS_COLLECTION)
                .document(roomId)

            val messagesRef = roomRef.collection(MESSAGES_COLLECTION)

            firestore.runBatch { batch ->
                // Generate a new ID if messageId is empty, else use the provided one
                val docRef = if (message.messageId.isEmpty()) {
                    messagesRef.document()
                } else {
                    messagesRef.document(message.messageId)
                }
                
                // Add message to sub-collection
                batch.set(docRef, message)

                // Update room's last message and timestamp
                batch.update(roomRef, "lastMessage", message.text)
                batch.update(roomRef, "timestamp", message.timestamp)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in sendMessageToFirestore", e)
            Result.failure(e)
        }
    }

    fun getMessages(userId: String, roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(ROOMS_COLLECTION)
            .document(roomId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listenerRegistration = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching messages", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)
                }
                trySend(messages)
            }
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}
