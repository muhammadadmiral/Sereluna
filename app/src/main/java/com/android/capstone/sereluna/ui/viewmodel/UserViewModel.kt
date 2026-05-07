package com.android.capstone.sereluna.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.capstone.sereluna.data.repository.UserRepository
import kotlinx.coroutines.launch

// A sealed class to represent the state of a UI operation
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class UserViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _userData = MutableLiveData<UiState<Map<String, Any>>>()
    val userData: LiveData<UiState<Map<String, Any>>> = _userData

    private val _updateState = MutableLiveData<UiState<Unit>>()
    val updateState: LiveData<UiState<Unit>> = _updateState

    fun loadUserData() {
        viewModelScope.launch {
            _userData.value = UiState.Loading
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _userData.value = UiState.Error("User not logged in.")
                return@launch
            }
            val data = repository.getUserData(userId)
            if (data != null) {
                _userData.value = UiState.Success(data)
            } else {
                _userData.value = UiState.Error("Failed to load user data.")
            }
        }
    }

    fun saveUserProfile(name: String, newImageUri: Uri?) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _updateState.value = UiState.Error("User not logged in.")
                return@launch
            }
            val result = repository.updateProfile(userId, name, newImageUri)
            result.onSuccess {
                _updateState.value = UiState.Success(Unit)
            }.onFailure {
                _updateState.value = UiState.Error(it.message ?: "Unknown error during profile update.")
            }
        }
    }
}
