package com.android.capstone.sereluna.ui.article

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityArticleDetailBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding
    private val repository = SerelunaRepository()
    private var article: Article? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        article = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_ARTICLE, Article::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ARTICLE)
        }

        article?.let { setupUI(it) }

        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.btnOpenArticle.setOnClickListener { openArticleUrl() }
        binding.btnNotifyArticle.setOnClickListener { createArticleNotification() }
    }

    private fun setupUI(article: Article) {
        binding.tvDetailTitle.text = article.title
        binding.tvDetailDate.text = listOf(article.source, article.section, article.publishedAt.take(10))
            .filter { it.isNotBlank() }
            .joinToString(" | ")
        binding.tvDetailTopic.text = article.topicLabel.ifBlank { article.section.ifBlank { "Artikel" } }
        
        if (article.imageUrl.isNotEmpty()) {
            Picasso.get().load(article.imageUrl).into(binding.ivDetailImage)
        } else {
            binding.ivDetailImage.setImageResource(com.android.capstone.sereluna.R.drawable.article)
        }

        binding.tvDetailContent.text = article.summary.ifBlank {
            article.content.ifBlank { article.whyRecommended.ifBlank { "Ringkasan artikel belum tersedia." } }
        }
        binding.tvContentWarning.text = article.contentWarning.ifBlank {
            "Artikel eksternal. Gunakan sebagai bacaan pendukung, bukan diagnosis atau pengganti bantuan profesional."
        }
    }

    private fun openArticleUrl() {
        val url = article?.url.orEmpty()
        if (url.isBlank()) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun createArticleNotification() {
        val item = article ?: return
        binding.btnNotifyArticle.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = repository.notifyArticleRecommendation(
                    articleId = item.id,
                    title = item.title,
                    url = item.url,
                    summary = item.summary.ifBlank { item.content }
                )
                Toast.makeText(
                    this@ArticleDetailActivity,
                    if (response.success) "Notifikasi artikel dibuat." else "Artikel sudah diproses.",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@ArticleDetailActivity, "Gagal membuat notifikasi artikel.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnNotifyArticle.isEnabled = true
            }
        }
    }

    companion object {
        const val EXTRA_ARTICLE = "extra_article"
    }
}
