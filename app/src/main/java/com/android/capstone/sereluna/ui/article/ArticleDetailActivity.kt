package com.android.capstone.sereluna.ui.article

import android.os.Build
import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.databinding.ActivityArticleDetailBinding
import com.squareup.picasso.Picasso

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val article = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_ARTICLE, Article::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ARTICLE)
        }

        if (article != null) {
            setupUI(article)
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupUI(article: Article) {
        binding.tvDetailTitle.text = article.title
        // The problematic line referencing tvDetailAuthor has been completely removed.
        
        if (article.imageUrl.isNotEmpty()) {
            Picasso.get().load(article.imageUrl).into(binding.ivDetailImage)
        }

        // Load content in WebView for better formatting
        binding.webView.webViewClient = WebViewClient()
        binding.webView.loadDataWithBaseURL(null, article.content, "text/html", "UTF-8", null)
    }

    companion object {
        const val EXTRA_ARTICLE = "extra_article"
    }
}
