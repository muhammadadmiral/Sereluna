package com.android.capstone.sereluna.ui.article

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.ActivityArticleDetailBinding
import com.squareup.picasso.Picasso

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil data dari intent
        val title = intent.getStringExtra("title") ?: "No Title"
        val date = intent.getStringExtra("date") ?: "No Date"
        val content = intent.getStringExtra("content") ?: "No Content"
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val articleUrl = intent.getStringExtra("articleUrl") ?: ""

        // Set data ke tampilan
        binding.tvDetailTitle.text = title
        binding.tvDetailDate.text = date
        binding.tvDetailContent.text = content

        // Load image jika URL tersedia
        if (imageUrl.isNotEmpty()) {
            Picasso.get().load(imageUrl).into(binding.ivDetailImage)
        } else {
            binding.ivDetailImage.setImageResource(R.drawable.placeholder_image) // Gambar placeholder
        }

        // Jika ada URL artikel, tampilkan WebView dan load URL
        if (articleUrl.isNotEmpty()) {
            binding.webView.visibility = View.VISIBLE
            binding.webView.loadUrl(articleUrl)
        } else {
            binding.webView.visibility = View.GONE
        }

        // Fungsi tombol kembali
        binding.ivBack.setOnClickListener {
            finish()
        }
    }
}
