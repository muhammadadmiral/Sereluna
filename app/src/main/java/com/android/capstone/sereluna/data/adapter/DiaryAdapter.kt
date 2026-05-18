package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ItemDiaryListBinding

class DiaryAdapter : ListAdapter<Diary, DiaryAdapter.ViewHolder>(DIFF_CALLBACK) {

    // Track expanded state by Diary ID to survive view recycling
    private val expandedStates = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiaryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val diary = getItem(position)
        holder.bind(diary)
    }

    inner class ViewHolder(private val binding: ItemDiaryListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(diary: Diary) {
            binding.tvDiaryDate.text = diary.date
            binding.tvDiaryDescription.text = diary.chatSummary.ifBlank { diary.content }
            binding.tvBotResponse.text = diary.chatSummary

            val isExpanded = expandedStates.contains(diary.id)
            updateExpandedState(isExpanded)

            // Calculate if text needs truncation (roughly by checking length or lines, but here we just show the button always or depending on length)
            val combinedLength = binding.tvDiaryDescription.text.length + binding.tvBotResponse.text.length
            if (combinedLength > 100) {
                binding.tvReadMore.visibility = View.VISIBLE
            } else {
                binding.tvReadMore.visibility = View.GONE
            }

            binding.tvReadMore.setOnClickListener {
                val currentlyExpanded = expandedStates.contains(diary.id)
                if (currentlyExpanded) {
                    expandedStates.remove(diary.id)
                } else {
                    expandedStates.add(diary.id)
                }
                updateExpandedState(!currentlyExpanded)
            }
        }

        private fun updateExpandedState(isExpanded: Boolean) {
            if (isExpanded) {
                binding.tvDiaryDescription.maxLines = Int.MAX_VALUE
                binding.tvBotResponse.maxLines = Int.MAX_VALUE
                binding.tvReadMore.text = "Tutup"
            } else {
                binding.tvDiaryDescription.maxLines = 3
                binding.tvBotResponse.maxLines = 3
                binding.tvReadMore.text = "Lihat Lebih Lanjut"
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Diary>() {
            override fun areItemsTheSame(oldItem: Diary, newItem: Diary): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Diary, newItem: Diary): Boolean {
                return oldItem == newItem
            }
        }
    }
}
