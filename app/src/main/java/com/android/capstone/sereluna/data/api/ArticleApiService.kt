package com.android.capstone.sereluna.data.api

import com.android.capstone.sereluna.data.model.ArticleResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ArticleApiService {
    @GET("search")
    fun getArticles(
        @Query("api-key") apiKey: String, // API Key dari The Guardian
        @Query("q") query: String,        // Query untuk pencarian artikel
        @Query("show-fields") fields: String = "headline,trailText,thumbnail,bodyText" // Menunjukkan fields yang akan diambil
    ): Call<ArticleResponse>
}
