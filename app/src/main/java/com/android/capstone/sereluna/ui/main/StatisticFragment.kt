package com.android.capstone.sereluna.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.api.MoodDistributionResponseDto
import com.android.capstone.sereluna.data.api.SleepTrendsResponseDto
import com.android.capstone.sereluna.databinding.FragmentStatisticBinding
import com.android.capstone.sereluna.ui.viewmodel.StatisticViewModel
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Locale

class StatisticFragment : Fragment() {

    private var _binding: FragmentStatisticBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticViewModel by viewModels()

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

        viewModel.loadStats(7)
    }

    private fun setupCharts() {
        binding.moodPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            legend.isEnabled = true
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
                val days = if (checkedId == R.id.btn7Days) 7 else 30
                viewModel.loadStats(days)
            }
        }
    }

    private fun setupObservers() {
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
    }

    private fun updateMoodChart(data: MoodDistributionResponseDto) {
        if (data.data.isEmpty()) {
            binding.moodPieChart.clear()
            binding.tvMoodInsight.text = getString(R.string.stats_placeholder)
            return
        }

        val entries = data.data.map { item ->
            PieEntry(item.count.toFloat(), item.mood.replaceFirstChar { c -> c.uppercase() }) 
        }
        
        val dataSet = PieDataSet(entries, "Distribusi Mood")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.calendar_green),
            ContextCompat.getColor(requireContext(), R.color.calendar_blue),
            ContextCompat.getColor(requireContext(), R.color.calendar_yellow),
            ContextCompat.getColor(requireContext(), R.color.calendar_orange),
            ContextCompat.getColor(requireContext(), R.color.calendar_red)
        )
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        binding.moodPieChart.data = PieData(dataSet)
        binding.moodPieChart.invalidate()
        binding.tvMoodInsight.text = data.insight ?: "Analisis mood kamu akan tampil di sini."
    }

    private fun updateSleepChart(data: SleepTrendsResponseDto) {
        binding.tvAvgSleep.text = String.format(Locale.US, "%.1f jam", data.average_hours)
        
        if (data.items.isEmpty()) {
            binding.sleepLineChart.clear()
            binding.tvSleepInsight.text = getString(R.string.stats_placeholder)
            return
        }

        val entries = data.items.mapIndexed { index, item ->
            Entry(index.toFloat(), item.hours.toFloat())
        }

        val dataSet = LineDataSet(entries, "Jam Tidur")
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
        binding.tvSleepInsight.text = data.insight ?: "Analisis kualitas tidur kamu akan tampil di sini."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
