package com.jght.wearosstopwatch.presentation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StopWatchViewModel: ViewModel() {

    private val _elapsedTime = MutableStateFlow(0L)
    private val _timerState = MutableStateFlow(TimerState.RESET)
    val timerState = _timerState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")

    @RequiresApi(Build.VERSION_CODES.O)
    val stopWatchText = _elapsedTime
        .map {milis ->
            LocalTime.ofNanoOfDay(milis * 1_000_000).format(formatter)
         }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "00:00:00:000"
        )
    init {
        _timerState
            .flatMapLatest { timerState ->
                getTimerFlow(
                    isRunning = timerState == TimerState.RUNNING
                )
            }
            .onEach {timeDiff ->
                _elapsedTime.update {
                    it + timeDiff
                }
            }
            .launchIn(viewModelScope)
    }

    fun toggleRunning(){
        when(timerState.value) {
            TimerState.RUNNING -> _timerState.update { TimerState.PAUSED }
            TimerState.PAUSED,
            TimerState.RESET -> _timerState.update { TimerState.RUNNING }
        }
    }

    fun resetTimer(){
        _timerState.update { TimerState.RESET }
        _elapsedTime.update { 0L }
    }

    private fun getTimerFlow(isRunning: Boolean): Flow<Long> {
        return flow {
            var startMilis = System.currentTimeMillis()
            while (isRunning){
                val currentMilis = System.currentTimeMillis()
                val timeDiff = if (currentMilis > startMilis){
                    currentMilis - startMilis
                } else 0L
                emit(timeDiff)
                startMilis = System.currentTimeMillis()
                delay(10L)
            }
        }
    }
}