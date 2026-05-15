package com.vaultapp.securevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.vaultapp.securevault.data.SecureVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: SecureVideoRepository
) : ViewModel() {

    private val _subtitleUri = MutableStateFlow<String?>(null)
    val subtitleUri: StateFlow<String?> = _subtitleUri.asStateFlow()

    fun loadSubtitle(uri: String) {
        _subtitleUri.value = uri
    }

    fun clearSubtitle() {
        _subtitleUri.value = null
    }

    suspend fun getVideoById(id: Long) = repository.getVideoById(id)
}
