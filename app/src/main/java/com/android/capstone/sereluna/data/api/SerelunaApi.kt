package com.android.capstone.sereluna.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SerelunaApi {
    @POST("api/v1/chat/")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequestDto
    ): ChatResponseDto

    @POST("api/v1/chat/finish/")
    suspend fun finishChat(
        @Header("Authorization") authorization: String,
        @Body request: ChatFinishRequestDto
    ): ChatResponseDto

    @POST("api/v1/screening/")
    suspend fun submitScreening(
        @Header("Authorization") authorization: String,
        @Body request: ScreeningRequestDto
    ): ScreeningResponseDto

    @GET("api/v1/me/context/")
    suspend fun getContext(
        @Header("Authorization") authorization: String
    ): UserContextResponseDto
}
