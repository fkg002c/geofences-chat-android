package com.ruinkogr.chatapp.websocket

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(
    private val webSocketManager: WebSocketManager
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.w(TAG, "onStart owner ${owner.javaClass.simpleName}")
        webSocketManager.connect()
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.w(TAG, "onStop owner ${owner.javaClass.simpleName}")
        webSocketManager.disconnect()
        super.onStop(owner)
    }

    companion object {
        private const val TAG = "AppLifecycleObserver"
    }
}
