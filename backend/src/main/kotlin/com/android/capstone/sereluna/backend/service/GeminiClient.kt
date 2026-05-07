package com.android.capstone.sereluna.backend.service

import com.android.capstone.sereluna.backend.model.Journal
import com.android.capstone.sereluna.backend.model.UserSession
import com.android.capstone.sereluna.backend.model.fromGeneratedContent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

class GeminiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String,
    private val json: Json
) {
    private val logger = LoggerFactory.getLogger(GeminiClient::class.java)

    suspend fun generateJournalEntry(userText: String, userSession: UserSession): Journal {
        val prompt = buildPrompt(userText, userSession)
        val response = runCatching {
            httpClient.post("$BASE_URL/$model:generateContent") {
                header("X-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(
                    GeminiRequest(
                        contents = listOf(
                            GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                        ),
                        generationConfig = GenerationConfig(
                            temperature = 0.4,
                            topK = 32,
                            topP = 0.95,
                            maxOutputTokens = 512,
                            responseMimeType = "application/json"
                        )
                    )
                )
            }.body<GeminiResponse>()
        }.getOrElse { throwable ->
            throw GeminiClientException("Gemini API call failed", throwable)
        }

        val candidateText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?.takeIf { !it.isNullOrBlank() }

        val parsedPayload = candidateText?.let { payload ->
            runCatching {
                json.decodeFromString(GeminiJournalPayload.serializer(), payload)
            }.onFailure {
                logger.warn("Unable to parse Gemini JSON payload, falling back to raw text", it)
            }.getOrNull()
        }

        val finalText = parsedPayload?.text ?: candidateText ?: defaultFallbackText(userSession)
        val finalFeeling = parsedPayload?.feeling ?: "reflective"
        val finalSuggestion = parsedPayload?.suggestion ?: "Take a calming breath and acknowledge each emotion that surfaced."

        return Journal.fromGeneratedContent(
            text = finalText,
            feeling = finalFeeling,
            suggestion = finalSuggestion
        )
    }

    private fun buildPrompt(userText: String, userSession: UserSession): String {
        return """
            You are Sereluna, a compassionate mental wellness journaling coach.
            Analyze the provided diary entry and produce a short JSON response with the keys "text", "feeling", and "suggestion".
            - "text": empathetic summary (<=120 words) that reflects back the user's story.
            - "feeling": single lowercase mood label (e.g., "grateful", "overwhelmed", "hopeful").
            - "suggestion": single actionable tip (<=40 words) encouraging healthy coping.
            Respond ONLY with valid JSON and no additional commentary.

            User id: ${userSession.uid}
            Diary entry:
            $userText
        """.trimIndent()
    }

    private fun defaultFallbackText(userSession: UserSession): String =
        "I heard you, and your reflection has been saved. Keep journaling so we can continue learning how to support you, ${userSession.name ?: "friend"}."
}

class GeminiClientException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

@Serializable
private data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig")
    val generationConfig: GenerationConfig? = null
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(
    val text: String
)

@Serializable
private data class GenerationConfig(
    val temperature: Double? = null,
    val topK: Int? = null,
    val topP: Double? = null,
    val maxOutputTokens: Int? = null,
    @SerialName("response_mime_type")
    val responseMimeType: String? = null
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null
)

@Serializable
private data class GeminiJournalPayload(
    val text: String,
    val feeling: String,
    val suggestion: String
)
