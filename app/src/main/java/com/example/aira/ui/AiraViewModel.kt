package com.example.aira.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aira.AiraApplication
import com.example.aira.data.history.HistoryRepository
import com.example.aira.data.model.AssistantHistoryItem
import com.example.aira.data.model.AssistantStatus
import com.example.aira.data.preferences.PreferencesManager
import com.example.aira.service.AssistantService
import com.example.aira.service.AssistantStateHolder
import com.example.aira.speech.SpeechModelManager
import com.example.aira.utils.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiraViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager: PreferencesManager = AiraApplication.instance.preferencesManager
    private val historyRepository: HistoryRepository = HistoryRepository(preferencesManager)
    val modelManager: SpeechModelManager = SpeechModelManager(application)

    val assistantStatus: StateFlow<AssistantStatus> = AssistantStateHolder.status

    val conversationHistory: StateFlow<List<AssistantHistoryItem>> = historyRepository.historyFlow

    val timeoutMinutes: StateFlow<Int> = preferencesManager.timeoutMinutesFlow
    val voiceResponseEnabled: StateFlow<Boolean> = preferencesManager.voiceResponseEnabledFlow
    val speechRate: StateFlow<Float> = preferencesManager.speechRateFlow
    val selectedModelName: StateFlow<String> = preferencesManager.selectedModelFlow
    val wakePhrase: StateFlow<String> = preferencesManager.wakePhraseFlow

    // Download progress state: null = not downloading, 0..100 = downloading
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    fun toggleListening(context: Context) {
        val currentStatus = assistantStatus.value
        if (currentStatus.isListening) {
            AssistantService.stopListening(context)
        } else {
            AssistantService.startListening(context)
        }
    }

    fun stopListening(context: Context) {
        AssistantService.stopListening(context)
    }

    fun clearHistory() {
        historyRepository.clearHistory()
    }

    fun setTimeoutMinutes(minutes: Int) {
        preferencesManager.setTimeoutMinutes(minutes)
    }

    fun setVoiceResponse(enabled: Boolean) {
        preferencesManager.setVoiceResponseEnabled(enabled)
    }

    fun setSpeechRate(rate: Float) {
        preferencesManager.setSpeechRate(rate)
    }

    fun setSelectedModel(modelName: String) {
        preferencesManager.setSelectedModelName(modelName)
    }

    fun setWakePhrase(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isNotBlank()) {
            preferencesManager.setWakePhrase(trimmed)
        }
    }

    fun downloadModel(modelInfo: SpeechModelManager.ModelInfo) {
        viewModelScope.launch {
            _downloadProgress.value = 0
            _downloadError.value = null

            val result = modelManager.downloadAndInstallModel(modelInfo) { progress ->
                _downloadProgress.value = progress
            }

            if (result.isSuccess) {
                _downloadProgress.value = null
                preferencesManager.setSelectedModelName(modelInfo.name)
            } else {
                _downloadProgress.value = null
                _downloadError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to download model"
            }
        }
    }

    fun clearDownloadError() {
        _downloadError.value = null
    }

    fun isRecordAudioGranted(): Boolean = PermissionManager.hasRecordAudioPermission(getApplication())
    fun isNotificationGranted(): Boolean = PermissionManager.hasNotificationPermission(getApplication())
    fun isIgnoringBatteryOptimizations(): Boolean = PermissionManager.isIgnoringBatteryOptimizations(getApplication())
}
