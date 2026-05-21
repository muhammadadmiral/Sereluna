package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.api.ArticleTopicDto
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

class ArticleViewModel(
    private val repository: SerelunaRepository = SerelunaRepository()
) : ViewModel() {

    private val _topics = MutableLiveData<UiState<List<ArticleTopicDto>>>()
    val topics: LiveData<UiState<List<ArticleTopicDto>>> = _topics

    private val _articles = MutableLiveData<UiState<List<Article>>>()
    val articles: LiveData<UiState<List<Article>>> = _articles

    private val _meta = MutableLiveData<ArticleMeta>()
    val meta: LiveData<ArticleMeta> = _meta

    private val cache = mutableMapOf<String, CachedArticles>()
    private var defaultTopic: String = "wellbeing"

    fun loadInitial() {
        if (_articles.value is UiState.Success) return
        loadTopics()
    }

    fun loadTopics() {
        viewModelScope.launch {
            _topics.value = UiState.Loading
            try {
                val response = repository.getArticleTopics()
                defaultTopic = response.default_topic.ifBlank { "wellbeing" }
                _topics.value = UiState.Success(response.topics)
                loadRecommendations(topic = defaultTopic)
            } catch (e: Exception) {
                _topics.value = UiState.Error(e.message ?: "Gagal memuat topik artikel.")
                loadRecommendations(topic = defaultTopic)
            }
        }
    }

    fun loadRecommendations(topic: String? = null, mood: String? = null, query: String? = null) {
        val cacheKey = listOf(topic.orEmpty(), mood.orEmpty(), query.orEmpty()).joinToString("|")
        cache[cacheKey]?.takeIf { !it.isExpired() }?.let {
            _meta.value = it.meta
            _articles.value = UiState.Success(it.articles)
            return
        }

        viewModelScope.launch {
            _articles.value = UiState.Loading
            try {
                val response = repository.getArticleRecommendations(
                    topic = topic,
                    mood = mood,
                    query = query?.takeIf { it.isNotBlank() },
                    limit = 8
                )
                val articles = response.articles.map {
                    Article(
                        id = it.id,
                        title = it.title,
                        author = it.source,
                        url = it.url,
                        imageUrl = it.thumbnail.orEmpty(),
                        content = it.summary.orEmpty(),
                        section = it.section.orEmpty(),
                        publishedAt = it.published_at.orEmpty(),
                        summary = it.summary.orEmpty(),
                        topic = it.topic.orEmpty(),
                        topicLabel = it.topic_label.orEmpty(),
                        source = it.source,
                        contentWarning = it.content_warning.orEmpty(),
                        whyRecommended = it.why_recommended.orEmpty(),
                        tags = it.tags
                    )
                }
                val meta = ArticleMeta(
                    source = response.source,
                    topicLabel = response.topic_label.orEmpty(),
                    topicSummary = response.topic_summary.orEmpty(),
                    disclaimer = response.disclaimer,
                    query = response.query
                )
                cache[cacheKey] = CachedArticles(articles, meta)
                _meta.value = meta
                _articles.value = UiState.Success(articles)
            } catch (e: Exception) {
                _articles.value = UiState.Error(e.message ?: "Gagal memuat rekomendasi artikel.")
            }
        }
    }

    data class ArticleMeta(
        val source: String = "The Guardian",
        val topicLabel: String = "",
        val topicSummary: String = "",
        val disclaimer: String = "",
        val query: String = ""
    )

    private data class CachedArticles(
        val articles: List<Article>,
        val meta: ArticleMeta,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - createdAt > CACHE_TTL_MS
    }

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
