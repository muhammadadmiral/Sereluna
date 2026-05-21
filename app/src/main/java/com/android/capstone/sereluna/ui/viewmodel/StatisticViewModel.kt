package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.api.MoodCountDto
import com.android.capstone.sereluna.data.api.MoodDistributionResponseDto
import com.android.capstone.sereluna.data.api.SleepTrendItemDto
import com.android.capstone.sereluna.data.api.SleepTrendsResponseDto
import com.android.capstone.sereluna.data.api.WellbeingStatisticsResponseDto
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

class StatisticViewModel : ViewModel() {

    private val repository = SerelunaRepository()

    private val _moodData = MutableLiveData<UiState<MoodDistributionResponseDto>>()
    val moodData: LiveData<UiState<MoodDistributionResponseDto>> = _moodData

    private val _sleepData = MutableLiveData<UiState<SleepTrendsResponseDto>>()
    val sleepData: LiveData<UiState<SleepTrendsResponseDto>> = _sleepData

    private val _wellbeingData = MutableLiveData<UiState<WellbeingStatisticsResponseDto>>()
    val wellbeingData: LiveData<UiState<WellbeingStatisticsResponseDto>> = _wellbeingData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val moodCache = mutableMapOf<Int, MoodDistributionResponseDto>()
    private val trendCache = mutableMapOf<Int, SleepTrendsResponseDto>()
    private val wellbeingCache = mutableMapOf<Int, WellbeingStatisticsResponseDto>()
    private val failedPeriods = mutableMapOf<Int, String>()

    private var selectedPeriod: Int = 7
    private var isFetching = false

    fun fetchAll(forceRefresh: Boolean = false) {
        if (!forceRefresh && hasAllWellbeingRanges()) {
            _loading.value = false
            switchPeriod(selectedPeriod)
            return
        }
        if (isFetching) return

        viewModelScope.launch {
            isFetching = true
            _loading.value = true
            val periods = listOf(7, 30, 90)

            val jobs = periods.map { days ->
                launch {
                    if (!forceRefresh && wellbeingCache.containsKey(days)) return@launch
                    try {
                        val range = "${days}d"
                        val wellbeing = repository.getWellbeingStatistics(range)
                        wellbeingCache[days] = wellbeing
                        moodCache[days] = wellbeing.toMoodDistribution()
                        trendCache[days] = wellbeing.toWellbeingTrend()
                        failedPeriods.remove(days)

                        if (days == selectedPeriod) {
                            _wellbeingData.postValue(UiState.Success(wellbeing))
                            _moodData.postValue(UiState.Success(moodCache.getValue(days)))
                            _sleepData.postValue(UiState.Success(trendCache.getValue(days)))
                        }
                    } catch (e: Exception) {
                        failedPeriods[days] = e.message ?: "Gagal memuat statistik."
                        if (days == selectedPeriod) {
                            _wellbeingData.postValue(UiState.Error(failedPeriods.getValue(days)))
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
            isFetching = false
            _loading.value = false
            switchPeriod(selectedPeriod)
        }
    }

    fun switchPeriod(days: Int) {
        selectedPeriod = days
        wellbeingCache[days]?.let {
            _wellbeingData.value = UiState.Success(it)
            moodCache[days]?.let { mood -> _moodData.value = UiState.Success(mood) }
            trendCache[days]?.let { trend -> _sleepData.value = UiState.Success(trend) }
            return
        }

        failedPeriods[days]?.let {
            _wellbeingData.value = UiState.Error(it)
            return
        }

        if (!isFetching) fetchAll()
    }

    fun loadStats(days: Int) {
        selectedPeriod = days
        if (hasAllWellbeingRanges() || wellbeingCache.containsKey(days) || failedPeriods.containsKey(days)) switchPeriod(days) else fetchAll()
    }

    private fun hasAllWellbeingRanges(): Boolean {
        return listOf(7, 30, 90).all { wellbeingCache.containsKey(it) }
    }

    private fun WellbeingStatisticsResponseDto.toMoodDistribution(): MoodDistributionResponseDto {
        return MoodDistributionResponseDto(
            period_days = period_days,
            data = mood_distribution.map { (mood, count) -> MoodCountDto(mood, count) },
            dominant_mood = dominant_mood,
            insight = insights.firstOrNull()
        )
    }

    private fun WellbeingStatisticsResponseDto.toWellbeingTrend(): SleepTrendsResponseDto {
        val scores = daily_items.mapNotNull { it.wellbeing_score?.toDouble() }
        return SleepTrendsResponseDto(
            average_hours = average_wellbeing_score ?: scores.average().takeIf { !it.isNaN() } ?: 0.0,
            items = daily_items.mapIndexedNotNull { index, item ->
                val score = item.wellbeing_score ?: return@mapIndexedNotNull null
                SleepTrendItemDto(
                    date = item.date.ifBlank { (index + 1).toString() },
                    hours = score.toDouble()
                )
            },
            insight = insights.getOrNull(1) ?: insights.firstOrNull()
        )
    }
}
