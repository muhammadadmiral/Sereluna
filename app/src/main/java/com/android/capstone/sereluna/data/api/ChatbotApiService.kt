package com.android.capstone.sereluna.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Request body for the Sereluna backend.
 * The variable name "text" must match the key expected by the Google Apps Script.
 */
data class ChatRequest(
    val text: String,
    val room_id: String? = null,
    val screening_context: String? = null,
    val session_summary: String? = null,
    val risk_level: String? = null,
    val mood_signal: String? = null,
    val mode: String? = null,
    val session_raw: String? = null,
    val user_name: String? = null,
    val profile_context: String? = null,
    val groq_api_key: String? = null
)

/**
 * Response body from the Sereluna backend.
 * The variable names must match the keys sent from the Google Apps Script.
 */
data class ChatResponse(
    val reply: String?,
    val error: String?,
    val details: String?,
    val session_summary: String? = null
)

interface ChatbotApiService {
    /**
     * Send message to the backend (FastAPI or Google Apps Script).
     * Uses @Url to allow switching between environments (Dev/Prod).
     */
    @POST
    fun sendMessage(@Url url: String, @Body request: ChatRequest): Call<ChatResponse>

    companion object {
        // Current Google Apps Script URL (Legacy)
        const val GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzdTdXAz0jFNtMpYXpUOMd7leNFBLQpcAcvWJKHUflX1UAmZLadegnSZnsL9TuqADnn/exec"
        
        // Placeholder for FastAPI URL (Update this when hosted)
        // const val FASTAPI_URL = "https://your-fastapi-instance.com/chat"
        // const val LOCAL_DEV_URL = "http://10.0.2.2:8000/chat" // Use this for Android Emulator

        fun create(): ChatbotApiService {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://sereluna.ai/") // Generic base URL
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ChatbotApiService::class.java)
        }
    }
}
