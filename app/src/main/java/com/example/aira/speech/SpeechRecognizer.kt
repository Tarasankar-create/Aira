package com.example.aira.speech

import android.util.Log
import com.google.gson.JsonParser
import org.vosk.Model
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean

class SpeechRecognizer(
    private val model: Model
) {
    private var recognizer: Recognizer? = null
    private val isListening = AtomicBoolean(false)

    private var onPartialCallback: ((String) -> Unit)? = null
    private var onFinalCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    private var speechStartTime = 0L
    private var lastSpeechDetectedTime = 0L
    private var hasHeardAnySpeech = false

    companion object {
        private const val TAG = "AiraSpeechRecognizer"
        private const val MAX_COMMAND_DURATION_MS = 6000L
        private const val SILENCE_AFTER_SPEECH_MS = 1400L
        private const val NO_INPUT_TIMEOUT_MS = 4000L
    }

    fun startListening(
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isListening.get()) {
            stopListening()
        }

        this.onPartialCallback = onPartialResult
        this.onFinalCallback = onFinalResult
        this.onErrorCallback = onError

        try {
            recognizer = Recognizer(model, 16000.0f)
            isListening.set(true)
            speechStartTime = System.currentTimeMillis()
            lastSpeechDetectedTime = speechStartTime
            hasHeardAnySpeech = false
            Log.d(TAG, "SpeechRecognizer started listening for command")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Recognizer", e)
            onError("Failed to start speech recognition: ${e.localizedMessage}")
        }
    }

    fun feedAudio(buffer: ByteArray, length: Int) {
        if (!isListening.get() || recognizer == null) return

        val now = System.currentTimeMillis()

        try {
            val rec = recognizer ?: return
            val completedPhrase = rec.acceptWaveForm(buffer, length)

            if (completedPhrase) {
                val resultText = parseText(rec.result)
                if (resultText.isNotBlank()) {
                    Log.i(TAG, "Final phrase completed: $resultText")
                    finishWithResult(resultText)
                    return
                }
            } else {
                val partialText = parseText(rec.partialResult)
                if (partialText.isNotBlank()) {
                    hasHeardAnySpeech = true
                    lastSpeechDetectedTime = now
                    onPartialCallback?.invoke(partialText)
                }
            }

            // Check silence timeout
            if (hasHeardAnySpeech && (now - lastSpeechDetectedTime > SILENCE_AFTER_SPEECH_MS)) {
                Log.d(TAG, "Silence detected after speech, finalizing command")
                val finalResult = parseText(rec.finalResult)
                finishWithResult(finalResult)
                return
            }

            // Check no input timeout
            if (!hasHeardAnySpeech && (now - speechStartTime > NO_INPUT_TIMEOUT_MS)) {
                Log.d(TAG, "No speech detected within timeout")
                finishWithResult("")
                return
            }

            // Max overall duration guard
            if (now - speechStartTime > MAX_COMMAND_DURATION_MS) {
                Log.d(TAG, "Max command duration exceeded")
                val finalResult = parseText(rec.finalResult)
                finishWithResult(finalResult)
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in speech recognition feedAudio", e)
            onErrorCallback?.invoke(e.localizedMessage ?: "Unknown speech error")
        }
    }

    private fun finishWithResult(resultText: String) {
        if (!isListening.getAndSet(false)) return
        Log.d(TAG, "Delivering final command result: '$resultText'")
        onFinalCallback?.invoke(resultText.trim())
    }

    private fun parseText(json: String): String {
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            when {
                jsonObject.has("text") -> jsonObject.get("text").asString
                jsonObject.has("partial") -> jsonObject.get("partial").asString
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun stopListening() {
        if (isListening.getAndSet(false)) {
            try {
                recognizer?.reset()
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting recognizer", e)
            }
        }
    }

    fun release() {
        stopListening()
        recognizer?.close()
        recognizer = null
        onPartialCallback = null
        onFinalCallback = null
        onErrorCallback = null
    }

    fun isListening(): Boolean = isListening.get()
}
