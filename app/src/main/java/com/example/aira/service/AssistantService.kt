package com.example.aira.service

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
import com.example.aira.speech.AudioRecorder
import com.example.aira.speech.SpeechModelManager
import com.example.aira.speech.SpeechRecognizer
import com.example.aira.speech.VoskWakeWordDetector
import com.example.aira.speech.WakeWordDetector
import com.example.aira.tts.TextToSpeechManager
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

    private var activeVoskModel: Model? = null
    private var activeModelName: String? = null
    private var wakeWordDetector: WakeWordDetector? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecorder: AudioRecorder? = null

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

        timeoutDurationSeconds = preferencesManager.getTimeoutMinutes() * 60L
        timeoutSecondsRemaining = timeoutDurationSeconds

        // Observe wake phrase changes dynamically
        serviceScope.launch {
            preferencesManager.wakePhraseFlow.collect { newPhrase ->
                Log.d(TAG, "Wake phrase changed in preferences: $newPhrase")
                (wakeWordDetector as? VoskWakeWordDetector)?.updateWakePhrase(newPhrase)
                val notification = NotificationHelper.buildListeningNotification(
                    this@AssistantService,
                    title = "Aira is listening",
                    content = "Say '$newPhrase' to activate."
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                manager?.notify(NotificationHelper.NOTIFICATION_ID, notification)
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
                // Route audio according to current state
                val currentState = AssistantStateHolder.status.value.state
                when (currentState) {
                    ListeningState.LISTENING_FOR_WAKE_WORD -> {
                        wakeWordDetector?.feedAudio(buffer, length)
                    }
                    ListeningState.LISTENING_FOR_COMMAND -> {
                        speechRecognizer?.feedAudio(buffer, length)
                    }
                    else -> {
                        // In other states (speaking response, etc.), audio is suppressed
                    }
                }
            },
            onAmplitudeChanged = { amplitude ->
                AssistantStateHolder.updateStatus { it.copy(audioAmplitude = amplitude) }
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
        audioRecorder?.resume()

        wakeWordDetector?.start {
            Log.i(TAG, "Wake word triggered! Moving to command listening flow")
            onWakeWordDetected()
        }
    }

    private fun onWakeWordDetected() {
        resetInactivityTimer()
        wakeWordDetector?.stop()
        audioRecorder?.pause()

        AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
        AssistantStateHolder.updateStatus {
            it.copy(
                lastAssistantResponse = "Yes, I'm listening."
            )
        }

        // Acknowledge wake word with speech response
        ttsManager.speak("Yes, I'm listening.") {
            serviceScope.launch(Dispatchers.Main) {
                startCommandRecognition()
            }
        }
    }

    private fun startCommandRecognition() {
        AssistantStateHolder.setState(ListeningState.LISTENING_FOR_COMMAND)
        audioRecorder?.resume()

        speechRecognizer?.startListening(
            onPartialResult = { partial ->
                AssistantStateHolder.updateStatus {
                    it.copy(lastRecognizedText = partial)
                }
            },
            onFinalResult = { finalCommand ->
                serviceScope.launch(Dispatchers.Main) {
                    onCommandRecognized(finalCommand)
                }
            },
            onError = { error ->
                Log.e(TAG, "Speech recognition error: $error")
                serviceScope.launch(Dispatchers.Main) {
                    startWakeWordListening()
                }
            }
        )
    }

    private fun onCommandRecognized(commandText: String) {
        resetInactivityTimer()
        speechRecognizer?.stopListening()
        audioRecorder?.pause()

        if (commandText.isBlank()) {
            AssistantStateHolder.setState(ListeningState.SPEAKING_RESPONSE)
            ttsManager.speak("I didn't hear a command.") {
                serviceScope.launch(Dispatchers.Main) {
                    startWakeWordListening()
                }
            }
            return
        }

        AssistantStateHolder.setState(ListeningState.PROCESSING_COMMAND)
        AssistantStateHolder.updateStatus {
            it.copy(lastRecognizedText = commandText)
        }

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
        // Foreground service continues running when user swipes app from Recent Apps
        Log.d(TAG, "onTaskRemoved: Activity closed/swiped, assistant foreground service continues running.")
    }

    override fun onDestroy() {
        Log.d(TAG, "AssistantService onDestroy")
        inactivityTimerJob?.cancel()
        serviceScope.cancel()

        wakeWordDetector?.release()
        wakeWordDetector = null

        speechRecognizer?.release()
        speechRecognizer = null

        audioRecorder?.stop()
        audioRecorder = null

        ttsManager.shutdown()
        modelManager.releaseModel()
        activeVoskModel = null
        activeModelName = null

        AssistantStateHolder.resetToOff()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
