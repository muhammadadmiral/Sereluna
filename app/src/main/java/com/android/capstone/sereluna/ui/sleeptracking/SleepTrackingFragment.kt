package com.android.capstone.sereluna.ui.sleeptracking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.databinding.FragmentSleepTrackingBinding
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SleepTrackingFragment : Fragment() {

    private var _binding: FragmentSleepTrackingBinding? = null
    private val binding get() = _binding!!
    private val repository = SerelunaRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveSleepDataButton.setOnClickListener {
            saveSleepData()
        }

        loadSleepHistory()
    }

    private fun saveSleepData() {
        val bedtimeHour = binding.bedtimePicker.hour
        val bedtimeMinute = binding.bedtimePicker.minute
        val wakeupHour = binding.wakeupPicker.hour
        val wakeupMinute = binding.wakeupPicker.minute

        val bedtime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, bedtimeMinute)
        }

        val wakeup = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeupHour)
            set(Calendar.MINUTE, wakeupMinute)
        }

        if (wakeup.before(bedtime)) {
            wakeup.add(Calendar.DATE, 1) // Menangani jika waktu bangun setelah tengah malam
        }

        val sleepDuration = (wakeup.timeInMillis - bedtime.timeInMillis) / (1000 * 60 * 60) // dalam jam

        val sleepQuality = when {
            sleepDuration < 6 -> "Poor"
            sleepDuration in 6..8 -> "Good"
            else -> "Excellent"
        }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(bedtime.time)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.submitSleepDaily(
                    date = date,
                    sleepQuality = sleepQuality,
                    totalSleepHours = sleepDuration
                )
                Toast.makeText(requireContext(), "Sleep data saved successfully", Toast.LENGTH_SHORT).show()
                binding.sleepQualityResult.text = "Sleep Quality: $sleepQuality"
                loadSleepHistory()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save sleep data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSleepHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sleepHistory = repository.getSleepDaily().map { item ->
                    SleepData(
                        date = item.date,
                        sleepDuration = item.total_sleep_hours,
                        sleepQuality = item.sleep_quality
                    )
                }
                binding.sleepHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.sleepHistoryRecyclerView.adapter = SleepHistoryAdapter(sleepHistory)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load sleep history: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class SleepData(
    val date: String = "",
    val bedtime: Date? = null,
    val wakeup: Date? = null,
    val sleepDuration: Long = 0,
    val sleepQuality: String = ""
)
