package com.android.capstone.sereluna.ui.article

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.model.GuardianArticle
import com.squareup.picasso.Picasso

class ArticleAdapter(
    private val articles: List<GuardianArticle>,
    private val onArticleClick: (GuardianArticle) -> Unit
) : RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.bind(article, onArticleClick)
    }

    override fun getItemCount(): Int = articles.size

    class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivArticleImage: ImageView = itemView.findViewById(R.id.ivArticleImage)
        private val tvArticleTitle: TextView = itemView.findViewById(R.id.tvArticleTitle)
        private val tvArticleDate: TextView = itemView.findViewById(R.id.tvArticleDate)
        private val tvArticleDescription: TextView = itemView.findViewById(R.id.tvArticleDescription)
        private val tvShowMore: TextView = itemView.findViewById(R.id.tvShowMore)

        fun bind(article: GuardianArticle, onArticleClick: (GuardianArticle) -> Unit) {
            tvArticleTitle.text = article.webTitle
            tvArticleDate.text = article.webPublicationDate

            // Gunakan safe call atau cek null untuk properti dalam `Fields`
            tvArticleDescription.text = article.fields?.trailText ?: "No Description Available"
            val thumbnailUrl = article.fields?.thumbnail ?: ""

            if (thumbnailUrl.isNotEmpty()) {
                Picasso.get().load(thumbnailUrl).into(ivArticleImage)
            } else {
                ivArticleImage.setImageResource(R.drawable.placeholder_image) // Gambar placeholder jika thumbnail kosong
            }

            // Atur listener untuk tombol "Show More"
            tvShowMore.setOnClickListener {
                onArticleClick(article)
            }
        }
    }
}
