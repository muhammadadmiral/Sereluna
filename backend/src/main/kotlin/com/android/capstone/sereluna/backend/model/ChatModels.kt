package com.android.capstone.sereluna.backend.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class ChatRequest(
    val text: String
)

@Serializable
data class ChatResponse(
    val journal: Journal
)

@Serializable
data class Journal(
    val text: String,
    val feeling: String,
    val suggestion: String,
    @SerialName("_id") val id: String,
    val createdAt: String,
    val updatedAt: String,
    @SerialName("__v") val version: Int
) {
    companion object
}

fun Journal.Companion.fromGeneratedContent(
    text: String,
    feeling: String,
    suggestion: String
): Journal = Journal(
    text = text,
    feeling = feeling,
    suggestion = suggestion,
    id = UUID.randomUUID().toString(),
    createdAt = Instant.now().toString(),
    updatedAt = Instant.now().toString(),
    version = 1
)

@Serializable
data class ErrorResponse(
    val message: String
)

data class UserSession(
    val uid: String,
    val email: String?,
    val name: String?
)
