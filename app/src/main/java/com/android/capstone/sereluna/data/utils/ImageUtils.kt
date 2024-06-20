package com.android.capstone.sereluna.utils

import android.widget.ImageView
import coil.load
import com.android.capstone.sereluna.R

fun ImageView.loadImage(url: String?) {
    this.load(url) {
        crossfade(true)
        placeholder(R.drawable.placeholder_image) // Ganti dengan placeholder Anda
        error(R.drawable.error_image) // Ganti dengan gambar error Anda
    }
}