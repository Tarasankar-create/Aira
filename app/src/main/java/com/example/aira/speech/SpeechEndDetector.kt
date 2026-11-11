package com.example.aira.speech

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Detects voice activity and end-of-speech silence based on real-time microphone amplitude
 * and Vosk speech recognition events.
 */
class SpeechEndDetector(
    private val silenceDurationMs: Long = 1400L,
    private val minSpeechDurationMs: Long = 350L,
    private val initialTimeoutMs: Long = 4500L,
    private val maxCommandDurationMs: Long = 8000L,
    private val voiceThresholdAmplitude: Float = 0.08f,
    private val onSpeechStart: () -> Unit = {},
    private val onSpeechEnd: () -> Unit,
    private val onTimeout: (hasSpoken: Boolean) -> Unit = {},
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) {

    companion object {
        private const val TAG = "AiraSpeechEndDetector"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isRunning = false
    private var hasSpoken = false
    private var isCurrentlySpeaking = false
    private var sessionStartTime = 0L
    private var speechStartTime = 0L
    private var lastVoiceTimestamp = 0L

    private val silenceCheckRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val now = System.currentTimeMillis()

            if (hasSpoken) {
                // User has spoken at least once in this session
                val silenceElapsed = now - lastVoiceTimestamp
                val speechElapsed = now - speechStartTime

                if (silenceElapsed >= silenceDurationMs && speechElapsed >= minSpeechDurationMs) {
                    Log.i(TAG, "Speech end detected: silence of ${silenceElapsed}ms after ${speechElapsed}ms speech")
                    notifySpeechEnd()
                    return
                }

                if (now - sessionStartTime >= maxCommandDurationMs) {
                    Log.i(TAG, "Max command duration reached (${maxCommandDurationMs}ms), concluding speech")
                    notifySpeechEnd()
                    return
                }
            } else {
                // User hasn't spoken yet
                if (now - sessionStartTime >= initialTimeoutMs) {
                    Log.i(TAG, "Initial silence timeout reached (${initialTimeoutMs}ms without speech)")
                    isRunning = false
                    mainHandler.post { onTimeout(false) }
                    return
                }
            }

            mainHandler.postDelayed(this, 100L)
        }
    }

    fun start() {
        isRunning = true
        hasSpoken = false
        isCurrentlySpeaking = false
        sessionStartTime = System.currentTimeMillis()
        speechStartTime = 0L
        lastVoiceTimestamp = sessionStartTime

        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed(silenceCheckRunnable, 100L)
        Log.d(TAG, "SpeechEndDetector started with silence=${silenceDurationMs}ms, timeout=${initialTimeoutMs}ms")
    }

    fun onAudioAmplitude(amplitude: Float) {
        if (!isRunning) return

        val now = System.currentTimeMillis()
        val isVoice = amplitude >= voiceThresholdAmplitude

        if (isVoice) {
            lastVoiceTimestamp = now
            if (!hasSpoken) {
                hasSpoken = true
                speechStartTime = now
                mainHandler.post { onSpeechStart() }
            }
            if (!isCurrentlySpeaking) {
                isCurrentlySpeaking = true
                mainHandler.post { onSpeakingStateChanged(true) }
            }
        } else {
            // Check if voice stopped
            if (isCurrentlySpeaking && (now - lastVoiceTimestamp > 250L)) {
                isCurrentlySpeaking = false
                mainHandler.post { onSpeakingStateChanged(false) }
            }
        }
    }

    fun onVoskPartialText(partialText: String) {
        if (!isRunning) return
        if (partialText.isNotBlank()) {
            val now = System.currentTimeMillis()
            lastVoiceTimestamp = now
            if (!hasSpoken) {
                hasSpoken = true
                speechStartTime = now
                mainHandler.post { onSpeechStart() }
            }
            if (!isCurrentlySpeaking) {
                isCurrentlySpeaking = true
                mainHandler.post { onSpeakingStateChanged(true) }
            }
        }
    }

    private fun notifySpeechEnd() {
        if (!isRunning) return
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            onSpeakingStateChanged(false)
            onSpeechEnd()
        }
    }

    fun stop() {
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        isCurrentlySpeaking = false
        Log.d(TAG, "SpeechEndDetector stopped")
    }
}
