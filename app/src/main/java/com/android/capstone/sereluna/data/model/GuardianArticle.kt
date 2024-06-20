package com.android.capstone.sereluna.data.model

data class GuardianArticle(
    val id: String,
    val type: String,
    val sectionId: String,
    val sectionName: String,
    val webPublicationDate: String,
    val webTitle: String,
    val webUrl: String,
    val apiUrl: String,
    val fields: Fields?
)

data class Fields(
    val headline: String?,
    val trailText: String?,
    val bodyText: String?,
    val thumbnail: String?
)
