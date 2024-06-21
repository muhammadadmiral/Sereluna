// ChatbotApiService.kt
package com.android.capstone.sereluna.data.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(val text: String)

interface ChatbotApiService {
    @POST("journal")
    fun sendMessage(@Body request: ChatRequest): Call<ChatResponse>

    companion object {
        private const val BASE_URL = "https://capstoneserelunabackend-lm6e6pgj7q-et.a.run.app/"

        fun create(): ChatbotApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ChatbotApiService::class.java)
        }
    }
}

// Model untuk respons dari API
data class ChatResponse(val journal: Journal)

data class Journal(
    val text: String,
    val feeling: String,
    val suggestion: String,
    val _id: String,
    val createdAt: String,
    val updatedAt: String,
    val __v: Int
)
