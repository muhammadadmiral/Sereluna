package com.android.capstone.sereluna.data.api

import com.android.capstone.sereluna.data.model.ArticleResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ArticleApiService {
    @GET("search")
    fun getArticles(
        @Query("api-key") apiKey: String,
        @Query("q") query: String,
        @Query("show-fields") fields: String = "headline,trailText,thumbnail,bodyText",
        @Query("tag") tag: String = "society/mental-health",
        @Query("page-size") pageSize: Int = 30,
        @Query("order-by") orderBy: String = "newest"
    ): Call<ArticleResponse>
}
