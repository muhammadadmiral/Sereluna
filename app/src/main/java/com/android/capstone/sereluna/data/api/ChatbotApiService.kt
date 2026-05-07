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
    val profile_context: String? = null
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
     * We use @Url for dynamic URLs which is required for Google Apps Script.
     * The base URL will be ignored, and the full URL provided here will be used.
     */
    @POST
    fun sendMessage(@Url url: String, @Body request: ChatRequest): Call<ChatResponse>

    companion object {
        const val FULL_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwoVi39-hqUwSNjNMoBWLhV8bq9od3KgP8wGU2dtyBNL0al-DdCGRanidsNxWE7reQS/exec"

        fun create(): ChatbotApiService {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://script.google.com/") // Generic base URL
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ChatbotApiService::class.java)
        }
    }
}
