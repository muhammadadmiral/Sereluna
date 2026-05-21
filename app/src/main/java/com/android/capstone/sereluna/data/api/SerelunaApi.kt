package com.android.capstone.sereluna.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import com.google.gson.JsonElement

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

    @GET("api/v1/screening/dass21/")
    suspend fun getDass21Questionnaire(): Dass21QuestionnaireDto

    @GET("api/v1/screening/status/")
    suspend fun getScreeningStatus(
        @Header("Authorization") authorization: String
    ): ScreeningStatusDto

    @GET("api/v1/me/context/")
    suspend fun getContext(
        @Header("Authorization") authorization: String
    ): UserContextResponseDto

    @GET("api/v1/me/profile/")
    suspend fun getProfile(
        @Header("Authorization") authorization: String
    ): UserProfileResponseDto

    @PUT("api/v1/me/profile/")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: UserProfileUpdateRequestDto
    ): UserProfileResponseDto

    @GET("api/v1/diaries/")
    suspend fun getDiaries(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 30
    ): DiaryListResponseDto

    @GET("api/v1/diaries/entries/")
    suspend fun getDiaryEntries(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 30
    ): DiaryEntryListResponseDto

    @GET("api/v1/diaries/{diary_id}/")
    suspend fun getDiaryDetail(
        @Header("Authorization") authorization: String,
        @Path("diary_id") diaryId: String
    ): DiaryDetailDto

    @GET("api/v1/diaries/{diary_id}/sessions/{session_id}/messages/")
    suspend fun getDiaryMessages(
        @Header("Authorization") authorization: String,
        @Path("diary_id") diaryId: String,
        @Path("session_id") sessionId: String
    ): DiaryMessagesResponseDto

    @GET("api/v1/notifications/")
    suspend fun getNotifications(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 30
    ): NotificationsResponseDto

    @GET("api/v1/notifications/unread-count/")
    suspend fun getNotificationUnreadCount(
        @Header("Authorization") authorization: String
    ): NotificationUnreadCountDto

    @PATCH("api/v1/notifications/{notification_id}/read/")
    suspend fun markNotificationRead(
        @Header("Authorization") authorization: String,
        @Path("notification_id") notificationId: String
    ): SuccessResponseDto

    @POST("api/v1/device-token/")
    suspend fun submitDeviceToken(
        @Header("Authorization") authorization: String,
        @Body request: DeviceTokenRequestDto
    ): SuccessResponseDto

    @POST("api/v1/sleep/daily/")
    suspend fun submitSleepDaily(
        @Header("Authorization") authorization: String,
        @Body request: SleepDailyRequestDto
    ): SleepDailyResponseDto

    @GET("api/v1/sleep/daily/")
    suspend fun getSleepDaily(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 14
    ): SleepDailyHistoryResponseDto

    @POST("api/v1/mood/")
    suspend fun submitMood(
        @Header("Authorization") authorization: String,
        @Body request: MoodRequestDto
    ): SuccessResponseDto

    @GET("api/v1/calendar/summary/")
    suspend fun getCalendarSummary(
        @Header("Authorization") authorization: String,
        @Query("year") year: Int,
        @Query("month") month: Int
    ): JsonElement

    @GET("api/v1/calendar/detail/")
    suspend fun getCalendarDetail(
        @Header("Authorization") authorization: String,
        @Query("date") date: String
    ): JsonElement

    @POST("api/v1/auth/forgot-password/")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequestDto
    ): ForgotPasswordResponseDto

    @POST("api/v1/auth/change-password/")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: ChangePasswordRequestDto
    ): MessageResponseDto

    @PATCH("api/v1/notifications/read-all/")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") authorization: String
    ): SuccessResponseDto

    @DELETE("api/v1/me/account/")
    suspend fun deleteAccount(
        @Header("Authorization") authorization: String
    ): MessageResponseDto

    @GET("api/v1/stats/mood-distribution/")
    suspend fun getMoodDistribution(
        @Header("Authorization") authorization: String,
        @Query("days") days: Int
    ): MoodDistributionResponseDto

    @GET("api/v1/stats/sleep-trends/")
    suspend fun getSleepTrends(
        @Header("Authorization") authorization: String,
        @Query("days") days: Int
    ): SleepTrendsResponseDto

    @GET("api/v1/statistics/wellbeing/")
    suspend fun getWellbeingStatistics(
        @Header("Authorization") authorization: String,
        @Query("range") range: String
    ): WellbeingStatisticsResponseDto

    @GET("api/v1/articles/topics/")
    suspend fun getArticleTopics(
        @Header("Authorization") authorization: String
    ): ArticleTopicsResponseDto

    @GET("api/v1/articles/recommendations/")
    suspend fun getArticleRecommendations(
        @Header("Authorization") authorization: String,
        @Query("topic") topic: String? = null,
        @Query("mood") mood: String? = null,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 8,
        @Query("section") section: String? = null
    ): ArticleRecommendationsResponseDto

    @POST("api/v1/articles/recommendations/notify/")
    suspend fun notifyArticleRecommendation(
        @Header("Authorization") authorization: String,
        @Body request: ArticleNotifyRequestDto
    ): ArticleNotifyResponseDto
}
