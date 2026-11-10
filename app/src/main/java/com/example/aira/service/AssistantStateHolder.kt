package com.example.aira.service

import com.example.aira.data.model.AssistantStatus
import com.example.aira.data.model.ListeningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AssistantStateHolder {

    private val _status = MutableStateFlow(AssistantStatus())
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    fun updateStatus(transform: (AssistantStatus) -> AssistantStatus) {
        _status.update(transform)
    }

    fun setState(state: ListeningState) {
        _status.update {
            it.copy(
                state = state,
                isListening = (state != ListeningState.OFF && state != ListeningState.ERROR)
            )
        }
    }

    fun resetToOff(errorMessage: String? = null) {
        _status.update {
            it.copy(
                state = if (errorMessage != null) ListeningState.ERROR else ListeningState.OFF,
                isListening = false,
                errorMessage = errorMessage,
                audioAmplitude = 0f
            )
        }
    }
}
