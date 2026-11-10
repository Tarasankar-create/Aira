package com.example.aira.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.aira.data.preferences.PreferencesManager
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class TextToSpeechManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) {
    private var tts: TextToSpeech? = null
    private val isInitialized = AtomicBoolean(false)
    val isSpeaking = AtomicBoolean(false)

    private val utteranceCallbacks = mutableMapOf<String, () -> Unit>()

    companion object {
        private const val TAG = "AiraTTS"
    }

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale.US
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Locale US is missing data or not supported for TTS. Falling back to default.")
                    tts?.language = Locale.getDefault()
                }

                tts?.setSpeechRate(preferencesManager.getSpeechRate())

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking.set(true)
                        Log.d(TAG, "TTS started speaking: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking.set(false)
                        Log.d(TAG, "TTS finished speaking: $utteranceId")
                        utteranceId?.let { id ->
                            utteranceCallbacks.remove(id)?.invoke()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking.set(false)
                        Log.e(TAG, "TTS error on utterance: $utteranceId")
                        utteranceId?.let { id ->
                            utteranceCallbacks.remove(id)?.invoke()
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        isSpeaking.set(false)
                        Log.e(TAG, "TTS error on utterance: $utteranceId, code: $errorCode")
                        utteranceId?.let { id ->
                            utteranceCallbacks.remove(id)?.invoke()
                        }
                    }
                })

                isInitialized.set(true)
                Log.d(TAG, "TextToSpeech initialized successfully")
                onInitComplete?.invoke(true)
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: $status")
                isInitialized.set(false)
                onInitComplete?.invoke(false)
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!preferencesManager.isVoiceResponseEnabled()) {
            Log.d(TAG, "Voice response disabled in preferences; skipping speech.")
            onDone?.invoke()
            return
        }

        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        if (!isInitialized.get() || tts == null) {
            Log.w(TAG, "TextToSpeech is not initialized yet. Skipping speech.")
            onDone?.invoke()
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        if (onDone != null) {
            utteranceCallbacks[utteranceId] = onDone
        }

        try {
            tts?.setSpeechRate(preferencesManager.getSpeechRate())
            isSpeaking.set(true)
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "TTS speak failed with code: $result")
                isSpeaking.set(false)
                utteranceCallbacks.remove(utteranceId)?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during TTS speak", e)
            isSpeaking.set(false)
            utteranceCallbacks.remove(utteranceId)?.invoke()
        }
    }

    fun stop() {
        if (isSpeaking.get()) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping TTS", e)
            }
            isSpeaking.set(false)
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        }
        tts = null
        isInitialized.set(false)
    }
}
