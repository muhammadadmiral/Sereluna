package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.api.MoodDistributionResponseDto
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
    private val sleepCache = mutableMapOf<Int, SleepTrendsResponseDto>()
    private val wellbeingCache = mutableMapOf<Int, WellbeingStatisticsResponseDto>()

    fun fetchAll() {
        if (wellbeingCache.isNotEmpty()) return
        
        viewModelScope.launch {
            _loading.value = true
            val periods = listOf(7, 30, 90)
            
            val jobs = periods.map { days ->
                launch {
                    try {
                        val range = "${days}d"
                        val wellbeing = repository.getWellbeingStatistics(range)
                        wellbeingCache[days] = wellbeing
                        
                        val mood = repository.getMoodDistribution(days)
                        moodCache[days] = mood
                        
                        val sleep = repository.getSleepTrends(days)
                        sleepCache[days] = sleep
                        
                        // Set initial data to 7 days if it's the first one to finish or specifically 7
                        if (days == 7) {
                            _wellbeingData.postValue(UiState.Success(wellbeing))
                            _moodData.postValue(UiState.Success(mood))
                            _sleepData.postValue(UiState.Success(sleep))
                        }
                    } catch (e: Exception) {
                        if (days == 7) {
                            _wellbeingData.postValue(UiState.Error(e.message ?: "Gagal memuat statistik."))
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
            _loading.value = false
        }
    }

    fun switchPeriod(days: Int) {
        wellbeingCache[days]?.let { _wellbeingData.value = UiState.Success(it) }
        moodCache[days]?.let { _moodData.value = UiState.Success(it) }
        sleepCache[days]?.let { _sleepData.value = UiState.Success(it) }
    }

    fun loadStats(days: Int) {
        if (wellbeingCache.containsKey(days)) {
            switchPeriod(days)
            return
        }
        fetchAll()
    }
}
