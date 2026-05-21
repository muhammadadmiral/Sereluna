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
import com.android.capstone.sereluna.data.adapter.SleepHistoryAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
            validateAndSave()
        }

        loadSleepHistory()
    }

    private fun validateAndSave() {
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

        // Handle overnight sleep
        if (!wakeup.after(bedtime)) {
            wakeup.add(Calendar.DATE, 1)
        }

        val duration = (((wakeup.timeInMillis - bedtime.timeInMillis) / (1000 * 60 * 60.0)) * 10)
            .roundToInt() / 10.0

        if (duration < 3.0) {
            showConfirmation("Tidurmu sangat pendek (${duration} jam). Yakin ingin simpan?") {
                performSave(bedtime.time, wakeup.time, duration)
            }
        } else if (duration > 12.0) {
            showConfirmation("Tidurmu sangat panjang (${duration} jam). Yakin ingin simpan?") {
                performSave(bedtime.time, wakeup.time, duration)
            }
        } else {
            performSave(bedtime.time, wakeup.time, duration)
        }
    }

    private fun showConfirmation(message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Konfirmasi")
            .setMessage(message)
            .setPositiveButton("Ya, Simpan") { _, _ -> onConfirm() }
            .setNegativeButton("Cek Lagi", null)
            .show()
    }

    private fun performSave(bedtime: Date, wakeup: Date, duration: Double) {
        val checkedId = binding.moodChipGroup.checkedChipId
        val chip = binding.moodChipGroup.findViewById<Chip>(checkedId)
        val quality = chip?.tag?.toString() ?: "Good"

        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(wakeup)
        val bedtimeIso = toUtcIso(bedtime)
        val wakeupIso = toUtcIso(wakeup)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.saveSleepDataButton.isEnabled = false
                repository.submitSleepDaily(
                    date = dateKey,
                    bedtime = bedtimeIso,
                    wakeup = wakeupIso,
                    totalSleepHours = duration,
                    sleepQuality = quality
                )
                Toast.makeText(requireContext(), "Data tidur berhasil disimpan", Toast.LENGTH_SHORT).show()
                loadSleepHistory()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.saveSleepDataButton.isEnabled = true
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
                binding.sleepHistoryRecyclerView.apply {
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = SleepHistoryAdapter(sleepHistory)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show()
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
            }
        )
        return formats.firstNotNullOfOrNull { format ->
            runCatching { format.parse(value) }.getOrNull()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class SleepData(
        val date: String = "",
        val bedtime: Date? = null,
        val wakeup: Date? = null,
        val sleepDuration: Double = 0.0,
        val sleepQuality: String = ""
    )
}
