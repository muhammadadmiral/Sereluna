package com.android.capstone.sereluna.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Article(
    val id: String,
    val title: String,
    val author: String,
    val url: String,
    val imageUrl: String,
    val content: String,
    val section: String = "",
    val publishedAt: String = "",
    val summary: String = "",
    val topic: String = "",
    val topicLabel: String = "",
    val source: String = "The Guardian",
    val contentWarning: String = "",
    val whyRecommended: String = "",
    val tags: List<String> = emptyList()
) : Parcelable
