package com.android.capstone.sereluna.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class ChatRequest(
    val text: String = "",
    val room_id: String? = null,
    val screening_context: String? = null,
    val session_summary: String? = null,
    val risk_level: String? = null,
    val mood_signal: String? = null,
    val mode: String? = "chat",
    val session_raw: String? = null,
    val user_name: String? = null,
    val profile_context: String? = null,
    val past_diaries: List<String>? = emptyList()
)

data class ChatResponse(
    val reply: String? = null,
    val ui_metadata: UiMetadata? = null,
    val clinical_insight: ClinicalInsight? = null,
    val session_summary: String? = null
)

data class UiMetadata(
    val sentiment_score: Int? = null,
    val suggested_action: String? = null,
    val is_risky: Boolean? = null
)

data class ClinicalInsight(
    val detected_symptoms: List<String>? = emptyList(),
    val dass_category: String? = null,
    val risk_level: String? = null
)

interface ChatbotApiService {
    @POST
    fun sendMessage(@Url url: String, @Body request: ChatRequest): Call<ChatResponse>

    companion object {
        const val CHAT_URL = "http://10.0.2.2:8000/api/v1/chat/"

        fun create(): ChatbotApiService {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ChatbotApiService::class.java)
        }
    }
}
