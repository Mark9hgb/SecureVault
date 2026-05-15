package com.vaultapp.securevault.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaultapp.securevault.data.SecureVideoRepository
import com.vaultapp.securevault.data.database.VideoEntity
import com.vaultapp.securevault.security.EnvironmentChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: SecureVideoRepository,
    private val environmentChecker: EnvironmentChecker
) : ViewModel() {

    private val _videos = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videos: StateFlow<List<VideoEntity>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    private val _environmentStatus = MutableStateFlow<EnvironmentChecker.EnvironmentStatus?>(null)
    val environmentStatus: StateFlow<EnvironmentChecker.EnvironmentStatus?> = _environmentStatus.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow<VideoEntity?>(null)
    val showDeleteDialog: StateFlow<VideoEntity?> = _showDeleteDialog.asStateFlow()

    init {
        observeVideos()
        checkEnvironment()
    }

    private fun observeVideos() {
        viewModelScope.launch {
            repository.getAllVideos().collect { videoList ->
                _videos.value = videoList
            }
        }
    }

    private fun checkEnvironment() {
        _environmentStatus.value = environmentChecker.checkEnvironment()
    }

    fun importVideo(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _importError.value = null
            try {
                val result = repository.importVideo(uri, fileName)
                result.onFailure { exception ->
                    _importError.value = exception.message ?: "Import failed"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestDelete(video: VideoEntity) {
        _showDeleteDialog.value = video
    }

    fun confirmDelete() {
        val video = _showDeleteDialog.value ?: return
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
        _showDeleteDialog.value = null
    }

    fun cancelDelete() {
        _showDeleteDialog.value = null
    }

    fun clearImportError() {
        _importError.value = null
    }
}
