package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ItemDiaryListBinding

class DiaryAdapter : ListAdapter<Diary, DiaryAdapter.ViewHolder>(DIFF_CALLBACK) {

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
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Diary>() {
            override fun areItemsTheSame(oldItem: Diary, newItem: Diary): Boolean {
                return oldItem.id == newItem.id // Assuming Diary has an 'id' field
            }

            override fun areContentsTheSame(oldItem: Diary, newItem: Diary): Boolean {
                return oldItem == newItem
            }
        }
    }
}
