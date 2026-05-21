package com.android.capstone.sereluna.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

// A sealed class to represent the state of a UI operation
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class UserViewModel : ViewModel() {

    private val repository = SerelunaRepository()

    private val _userData = MutableLiveData<UiState<Map<String, Any>>>()
    val userData: LiveData<UiState<Map<String, Any>>> = _userData

    private val _updateState = MutableLiveData<UiState<Unit>>()
    val updateState: LiveData<UiState<Unit>> = _updateState

    private val _screeningStatus = MutableLiveData<UiState<com.android.capstone.sereluna.data.api.ScreeningStatusDto>>()
    val screeningStatus: LiveData<UiState<com.android.capstone.sereluna.data.api.ScreeningStatusDto>> = _screeningStatus

    fun loadUserData(forceRefresh: Boolean = false) {
        if (!forceRefresh && _userData.value is UiState.Success) {
            return // Use cached data
        }
        
        viewModelScope.launch {
            _userData.value = UiState.Loading
            try {
                _userData.value = UiState.Success(repository.getProfileAsMap())
            } catch (e: Exception) {
                _userData.value = UiState.Error(e.message ?: "Failed to load user data.")
            }
        }
    }

    fun loadScreeningStatus(forceRefresh: Boolean = false) {
        if (!forceRefresh && _screeningStatus.value is UiState.Success) {
            return
        }

        viewModelScope.launch {
            _screeningStatus.value = UiState.Loading
            try {
                _screeningStatus.value = UiState.Success(repository.getScreeningStatus())
            } catch (e: Exception) {
                _screeningStatus.value = UiState.Error(e.message ?: "Failed to load screening status.")
            }
        }
    }

    fun saveUserProfile(name: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            try {
                repository.updateProfile(name, newImageUri)
                _updateState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _updateState.value = UiState.Error(e.message ?: "Unknown error during profile update.")
            }
        }
    }
}
