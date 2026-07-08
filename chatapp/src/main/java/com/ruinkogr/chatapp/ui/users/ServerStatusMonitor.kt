package com.ruinkogr.chatapp.ui.users

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServerStatusMonitor {
    private val _isServerOnline = MutableStateFlow<Boolean?>(null)
    val isServerOnline = _isServerOnline.asStateFlow()

    fun setStatus(online: Boolean) {
        _isServerOnline.value = online
    }
}
