package com.app001.virtualcamera.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VirtualCameraUiState(
    val hasPermissions: Boolean = false,
    val isLoading: Boolean = false,
    val isCameraActive: Boolean = false,
    val isVideoPlaying: Boolean = false,
    val currentVideoPath: String? = null,
    val errorMessage: String? = null
)

class VirtualCameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VirtualCameraUiState())
    val uiState: StateFlow<VirtualCameraUiState> = _uiState.asStateFlow()

    fun onPermissionsGranted() {
        _uiState.value = _uiState.value.copy(
            hasPermissions = true,
            isLoading = true
        )
        
        viewModelScope.launch {
            // Simulate loading time
            delay(1000)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isCameraActive = true
            )
        }
    }

    fun startCamera() {
        _uiState.value = _uiState.value.copy(isCameraActive = true)
    }

    fun stopCamera() {
        _uiState.value = _uiState.value.copy(
            isCameraActive = false,
            isVideoPlaying = false
        )
    }

    fun startVideo(videoPath: String) {
        _uiState.value = _uiState.value.copy(
            isVideoPlaying = true,
            currentVideoPath = videoPath
        )
    }

    fun stopVideo() {
        _uiState.value = _uiState.value.copy(
            isVideoPlaying = false,
            currentVideoPath = null
        )
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
