package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.databinding.ItemArticleBinding
import com.squareup.picasso.Picasso

class ArticleAdapter(private val onClick: (Article) -> Unit) : ListAdapter<Article, ArticleAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = getItem(position)
        holder.bind(article)
    }

    inner class ViewHolder(private val binding: ItemArticleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) {
            binding.tvArticleTitle.text = article.title
            binding.tvArticleAuthor.text = article.summary.ifBlank { article.content.ifBlank { article.contentWarning } }
            binding.tvArticleTopic.text = article.topicLabel.ifBlank { article.section.ifBlank { "Artikel" } }
            binding.tvArticleSource.text = listOf(article.source, article.publishedAt.take(10))
                .filter { it.isNotBlank() }
                .joinToString(" | ")
            binding.tvWhyRecommended.text = article.whyRecommended.ifBlank {
                article.contentWarning.ifBlank { "Bacaan pendukung, bukan pengganti bantuan profesional." }
            }
            if (article.imageUrl.isNotEmpty()) {
                Picasso.get().load(article.imageUrl).fit().centerCrop().into(binding.ivArticleImage)
            } else {
                binding.ivArticleImage.setImageResource(com.android.capstone.sereluna.R.drawable.article)
            }
            itemView.setOnClickListener {
                onClick(article)
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Article>() {
            override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
                return oldItem == newItem
            }
        }
    }
}
