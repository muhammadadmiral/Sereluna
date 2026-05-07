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
    val content: String
) : Parcelable
