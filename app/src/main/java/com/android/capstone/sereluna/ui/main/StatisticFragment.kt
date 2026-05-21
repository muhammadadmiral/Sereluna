package com.android.capstone.sereluna.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.api.MoodDistributionResponseDto
import com.android.capstone.sereluna.data.api.SleepTrendsResponseDto
import com.android.capstone.sereluna.data.api.WellbeingStatisticsResponseDto
import com.android.capstone.sereluna.databinding.FragmentStatisticBinding
import com.android.capstone.sereluna.ui.viewmodel.StatisticViewModel
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.util.Locale

class StatisticFragment : Fragment() {

    private var _binding: FragmentStatisticBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharts()
        setupListeners()
        setupObservers()

        viewModel.fetchAll()
    }

    private fun setupCharts() {
        binding.moodPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            legend.isEnabled = true
            isHighlightPerTapEnabled = true
            setTouchEnabled(true)
            animateY(1000, Easing.EaseInOutQuad)
        }

        binding.sleepLineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            axisLeft.setDrawGridLines(true)
            animateX(1000)
        }
    }

    private fun setupListeners() {
        binding.toggleGroupPeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val days = when (checkedId) {
                    R.id.btn7Days -> 7
                    R.id.btn90Days -> 90
                    else -> 30
                }
                viewModel.switchPeriod(days)
            }
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.scrollView.visibility = if (isLoading && binding.tvWellbeingScore.text.isNullOrBlank()) View.INVISIBLE else View.VISIBLE
        }

        viewModel.moodData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> updateMoodChart(state.data)
                is UiState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }

        viewModel.sleepData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> updateSleepChart(state.data)
                is UiState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }

        viewModel.wellbeingData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> updateWellbeingCards(state.data)
                is UiState.Error -> {
                    binding.tvWellbeingScore.text = "--"
                    binding.tvOverallMood.text = "Belum ada insight wellbeing."
                    binding.tvWellbeingInsight.text = state.message
                }
                else -> {}
            }
        }
    }

    private fun updateWellbeingCards(data: WellbeingStatisticsResponseDto) {
        binding.tvWellbeingScore.text = data.average_wellbeing_score?.let {
            String.format(Locale.US, "%.0f", it)
        } ?: "--"
        binding.tvOverallMood.text = data.overall_mood.ifBlank { "Belum cukup data" }
        binding.tvDominantMood.text = data.dominant_mood?.toDisplayLabel() ?: "-"

        val baseline = data.screening_context
        binding.tvScreeningBaseline.text = if (baseline != null) {
            "Stres ${baseline.stress ?: "-"} • Cemas ${baseline.anxiety ?: "-"} • Depresi ${baseline.depression ?: "-"}"
        } else {
            "Baseline DASS-21 belum tersedia"
        }
        binding.tvStatsDisclaimer.text = data.disclaimer
            ?: baseline?.disclaimer
            ?: "Insight ini bukan diagnosis medis."

        binding.tvWellbeingInsight.text = data.insights.firstOrNull()
            ?: "Insight akan muncul setelah data mood, diary, dan tidur terkumpul."

        if (data.mood_distribution.isNotEmpty()) {
            val dto = MoodDistributionResponseDto(
                period_days = data.period_days,
                data = data.mood_distribution.map { (mood, count) ->
                    com.android.capstone.sereluna.data.api.MoodCountDto(mood, count)
                },
                dominant_mood = data.dominant_mood,
                insight = data.insights.firstOrNull()
            )
            updateMoodChart(dto)
        }
    }

    private fun updateMoodChart(data: MoodDistributionResponseDto) {
        val visibleItems = data.data.filter { it.count > 0 }
        if (visibleItems.isEmpty()) {
            binding.moodPieChart.clear()
            binding.tvMoodInsight.text = "Belum ada distribusi mood untuk periode ini."
            return
        }

        val total = visibleItems.sumOf { it.count }.coerceAtLeast(1)
        val entries = visibleItems.map { item ->
            PieEntry(item.count.toFloat(), moodDisplayName(item.mood), item.mood)
        }
        
        val dataSet = PieDataSet(entries, "Distribusi Mood")
        dataSet.colors = visibleItems.map { moodColor(it.mood) }
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        dataSet.selectionShift = 12f

        binding.moodPieChart.data = PieData(dataSet)
        binding.moodPieChart.centerText = "Tap bagian chart"
        binding.moodPieChart.setCenterTextSize(12f)
        binding.moodPieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val entry = e as? PieEntry ?: return
                val moodKey = entry.data as? String ?: entry.label
                val count = entry.value.toInt()
                val percent = (count * 100f) / total
                binding.tvMoodInsight.text = "${moodDisplayName(moodKey)} tercatat $count kali (${String.format(Locale.US, "%.0f", percent)}%) dalam periode ini. ${moodDetailText(moodKey)}"
            }

            override fun onNothingSelected() {
                binding.tvMoodInsight.text = data.insight ?: "Tap salah satu bagian chart untuk melihat detail mood."
            }
        })
        binding.moodPieChart.invalidate()
        binding.tvMoodInsight.text = data.insight ?: "Tap salah satu bagian chart untuk melihat detail mood."
    }

    private fun updateSleepChart(data: SleepTrendsResponseDto) {
        binding.tvAvgSleep.text = String.format(Locale.US, "%.0f", data.average_hours)
        
        if (data.items.isEmpty()) {
            binding.sleepLineChart.clear()
            binding.tvSleepInsight.text = getString(R.string.stats_placeholder)
            return
        }

        val entries = data.items.mapIndexed { index, item ->
            Entry(index.toFloat(), item.hours.toFloat())
        }

        val dataSet = LineDataSet(entries, "Skor Wellbeing")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.calendar_blue)
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.calendar_blue))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.calendar_blue)
        dataSet.fillAlpha = 50

        binding.sleepLineChart.xAxis.valueFormatter = IndexAxisValueFormatter(data.items.map { it.date.takeLast(5) })
        binding.sleepLineChart.data = LineData(dataSet)
        binding.sleepLineChart.invalidate()
        binding.tvSleepInsight.text = data.insight ?: "Tren wellbeing kamu akan tampil di sini."
    }

    private fun String.toDisplayLabel(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun moodDisplayName(mood: String): String {
        return when (mood.lowercase(Locale.ROOT)) {
            "happy" -> "Senang"
            "neutral" -> "Netral"
            "sad" -> "Sedih"
            "anxious" -> "Cemas"
            "angry", "anger" -> "Marah"
            else -> mood.toDisplayLabel()
        }
    }

    private fun moodDetailText(mood: String): String {
        return when (mood.lowercase(Locale.ROOT)) {
            "happy" -> "Ini biasanya menandakan hari dengan pengalaman positif atau energi yang lebih baik."
            "neutral" -> "Ini menandakan mood relatif stabil atau tidak terlalu ekstrem."
            "sad" -> "Perhatikan pemicu harian seperti tidur, diary, atau beban aktivitas."
            "anxious" -> "Coba lihat pola tidur dan catatan harian di tanggal terkait."
            "angry", "anger" -> "Perhatikan konteks konflik, kelelahan, atau tekanan pada hari terkait."
            else -> "Detail akan lebih akurat kalau backend mengirim contoh tanggal dan pemicu utamanya."
        }
    }

    private fun moodColor(mood: String): Int {
        val colorRes = when (mood.lowercase(Locale.ROOT)) {
            "happy" -> R.color.calendar_green
            "neutral" -> R.color.calendar_blue
            "sad" -> R.color.calendar_yellow
            "anxious" -> R.color.calendar_orange
            "angry", "anger" -> R.color.calendar_red
            else -> R.color.brand_purple_primary
        }
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
