package com.android.capstone.sereluna.data.model

data class Diary(
    var id: String = "", // Tidak menggunakan anotasi @DocumentId
    var date: String = "",
    var content: String = ""
)
