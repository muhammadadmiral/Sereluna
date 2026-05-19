package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.DiaryFeedItem
import com.android.capstone.sereluna.databinding.ItemDiaryEntryBinding

class DiaryEntryAdapter(
    private val onSeeMoreClick: (DiaryFeedItem) -> Unit
) : ListAdapter<DiaryFeedItem, DiaryEntryAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val expandedStates = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiaryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDiaryEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DiaryFeedItem) {
            binding.tvEntryStatus.text = item.status.ifBlank { "session" }
            binding.tvEntryModel.text = item.model.ifBlank { "model" }
            binding.tvEntryTime.text = buildTimeText(item)
            binding.tvEntrySummary.text = item.summary.ifBlank { item.preview }.ifBlank { "Tidak ada ringkasan." }
            binding.tvEntryPreview.text = item.preview.ifBlank { item.summary }.ifBlank { "Tidak ada preview." }

            val isExpanded = expandedStates.contains(item.id)
            updateExpandedState(isExpanded)

            binding.tvEntryToggle.visibility =
                if ((item.summary.length + item.preview.length) > 120) View.VISIBLE else View.GONE

            binding.tvEntryToggle.setOnClickListener {
                onSeeMoreClick(item)
            }
        }

        private fun updateExpandedState(isExpanded: Boolean) {
            if (isExpanded) {
                binding.tvEntrySummary.maxLines = Int.MAX_VALUE
                binding.tvEntryPreview.maxLines = Int.MAX_VALUE
                binding.tvEntryToggle.text = "Buka detail"
            } else {
                binding.tvEntrySummary.maxLines = 4
                binding.tvEntryPreview.maxLines = 3
                binding.tvEntryToggle.text = "Lihat lebih lanjut"
            }
        }

        private fun buildTimeText(item: DiaryFeedItem): String {
            val parts = listOfNotNull(item.startTime, item.endTime).filter { it.isNotBlank() }
            return when {
                parts.isEmpty() -> item.date
                parts.size == 1 -> parts.first()
                else -> "${parts.first()} - ${parts.last()}"
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DiaryFeedItem>() {
            override fun areItemsTheSame(oldItem: DiaryFeedItem, newItem: DiaryFeedItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: DiaryFeedItem, newItem: DiaryFeedItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}
