package com.example.aira.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.aira.AiraApplication
import com.example.aira.commands.CommandProcessor
import com.example.aira.data.history.HistoryRepository
import com.example.aira.data.model.AssistantHistoryItem
import com.example.aira.data.model.ListeningState
import com.example.aira.data.preferences.PreferencesManager
import com.example.aira.security.SpeakerVerificationManager
import com.example.aira.speech.AudioRecorder
import com.example.aira.speech.SpeechEndDetector
import com.example.aira.speech.SpeechModelManager
import com.example.aira.speech.SpeechRecognizer
import com.example.aira.speech.VoskWakeWordDetector
import com.example.aira.speech.WakeWordDetector
import com.example.aira.tts.TextToSpeechManager
import com.example.aira.ui.overlay.VoicePopupManager
import com.example.aira.utils.NotificationHelper
import com.example.aira.utils.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model

/**
 * Foreground service for Aira's 100% offline speech recognition, continuous background
 * wake-word listening ("Hey Assistant"), and voice command execution using Vosk.
 */
class AssistantService : Service() {

    companion object {
        private const val TAG = "AiraAssistantService"
        const val ACTION_START_LISTENING = "com.example.aira.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.example.aira.ACTION_STOP_LISTENING"

        fun startListening(context: Context) {
            val intent = Intent(context, AssistantService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopListening(context: Context) {
            val intent = Intent(context, AssistantService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var inactivityTimerJob: Job? = null

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var historyRepository: HistoryRepository
    private lateinit var modelManager: SpeechModelManager
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var voicePopupManager: VoicePopupManager
    private var speechEndDetector: SpeechEndDetector? = null

    private lateinit var speakerVerificationManager: SpeakerVerificationManager

    private var activeVoskModel: Model? = null
    private var activeModelName: String? = null
    private var wakeWordDetector: WakeWordDetector? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecorder: AudioRecorder? = null
    private val commandAudioChunks = java.util.Collections.synchronizedList(mutableListOf<ByteArray>())

    private var timeoutDurationSeconds: Long = 600L
    private var timeoutSecondsRemaining: Long = 600L

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AssistantService onCreate")
        preferencesManager = AiraApplication.instance.preferencesManager
        historyRepository = HistoryRepository(preferencesManager)
        modelManager = SpeechModelManager(this)
        commandProcessor = CommandProcessor(this)
        ttsManager = TextToSpeechManager(this, preferencesManager)
        speakerVerificationManager = SpeakerVerificationManager(this)

        voicePopupManager = VoicePopupManager(this) {
            Log.d(TAG, "Voice popup dismiss requested by user")
            speechEndDetector?.stop()
            speechRecognizer?.stopListening()
            startWakeWordListening()
        }

        timeoutDurationSeconds = preferencesManager.getTimeoutMinutes() * 60L
        timeoutSecondsRemaining = timeoutDurationSeconds

        // Observe wake phrase changes dynamically
        serviceScope.launch {
            preferencesManager.wakePhraseFlow.collect { newPhrase ->
                Log.d(TAG, "Wake phrase changed in preferences: $newPhrase")
                (wakeWordDetector as? VoskWakeWordDetector)?.updateWakePhrase(newPhrase)
                updateListeningNotification("Aira is listening", "Say '$newPhrase' to activate.")
            }
        }

        // Observe speech model selection changes dynamically
        serviceScope.launch {
            preferencesManager.selectedModelFlow.collect { newModelName ->
                if (activeModelName != null && activeModelName != newModelName && activeVoskModel != null) {
                    Log.d(TAG, "Selected model changed while service active. Switching model to: $newModelName")
                    switchActiveModel(newModelName)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_LISTENING
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_STOP_LISTENING -> {
                Log.d(TAG, "Explicit stop requested via action")
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_LISTENING -> {
                startForegroundWithNotification()
                initializeAndStartListening()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val currentPhrase = preferencesManager.getWakePhrase()
        val notification = NotificationHelper.buildListeningNotification(
            this,
            title = "Aira is listening",
            content = "Say '$currentPhrase' to activate."
        )

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
            Log.d(TAG, "Foreground service started successfully with microphone type")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
            AssistantStateHolder.resetToOff("Failed to start foreground microphone service: ${e.localizedMessage}")
            stopSelf()
        }
    }

    private fun updateListeningNotification(title: String, content: String) {
        val notification = NotificationHelper.buildListeningNotification(this, title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun initializeAndStartListening() {
        if (!PermissionManager.hasRecordAudioPermission(this)) {
            Log.e(TAG, "Microphone permission missing!")
            AssistantStateHolder.resetToOff("Microphone permission required")
            stopSelf()
            return
        }

        AssistantStateHolder.setState(ListeningState.STARTING)

        serviceScope.launch {
            val selectedModelName = preferencesManager.getSelectedModelName()
            if (!modelManager.isModelInstalled(selectedModelName)) {
                Log.w(TAG, "Selected model not installed: $selectedModelName")
                withContext(Dispatchers.Main) {
                    AssistantStateHolder.resetToOff("Speech model not installed. Please download it in Settings.")
                    stopSelf()
                }
                return@launch
            }

            val model = modelManager.getOrLoadModel(selectedModelName)
            if (model == null) {
                Log.e(TAG, "Failed to load speech model: $selectedModelName")
                withContext(Dispatchers.Main) {
                    AssistantStateHolder.resetToOff("Failed to load offline speech model")
                    stopSelf()
                }
                return@launch
            }

            activeVoskModel = model
            activeModelName = selectedModelName
            val wakePhrase = preferencesManager.getWakePhrase()

            wakeWordDetector = VoskWakeWordDetector(model, wakePhrase)
            speechRecognizer = SpeechRecognizer(model)

            withContext(Dispatchers.Main) {
                setupAudioRecorderAndStart()
                resetInactivityTimer()
            }
        }
    }

    private fun switchActiveModel(newModelName: String) {
        serviceScope.launch {
            if (!modelManager.isModelInstalled(newModelName)) {
                Log.w(TAG, "Cannot switch to model: $newModelName is not installed.")
                return@launch
            }

            Log.d(TAG, "Reloading speech engine with new model: $newModelName")
            audioRecorder?.pause()
            wakeWordDetector?.release()
            wakeWordDetector = null
            speechRecognizer?.release()
            speechRecognizer = null

            val model = modelManager.getOrLoadModel(newModelName)
            if (model == null) {
                Log.e(TAG, "Failed to load model during switch: $newModelName")
                withContext(Dispatchers.Main) {
                    AssistantStateHolder.resetToOff("Failed to load model: $newModelName")
                    stopSelf()
                }
                return@launch
            }

            activeVoskModel = model
            activeModelName = newModelName
            val wakePhrase = preferencesManager.getWakePhrase()

            wakeWordDetector = VoskWakeWordDetector(model, wakePhrase)
            speechRecognizer = SpeechRecognizer(model)

            withContext(Dispatchers.Main) {
                startWakeWordListening()
            }
            Log.i(TAG, "Successfully switched active speech model to: $newModelName")
        }
    }

    private fun setupAudioRecorderAndStart() {
        audioRecorder?.stop()

        audioRecorder = AudioRecorder(
            onAudioChunk = { buffer, length ->
                val currentState = AssistantStateHolder.status.value.state
                when (currentState) {
                    ListeningState.LISTENING_FOR_WAKE_WORD -> {
                        wakeWordDetector?.feedAudio(buffer, length)
                    }
                    ListeningState.LISTENING_FOR_COMMAND -> {
                        if (length > 0) {
                            commandAudioChunks.add(buffer.copyOf(length))
                        }
                        speechRecognizer?.feedAudio(buffer, length)
                    }
                    else -> Unit
                }
            },
            onAmplitudeChanged = { amplitude ->
                AssistantStateHolder.updateStatus { it.copy(audioAmplitude = amplitude) }
                if (AssistantStateHolder.status.value.state == ListeningState.LISTENING_FOR_COMMAND) {
                    speechEndDetector?.onAudioAmplitude(amplitude)
                }
            }
        )

        val recordSuccess = audioRecorder?.start() ?: false
        if (!recordSuccess) {
            Log.e(TAG, "Failed to initialize and start AudioRecord")
            AssistantStateHolder.resetToOff("Could not access microphone hardware")
            stopSelf()
            return
        }

        startWakeWordListening()
    }

    private fun startWakeWordListening() {
        AssistantStateHolder.setState(ListeningState.LISTENING_FOR_WAKE_WORD)
        val currentPhrase = preferencesManager.getWakePhrase()
        updateListeningNotification("Aira is listening", "Say '$currentPhrase' to activate.")

        AssistantStateHolder.updateStatus {
            it.copy(
                isPopupVisible = false,
                isSpeaking = false,
                lastRecognizedText = ""
            )
        }
        audioRecorder?.resume()

        wakeWordDetector?.start {
            Log.i(TAG, "Wake word triggered! Moving to command listening flow")
            onWakeWordDetected()
        }
    }

    private fun onWakeWordDetected() {
        resetInactivityTimer()
        wakeWordDetector?.stop()

        AssistantStateHolder.setState(ListeningState.WAKE_WORD_DETECTED)
        updateListeningNotification("Aira", "Listening for your command...")

        AssistantStateHolder.updateStatus {
            it.copy(
                lastRecognizedText = "",
                lastAssistantResponse = "",
                lastCommandStatus = "",
                isSpeaking = false,
                isPopupVisible = true
            )
        }

        // Show the Siri-style floating orb popup over other apps or in-app
        voicePopupManager.show()

        // Start command recognition with audio-reactive animations and speech-end silence detection
        startCommandRecognition()
    }

    private fun startCommandRecognition() {
        AssistantStateHolder.setState(ListeningState.LISTENING_FOR_COMMAND)
        commandAudioChunks.clear()
        audioRecorder?.resume()

        speechEndDetector?.stop()
        speechEndDetector = SpeechEndDetector(
            silenceDurationMs = 1400L,
            minSpeechDurationMs = 350L,
            initialTimeoutMs = 4500L,
            maxCommandDurationMs = 8000L,
            onSpeechStart = {
                Log.d(TAG, "Speech started by user")
                AssistantStateHolder.updateStatus { it.copy(isSpeaking = true) }
            },
            onSpeakingStateChanged = { isSpeaking ->
                AssistantStateHolder.updateStatus { it.copy(isSpeaking = isSpeaking) }
            },
            onSpeechEnd = {
                Log.d(TAG, "Speech end detected by silence! Finalizing recognition.")
                AssistantStateHolder.setState(ListeningState.PROCESSING_COMMAND)
                speechRecognizer?.finalizeRecognition()
            },
            onTimeout = { hasSpoken ->
                Log.d(TAG, "Speech timeout (hasSpoken=$hasSpoken)")
                if (!hasSpoken) {
                    serviceScope.launch(Dispatchers.Main) {
                        onNoSpeechDetected()
                    }
                } else {
                    AssistantStateHolder.setState(ListeningState.PROCESSING_COMMAND)
                    speechRecognizer?.finalizeRecognition()
                }
            }
        ).apply { start() }

        speechRecognizer?.startListening(
            onPartialResult = { partial ->
                speechEndDetector?.onVoskPartialText(partial)
                AssistantStateHolder.updateStatus {
                    it.copy(lastRecognizedText = partial)
                }
                updateListeningNotification("Listening...", "\"$partial\"")
            },
            onFinalResult = { finalCommand ->
                serviceScope.launch(Dispatchers.Main) {
                    onCommandRecognized(finalCommand)
                }
            },
            onError = { error ->
                Log.e(TAG, "Speech recognition error: $error")
                serviceScope.launch(Dispatchers.Main) {
                    onSpeechError(error)
                }
            }
        )
    }

    private fun onNoSpeechDetected() {
        resetInactivityTimer()
        speechEndDetector?.stop()
        speechRecognizer?.stopListening()
        audioRecorder?.pause()
        voicePopupManager.hide()

        AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
        val response = "Sorry, I didn't catch that. Try again."
        AssistantStateHolder.updateStatus {
            it.copy(lastAssistantResponse = response)
        }
        updateListeningNotification("Aira", response)

        ttsManager.speak(response) {
            serviceScope.launch(Dispatchers.Main) {
                startWakeWordListening()
            }
        }
    }

    private fun onSpeechError(error: String) {
        Log.e(TAG, "Speech recognition error: $error")
        resetInactivityTimer()
        speechEndDetector?.stop()
        speechRecognizer?.stopListening()
        audioRecorder?.pause()
        voicePopupManager.hide()

        AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
        val errorMsg = "Sorry, I had trouble hearing that. Try again."
        AssistantStateHolder.updateStatus {
            it.copy(lastAssistantResponse = errorMsg)
        }
        updateListeningNotification("Aira", errorMsg)

        ttsManager.speak(errorMsg) {
            serviceScope.launch(Dispatchers.Main) {
                startWakeWordListening()
            }
        }
    }

    private fun onCommandRecognized(commandText: String) {
        resetInactivityTimer()
        speechEndDetector?.stop()
        speechRecognizer?.stopListening()
        audioRecorder?.pause()

        // Smoothly dismiss the voice popup
        voicePopupManager.hide()

        if (commandText.isBlank()) {
            AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
            val msg = "Sorry, I didn't catch that. Try again."
            AssistantStateHolder.updateStatus {
                it.copy(lastAssistantResponse = msg)
            }
            updateListeningNotification("Aira", msg)
            ttsManager.speak(msg) {
                serviceScope.launch(Dispatchers.Main) {
                    startWakeWordListening()
                }
            }
            return
        }

        // Biometric Speaker Verification: Ensure command is spoken by the enrolled owner
        val chunksCopy: List<ByteArray> = synchronized(commandAudioChunks) {
            commandAudioChunks.toList().also { commandAudioChunks.clear() }
        }
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

        if (preferencesManager.isVoiceSecurityEnabled() && speakerVerificationManager.isOwnerEnrolled()) {
            speakerVerificationManager.threshold = preferencesManager.getVoiceMatchThreshold()
            val verification = speakerVerificationManager.verify(floatSamples)

            if (!verification.isOwner) {
                val simPercent = (verification.similarity.coerceAtLeast(0f) * 100).toInt()
                Log.w(TAG, "Speaker verification failed for '$commandText': ${verification.reason} (similarity: ${verification.similarity}, threshold: ${verification.threshold})")
                AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
                val rejectedResponse = "Access denied. I only listen to my owner."
                AssistantStateHolder.updateStatus {
                    it.copy(
                        lastAssistantResponse = rejectedResponse,
                        lastCommandStatus = "Access Denied: Voice Not Recognized ($simPercent%)"
                    )
                }
                updateListeningNotification("Aira Security", rejectedResponse)
                ttsManager.speak(rejectedResponse) {
                    serviceScope.launch(Dispatchers.Main) {
                        startWakeWordListening()
                    }
                }
                return
            } else {
                Log.i(TAG, "Speaker verification passed! Similarity: ${verification.similarity} >= ${verification.threshold}")
            }
        }

        AssistantStateHolder.setState(ListeningState.PROCESSING_COMMAND)
        AssistantStateHolder.updateStatus {
            it.copy(lastRecognizedText = commandText)
        }
        updateListeningNotification("Processing...", "\"$commandText\"")

        // Execute command via CommandProcessor
        val result = commandProcessor.processCommand(commandText)

        // Record interaction in history
        val historyItem = AssistantHistoryItem(
            userCommand = commandText,
            assistantResponse = result.spokenResponse,
            isSuccess = result.isSuccess,
            actionType = result.actionType
        )
        historyRepository.addHistoryItem(historyItem)

        AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
        AssistantStateHolder.updateStatus {
            it.copy(
                lastAssistantResponse = result.spokenResponse,
                lastCommandStatus = result.displayText
            )
        }
        updateListeningNotification("Aira", result.spokenResponse)

        ttsManager.speak(result.spokenResponse) {
            serviceScope.launch(Dispatchers.Main) {
                if (result.shouldStopListening) {
                    Log.d(TAG, "Command requested stopping listening. Stopping service.")
                    stopSelf()
                } else {
                    startWakeWordListening()
                }
            }
        }
    }

    private fun resetInactivityTimer() {
        inactivityTimerJob?.cancel()
        timeoutDurationSeconds = preferencesManager.getTimeoutMinutes() * 60L
        timeoutSecondsRemaining = timeoutDurationSeconds

        AssistantStateHolder.updateStatus {
            it.copy(timeoutRemainingSeconds = timeoutSecondsRemaining)
        }

        inactivityTimerJob = serviceScope.launch {
            while (isActive && timeoutSecondsRemaining > 0) {
                delay(1000L)
                timeoutSecondsRemaining--
                AssistantStateHolder.updateStatus {
                    it.copy(timeoutRemainingSeconds = timeoutSecondsRemaining)
                }
            }

            if (timeoutSecondsRemaining <= 0) {
                Log.i(TAG, "10-minute inactivity timeout reached! Automatically stopping assistant service.")
                withContext(Dispatchers.Main) {
                    AssistantStateHolder.resetToOff()
                    stopSelf()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: Activity closed/swiped, assistant foreground service continues running.")
    }

    override fun onDestroy() {
        Log.d(TAG, "AssistantService onDestroy")
        inactivityTimerJob?.cancel()
        serviceScope.cancel()

        speechEndDetector?.stop()
        speechEndDetector = null

        audioRecorder?.stop()
        audioRecorder = null

        wakeWordDetector?.release()
        wakeWordDetector = null

        speechRecognizer?.release()
        speechRecognizer = null

        modelManager.releaseModel()
        activeVoskModel = null

        voicePopupManager.release()
        ttsManager.shutdown()

        AssistantStateHolder.resetToOff()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
