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

    fun loadStats(days: Int) {
        viewModelScope.launch {
            _wellbeingData.value = UiState.Loading
            _moodData.value = UiState.Loading
            _sleepData.value = UiState.Loading

            try {
                val range = "${days}d"
                _wellbeingData.value = UiState.Success(repository.getWellbeingStatistics(range))
            } catch (e: Exception) {
                _wellbeingData.value = UiState.Error(e.message ?: "Gagal memuat statistik wellbeing.")
            }

            try {
                val mood = repository.getMoodDistribution(days)
                _moodData.value = UiState.Success(mood)
            } catch (e: Exception) {
                _moodData.value = UiState.Error(e.message ?: "Gagal memuat statistik mood.")
            }

            try {
                val sleep = repository.getSleepTrends(days)
                _sleepData.value = UiState.Success(sleep)
            } catch (e: Exception) {
                _sleepData.value = UiState.Error(e.message ?: "Gagal memuat statistik tidur.")
            }
        }
    }
}
