package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.api.DoctorDto
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

class DoctorViewModel(
    private val repository: SerelunaRepository = SerelunaRepository()
) : ViewModel() {

    private val _doctors = MutableLiveData<UiState<List<DoctorDto>>>()
    val doctors: LiveData<UiState<List<DoctorDto>>> = _doctors

    fun loadDoctors() {
        _doctors.value = UiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.getDoctors()
                _doctors.value = UiState.Success(response.doctors)
            } catch (e: Exception) {
                _doctors.value = UiState.Error(e.message ?: "Failed to load doctors")
            }
        }
    }
}