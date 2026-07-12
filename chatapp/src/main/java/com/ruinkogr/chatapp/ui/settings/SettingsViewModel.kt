package com.ruinkogr.chatapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
) : ViewModel() {

    val languageCode: StateFlow<String> = settingsManager.languageCodeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val trackServerStatus: StateFlow<Boolean> = settingsManager.trackServerStatusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setLanguage(code: String) {
        viewModelScope.launch {
            settingsManager.saveLanguage(code)
        }
    }

    fun toggleServerTracking(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setServerTracking(enabled)
        }
    }
}