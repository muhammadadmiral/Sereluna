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
import java.util.TimeZone
import kotlin.math.roundToInt

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
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val wakeup = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeupHour)
            set(Calendar.MINUTE, wakeupMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!wakeup.after(bedtime)) {
            wakeup.add(Calendar.DATE, 1) // Menangani jika waktu bangun setelah tengah malam
        }

        val sleepDuration = (((wakeup.timeInMillis - bedtime.timeInMillis) / (1000 * 60 * 60.0)) * 10)
            .roundToInt() / 10.0

        val sleepQuality = when {
            sleepDuration < 6 -> "Poor"
            sleepDuration <= 8.5 -> "Good"
            else -> "Excellent"
        }

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(wakeup.time)
        val bedtimeIso = toUtcIso(bedtime.time)
        val wakeupIso = toUtcIso(wakeup.time)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.submitSleepDaily(
                    date = date,
                    bedtime = bedtimeIso,
                    wakeup = wakeupIso,
                    totalSleepHours = sleepDuration,
                    sleepQuality = sleepQuality,
                )
                Toast.makeText(requireContext(), "Sleep data saved successfully", Toast.LENGTH_SHORT).show()
                binding.sleepQualityResult.text = "Sleep Quality: $sleepQuality - ${formatHours(sleepDuration)}"
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
                        bedtime = parseUtcIso(item.bedtime),
                        wakeup = parseUtcIso(item.wakeup),
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

    private fun toUtcIso(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(date)
    }

    private fun parseUtcIso(value: String?): Date? {
        if (value.isNullOrBlank()) return null
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        )
        return formats.firstNotNullOfOrNull { format ->
            runCatching { format.parse(value) }.getOrNull()
        }
    }

    private fun formatHours(value: Double): String {
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
        return "$formatted hours"
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
    val sleepDuration: Double = 0.0,
    val sleepQuality: String = ""
)
