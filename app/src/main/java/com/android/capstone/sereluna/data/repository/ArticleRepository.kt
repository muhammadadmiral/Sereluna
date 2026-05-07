package com.android.capstone.sereluna.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.capstone.sereluna.data.api.ApiConfig
import com.android.capstone.sereluna.data.api.ArticleApiService
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.data.model.ArticleResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticleRepository(private val apiService: ArticleApiService) {

    fun getArticles(apiKey: String, query: String): LiveData<List<Article>> {
        val articlesLiveData = MutableLiveData<List<Article>>()

        apiService.getArticles(apiKey, query).enqueue(object : Callback<ArticleResponse> {
            override fun onResponse(call: Call<ArticleResponse>, response: Response<ArticleResponse>) {
                if (response.isSuccessful) {
                    val articleResults = response.body()?.response?.results
                    val articles = articleResults?.mapNotNull { result ->
                        // Use safe calls and elvis operator to handle potential nulls
                        Article(
                            id = result.id ?: "",
                            title = result.webTitle ?: "No Title",
                            author = result.fields?.headline ?: "Unknown Author",
                            url = result.webUrl ?: "",
                            imageUrl = result.fields?.thumbnail ?: "",
                            content = result.fields?.bodyText ?: "No Content"
                        )
                    } ?: emptyList()
                    articlesLiveData.postValue(articles)
                } else {
                    // Handle API error
                }
            }

            override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                // Handle network failure
            }
        })
        return articlesLiveData
    }

    companion object {
        @Volatile
        private var instance: ArticleRepository? = null
        fun getInstance(): ArticleRepository =
            instance ?: synchronized(this) {
                instance ?: ArticleRepository(ApiConfig.getApiService()).also { instance = it }
            }
    }
}
