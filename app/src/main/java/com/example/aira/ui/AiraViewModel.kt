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
import com.example.aira.speech.AudioRecorder
import com.example.aira.speech.SpeechModelManager
import com.example.aira.utils.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val selectedProfile: StateFlow<String> = preferencesManager.selectedProfileFlow
    val voiceSecurityEnabled: StateFlow<Boolean> = preferencesManager.voiceSecurityEnabledFlow
    val voiceMatchThreshold: StateFlow<Float> = preferencesManager.voiceMatchThresholdFlow
    val wakePhrase: StateFlow<String> = preferencesManager.wakePhraseFlow

    val speakerVerificationManager: com.example.aira.security.SpeakerVerificationManager =
        com.example.aira.security.SpeakerVerificationManager(application)
    val voiceEnrollmentManager: com.example.aira.security.VoiceEnrollmentManager =
        com.example.aira.security.VoiceEnrollmentManager(application, speakerVerificationManager)

    private val _isOwnerEnrolledFlow = MutableStateFlow(voiceEnrollmentManager.isEnrolled())
    val isOwnerEnrolledFlow: StateFlow<Boolean> = _isOwnerEnrolledFlow.asStateFlow()

    // Voice Enrollment State
    private val _isEnrollingVoice = MutableStateFlow(false)
    val isEnrollingVoice: StateFlow<Boolean> = _isEnrollingVoice.asStateFlow()

    private val _enrollmentCountdown = MutableStateFlow(3)
    val enrollmentCountdown: StateFlow<Int> = _enrollmentCountdown.asStateFlow()

    private val _enrollmentProgress = MutableStateFlow(0f)
    val enrollmentProgress: StateFlow<Float> = _enrollmentProgress.asStateFlow()

    private val _enrollmentAmplitude = MutableStateFlow(0f)
    val enrollmentAmplitude: StateFlow<Float> = _enrollmentAmplitude.asStateFlow()

    private val _enrollmentError = MutableStateFlow<String?>(null)
    val enrollmentError: StateFlow<String?> = _enrollmentError.asStateFlow()

    private val _enrollmentSuccess = MutableStateFlow(false)
    val enrollmentSuccess: StateFlow<Boolean> = _enrollmentSuccess.asStateFlow()

    private var enrollmentRecorder: AudioRecorder? = null
    private var enrollmentJob: Job? = null

    // Download progress state: null = not downloading, 0..100 = downloading
    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    private val _downloadingModelName = MutableStateFlow<String?>(null)
    val downloadingModelName: StateFlow<String?> = _downloadingModelName.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private val _modelsUpdated = MutableStateFlow(0)
    val modelsUpdated: StateFlow<Int> = _modelsUpdated.asStateFlow()

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

    fun selectVoskModel(modelName: String) {
        preferencesManager.setSelectedModelName(modelName)
    }

    fun setSelectedModel(modelName: String) {
        selectVoskModel(modelName)
    }

    fun setSelectedProfile(profile: String) {
        preferencesManager.setSelectedProfileType(profile)
    }

    fun setVoiceSecurityEnabled(enabled: Boolean) {
        preferencesManager.setVoiceSecurityEnabled(enabled)
    }

    fun setVoiceMatchThreshold(threshold: Float) {
        preferencesManager.setVoiceMatchThreshold(threshold)
    }

    fun clearVoiceEnrollment() {
        voiceEnrollmentManager.clearEnrollment()
        _isOwnerEnrolledFlow.value = false
    }

    fun refreshOwnerEnrollment() {
        _isOwnerEnrolledFlow.value = voiceEnrollmentManager.isEnrolled()
    }

    fun startVoiceEnrollment(onSuccess: (() -> Unit)? = null) {
        if (_isEnrollingVoice.value) return
        _isEnrollingVoice.value = true
        _enrollmentError.value = null
        _enrollmentSuccess.value = false
        _enrollmentProgress.value = 0f
        _enrollmentCountdown.value = 3

        val audioChunks = java.util.Collections.synchronizedList(mutableListOf<ByteArray>())

        // Stop background assistant listening temporarily to release mic
        if (assistantStatus.value.isListening) {
            AssistantService.stopListening(getApplication())
        }

        enrollmentRecorder?.stop()
        enrollmentRecorder = AudioRecorder(
            onAudioChunk = { buffer, length ->
                if (length > 0) {
                    audioChunks.add(buffer.copyOf(length))
                }
            },
            onAmplitudeChanged = { amp ->
                _enrollmentAmplitude.value = amp
            }
        )

        val started = enrollmentRecorder?.start() ?: false
        if (!started) {
            _isEnrollingVoice.value = false
            _enrollmentError.value = "Could not access microphone hardware. Please check permissions."
            return
        }

        enrollmentJob?.cancel()
        enrollmentJob = viewModelScope.launch(Dispatchers.Default) {
            val totalDurationMs = 3500L
            val stepMs = 100L
            var elapsedMs = 0L

            while (elapsedMs < totalDurationMs && isActive) {
                delay(stepMs)
                elapsedMs += stepMs
                _enrollmentProgress.value = (elapsedMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                val remainingSec = ((totalDurationMs - elapsedMs) / 1000L).toInt() + 1
                _enrollmentCountdown.value = remainingSec.coerceAtLeast(1)
            }

            // Finished recording period
            enrollmentRecorder?.stop()
            enrollmentRecorder = null
            _enrollmentAmplitude.value = 0f

            // Convert PCM ByteArray chunks to FloatArray
            val chunksCopy = synchronized(audioChunks) { audioChunks.toList() }
            val totalBytes = chunksCopy.sumOf { it.size }
            val totalSamples = totalBytes / 2
            val floatSamples = FloatArray(totalSamples)
            var sampleIdx = 0
            for (chunk in chunksCopy) {
                var i = 0
                while (i + 1 < chunk.size && sampleIdx < totalSamples) {
                    val s = (chunk[i].toInt() and 0xFF) or (chunk[i + 1].toInt() shl 8)
                    floatSamples[sampleIdx++] = s.toShort() / 32768.0f
                    i += 2
                }
            }

            val result = voiceEnrollmentManager.enrollVoiceFromAudio(floatSamples)
            if (result.isSuccess) {
                _isOwnerEnrolledFlow.value = true
                preferencesManager.setVoiceSecurityEnabled(true)
                _enrollmentSuccess.value = true
                _isEnrollingVoice.value = false
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke()
                }
            } else {
                _isEnrollingVoice.value = false
                _enrollmentError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Voice registration failed. Please speak clearly and try again."
            }
        }
    }

    fun cancelVoiceEnrollment() {
        enrollmentJob?.cancel()
        enrollmentJob = null
        enrollmentRecorder?.stop()
        enrollmentRecorder = null
        _isEnrollingVoice.value = false
        _enrollmentAmplitude.value = 0f
        _enrollmentProgress.value = 0f
        _enrollmentError.value = null
    }

    fun resetEnrollmentState() {
        _enrollmentError.value = null
        _enrollmentSuccess.value = false
        _isEnrollingVoice.value = false
        _enrollmentProgress.value = 0f
        _enrollmentAmplitude.value = 0f
    }

    fun setWakePhrase(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isNotBlank()) {
            preferencesManager.setWakePhrase(trimmed)
        }
    }

    fun downloadModel(modelInfo: SpeechModelManager.ModelInfo) {
        viewModelScope.launch {
            _downloadingModelName.value = modelInfo.name
            _downloadProgress.value = 0
            _downloadError.value = null

            val result = modelManager.downloadAndInstallModel(modelInfo) { progress ->
                _downloadProgress.value = progress
            }

            if (result.isSuccess) {
                _downloadProgress.value = null
                _downloadingModelName.value = null
                _modelsUpdated.value += 1
                preferencesManager.setSelectedModelName(modelInfo.name)
            } else {
                _downloadProgress.value = null
                _downloadingModelName.value = null
                _downloadError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to download model"
            }
        }
    }

    fun clearDownloadError() {
        _downloadError.value = null
    }

    fun isRecordAudioGranted(): Boolean = PermissionManager.hasRecordAudioPermission(getApplication())
    fun isNotificationGranted(): Boolean = PermissionManager.hasNotificationPermission(getApplication())
    fun isReadContactsGranted(): Boolean = PermissionManager.hasReadContactsPermission(getApplication())
    fun isOverlayPermissionGranted(): Boolean = PermissionManager.hasOverlayPermission(getApplication())
    fun isIgnoringBatteryOptimizations(): Boolean = PermissionManager.isIgnoringBatteryOptimizations(getApplication())

    override fun onCleared() {
        super.onCleared()
        cancelVoiceEnrollment()
    }
}
