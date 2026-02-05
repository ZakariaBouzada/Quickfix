package com.example.bikerepairapp.notifications

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class SimpleLifecycleObserver(
    private val onDestroy: () -> Unit
) : DefaultLifecycleObserver {
    override fun onDestroy(owner: LifecycleOwner) {
        onDestroy()
    }
}
