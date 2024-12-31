package com.android.servicedemo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CounterViewModel : ViewModel() {
    private val _counterFlow = MutableSharedFlow<Int>(replay = 1)
    val counterFlow = _counterFlow.asSharedFlow()

    suspend fun updateCounter(count: Int) {
        _counterFlow.emit(count)
    }

    companion object {
        private var instance: CounterViewModel? = null
        
        fun getInstance(): CounterViewModel {
            if (instance == null) {
                instance = CounterViewModel()
            }
            return instance!!
        }
    }
}
