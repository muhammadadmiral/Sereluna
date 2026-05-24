package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.DiaryFeedItem
import com.android.capstone.sereluna.databinding.ItemDiaryEntryBinding

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

            val displayTitle = item.title?.takeIf { it.isNotBlank() } ?: item.summary.ifBlank { item.preview }.ifBlank { "Tidak ada ringkasan." }
            val displayContent = item.content?.takeIf { it.isNotBlank() } ?: item.preview.ifBlank { item.summary }.ifBlank { "Tidak ada preview." }

            binding.tvEntrySummary.text = displayTitle
            binding.tvEntryPreview.text = displayContent

            val isExpanded = expandedStates.contains(item.id)
            updateExpandedState(isExpanded)

            binding.tvEntryToggle.visibility =
                if ((displayTitle.length + displayContent.length) > 120) View.VISIBLE else View.GONE

            binding.tvEntryToggle.setOnClickListener {
                onSeeMoreClick(item)
            }
        }

        private fun updateExpandedState(isExpanded: Boolean) {
            if (isExpanded) {
                binding.tvEntrySummary.maxLines = Int.MAX_VALUE
                binding.tvEntryPreview.maxLines = Int.MAX_VALUE
                binding.tvEntryToggle.setText(com.android.capstone.sereluna.R.string.buka_detail)
            } else {
                binding.tvEntrySummary.maxLines = 4
                binding.tvEntryPreview.maxLines = 3
                binding.tvEntryToggle.setText(com.android.capstone.sereluna.R.string.lihat_lebih_lanjut)
            }
        }

        private fun buildTimeText(item: DiaryFeedItem): String {
            val timestamp = item.endTime?.takeIf { it.isNotBlank() }
                ?: item.updatedAt?.takeIf { it.isNotBlank() }
                ?: item.startTime?.takeIf { it.isNotBlank() }

            if (timestamp != null) {
                try {
                    val local = OffsetDateTime.parse(timestamp).atZoneSameInstant(ZoneId.systemDefault())
                    return local.format(DateTimeFormatter.ofPattern("HH:mm", Locale("id", "ID")))
                } catch (_: Exception) {
                    // Ignore and fallback
                }
            }
            return item.date
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
