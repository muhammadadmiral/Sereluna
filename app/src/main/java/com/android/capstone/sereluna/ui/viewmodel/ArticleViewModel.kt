package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.android.capstone.sereluna.BuildConfig
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.data.repository.ArticleRepository

class ArticleViewModel(private val repository: ArticleRepository = ArticleRepository.getInstance()) : ViewModel() {

    private val apiKey = BuildConfig.GUARDIAN_API_KEY.ifBlank { "test" }
    private val query = "\"mental health\" OR anxiety OR depression OR stress OR therapy"

    fun getArticle(): LiveData<List<Article>> {
        return repository.getArticles(apiKey, query)
    }
}
