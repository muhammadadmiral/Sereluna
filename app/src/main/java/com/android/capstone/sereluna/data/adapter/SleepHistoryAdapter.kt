package com.android.capstone.sereluna.ui.sleeptracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.databinding.ItemSleepHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class SleepHistoryAdapter(private val sleepHistory: List<SleepData>) :
    RecyclerView.Adapter<SleepHistoryAdapter.SleepHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SleepHistoryViewHolder {
        val binding = ItemSleepHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SleepHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SleepHistoryViewHolder, position: Int) {
        val sleepData = sleepHistory[position]
        holder.bind(sleepData)
    }

    override fun getItemCount(): Int = sleepHistory.size

    class SleepHistoryViewHolder(private val binding: ItemSleepHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(sleepData: SleepData) {
            binding.sleepDateTextView.text = sleepData.bedtime?.let { dateFormat.format(it) }
            binding.bedtimeTextView.text = sleepData.bedtime?.let { timeFormat.format(it) }
            binding.wakeupTextView.text = sleepData.wakeup?.let { timeFormat.format(it) }
            binding.sleepDurationTextView.text = "${sleepData.sleepDuration} hours"
            binding.sleepQualityTextView.text = sleepData.sleepQuality
        }
    }
}
