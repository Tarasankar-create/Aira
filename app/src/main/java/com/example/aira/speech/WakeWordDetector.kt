package com.example.aira.speech

import android.util.Log
import com.google.gson.JsonParser
import org.vosk.Model
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean

interface WakeWordDetector {
    fun start(onWakeWordDetected: () -> Unit)
    fun feedAudio(buffer: ByteArray, length: Int)
    fun stop()
    fun release()
    fun isRunning(): Boolean
}

class VoskWakeWordDetector(
    private val model: Model,
    wakePhrase: String = "hey assistant"
) : WakeWordDetector {

    private var activeWakePhrase: String = wakePhrase
    private var recognizer: Recognizer? = null
    private var onDetectedCallback: (() -> Unit)? = null
    private val isRunning = AtomicBoolean(false)
    private var lastDetectionTimestamp = 0L

    companion object {
        private const val TAG = "AiraWakeWordDetector"
        private const val DEBOUNCE_INTERVAL_MS = 2000L
    }

    fun updateWakePhrase(newPhrase: String) {
        val trimmed = newPhrase.trim()
        if (trimmed.isBlank() || trimmed.equals(activeWakePhrase, ignoreCase = true)) return
        Log.d(TAG, "Updating wake phrase from '$activeWakePhrase' to '$trimmed'")
        activeWakePhrase = trimmed
        val wasRunning = isRunning.get()
        val callback = onDetectedCallback
        if (wasRunning) {
            stop()
            recognizer?.close()
            recognizer = null
            if (callback != null) {
                start(callback)
            }
        }
    }

    override fun start(onWakeWordDetected: () -> Unit) {
        if (isRunning.get()) {
            stop()
        }

        this.onDetectedCallback = onWakeWordDetected

        try {
            // Constrain Vosk recognition grammar strictly to the wake phrase and unknown words
            // This turns the acoustic decoder into an ultra-low-latency keyword spotter
            val normalizedPhrase = activeWakePhrase.lowercase().trim()
            val grammarJson = """["$normalizedPhrase", "[unk]"]"""

            recognizer = Recognizer(model, 16000.0f, grammarJson).apply {
                setMaxAlternatives(0)
                setWords(false)
            }
            isRunning.set(true)
            Log.d(TAG, "WakeWordDetector started with keyword grammar: $grammarJson")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Vosk wake-word recognizer", e)
            isRunning.set(false)
        }
    }

    override fun feedAudio(buffer: ByteArray, length: Int) {
        if (!isRunning.get() || recognizer == null) return

        try {
            val rec = recognizer ?: return
            val hasResult = rec.acceptWaveForm(buffer, length)

            if (hasResult) {
                val resultJson = rec.result
                checkAndTrigger(resultJson)
            } else {
                val partialJson = rec.partialResult
                checkAndTrigger(partialJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio in WakeWordDetector", e)
        }
    }

    private fun checkAndTrigger(jsonStr: String) {
        val now = System.currentTimeMillis()
        if (now - lastDetectionTimestamp < DEBOUNCE_INTERVAL_MS) {
            return
        }

        val text = parseRecognizedText(jsonStr)
        if (text.isBlank()) return

        val normalizedWake = activeWakePhrase.lowercase().trim()
        if (text.contains(normalizedWake)) {
            Log.i(TAG, "Wake word detected! ('$normalizedWake' in '$text')")
            lastDetectionTimestamp = now
            recognizer?.reset()
            onDetectedCallback?.invoke()
        }
    }

    private fun parseRecognizedText(json: String): String {
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            when {
                jsonObject.has("text") -> jsonObject.get("text").asString.lowercase().trim()
                jsonObject.has("partial") -> jsonObject.get("partial").asString.lowercase().trim()
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    override fun stop() {
        isRunning.set(false)
        recognizer?.reset()
        Log.d(TAG, "WakeWordDetector stopped")
    }

    override fun release() {
        stop()
        recognizer?.close()
        recognizer = null
        onDetectedCallback = null
        Log.d(TAG, "WakeWordDetector released")
    }

    override fun isRunning(): Boolean = isRunning.get()
}
