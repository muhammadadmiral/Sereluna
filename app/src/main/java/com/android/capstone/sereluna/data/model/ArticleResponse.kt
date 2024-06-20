package com.android.capstone.sereluna.data.model

data class ArticleResponse(
    val status: String,
    val totalResults: Int,
    val response: GuardianResponse // Perubahan: Tambahkan properti 'response' untuk The Guardian API
)

data class GuardianResponse(
    val status: String,
    val total: Int,
    val startIndex: Int,
    val pageSize: Int,
    val currentPage: Int,
    val pages: Int,
    val results: List<GuardianArticle>
)
