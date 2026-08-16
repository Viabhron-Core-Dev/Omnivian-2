package com.example.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.db.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class TokenPanelViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val metricsDao = db.metricsDao()

    // Emit current time every second to trigger updates
    private val timeTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    // RPM (Requests Per Minute) - last 60 seconds
    val currentRpm: StateFlow<Int> = timeTicker.flatMapLatest { now ->
        metricsDao.getRequestCountSince(now - 60_000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // TPM (Tokens Per Minute) - last 60 seconds
    val currentTpm: StateFlow<Int?> = timeTicker.flatMapLatest { now ->
        metricsDao.getTokensUsedSince(now - 60_000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCost = metricsDao.getTotalEstimatedCost()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
