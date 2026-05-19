package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.DiaryDayGroup
import com.android.capstone.sereluna.data.model.DiaryFeedItem
import com.android.capstone.sereluna.databinding.ItemDiaryGroupBinding

class DiaryGroupAdapter :
    ListAdapter<DiaryDayGroup, DiaryGroupAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val expandedStates = mutableSetOf<String>()
    var onSeeMoreClick: ((DiaryFeedItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiaryGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDiaryGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        private val entryAdapter = DiaryEntryAdapter { item ->
            onSeeMoreClick?.invoke(item)
        }

        init {
            binding.rvDiaryEntries.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = entryAdapter
                isNestedScrollingEnabled = false
            }
        }

        fun bind(group: DiaryDayGroup) {
            binding.tvGroupDate.text = group.date
            binding.tvGroupCount.text = "${group.entries.size} sesi"
            entryAdapter.submitList(group.entries)

            val isExpanded = expandedStates.contains(group.date)
            binding.groupBody.isVisible = isExpanded
            binding.ivGroupToggle.rotation = if (isExpanded) 90f else 0f
            binding.tvGroupHint.text = if (isExpanded) "Sembunyikan sesi" else "Lihat sesi"

            binding.groupHeader.setOnClickListener { toggle(group.date) }
            binding.tvGroupHint.setOnClickListener { toggle(group.date) }
            binding.ivGroupToggle.setOnClickListener { toggle(group.date) }
        }

        private fun toggle(key: String) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            if (expandedStates.contains(key)) {
                expandedStates.remove(key)
            } else {
                expandedStates.add(key)
            }
            notifyItemChanged(position)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DiaryDayGroup>() {
            override fun areItemsTheSame(oldItem: DiaryDayGroup, newItem: DiaryDayGroup): Boolean {
                return oldItem.date == newItem.date
            }

            override fun areContentsTheSame(oldItem: DiaryDayGroup, newItem: DiaryDayGroup): Boolean {
                return oldItem == newItem
            }
        }
    }
}
