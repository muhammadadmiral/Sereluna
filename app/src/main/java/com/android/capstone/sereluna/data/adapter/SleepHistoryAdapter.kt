package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.databinding.ItemSleepHistoryBinding
import com.android.capstone.sereluna.ui.sleeptracking.SleepTrackingFragment
import java.text.SimpleDateFormat
import java.util.*

class SleepHistoryAdapter(private val sleepHistory: List<SleepTrackingFragment.SleepData>) :
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

        fun bind(sleepData: SleepTrackingFragment.SleepData) {
            binding.sleepDateTextView.text = sleepData.bedtime?.let { dateFormat.format(it) } ?: sleepData.date
            binding.bedtimeTextView.text = "Bedtime: ${sleepData.bedtime?.let { timeFormat.format(it) } ?: "--"}"
            binding.wakeupTextView.text = "Wakeup: ${sleepData.wakeup?.let { timeFormat.format(it) } ?: "--"}"
            binding.sleepDurationTextView.text = "Duration: ${formatHours(sleepData.sleepDuration)}"
            binding.sleepQualityTextView.text = sleepData.sleepQuality
        }

        private fun formatHours(value: Double): String {
            val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
            return "$formatted hours"
        }
    }
}
