package com.example.aira.data.model

enum class ListeningState {
    OFF,
    STARTING,
    LISTENING_FOR_WAKE_WORD,
    LISTENING_FOR_COMMAND,
    PROCESSING_COMMAND,
    SPEAKING_RESPONSE,
    ERROR
}

data class AssistantStatus(
    val state: ListeningState = ListeningState.OFF,
    val isListening: Boolean = false,
    val lastRecognizedText: String = "",
    val lastAssistantResponse: String = "",
    val lastCommandStatus: String = "",
    val errorMessage: String? = null,
    val timeoutRemainingSeconds: Long = 600L,
    val audioAmplitude: Float = 0f
)
