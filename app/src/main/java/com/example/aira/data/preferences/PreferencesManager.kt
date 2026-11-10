package com.example.aira.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.aira.data.model.AssistantHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _timeoutMinutesFlow = MutableStateFlow(getTimeoutMinutes())
    val timeoutMinutesFlow: StateFlow<Int> = _timeoutMinutesFlow.asStateFlow()

    private val _voiceResponseEnabledFlow = MutableStateFlow(isVoiceResponseEnabled())
    val voiceResponseEnabledFlow: StateFlow<Boolean> = _voiceResponseEnabledFlow.asStateFlow()

    private val _speechRateFlow = MutableStateFlow(getSpeechRate())
    val speechRateFlow: StateFlow<Float> = _speechRateFlow.asStateFlow()

    private val _selectedModelFlow = MutableStateFlow(getSelectedModelName())
    val selectedModelFlow: StateFlow<String> = _selectedModelFlow.asStateFlow()

    private val _wakePhraseFlow = MutableStateFlow(getWakePhrase())
    val wakePhraseFlow: StateFlow<String> = _wakePhraseFlow.asStateFlow()

    fun getTimeoutMinutes(): Int = prefs.getInt(KEY_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES)

    fun setTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_TIMEOUT_MINUTES, minutes).apply()
        _timeoutMinutesFlow.value = minutes
    }

    fun isVoiceResponseEnabled(): Boolean = prefs.getBoolean(KEY_VOICE_RESPONSE_ENABLED, true)

    fun setVoiceResponseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_RESPONSE_ENABLED, enabled).apply()
        _voiceResponseEnabledFlow.value = enabled
    }

    fun getSpeechRate(): Float = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)

    fun setSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, rate).apply()
        _speechRateFlow.value = rate
    }

    fun getSelectedModelName(): String = prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL_NAME) ?: DEFAULT_MODEL_NAME

    fun setSelectedModelName(modelName: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelName).apply()
        _selectedModelFlow.value = modelName
    }

    fun getWakePhrase(): String = prefs.getString(KEY_WAKE_PHRASE, DEFAULT_WAKE_PHRASE) ?: DEFAULT_WAKE_PHRASE

    fun setWakePhrase(phrase: String) {
        prefs.edit().putString(KEY_WAKE_PHRASE, phrase).apply()
        _wakePhraseFlow.value = phrase
    }

    fun loadHistory(): List<AssistantHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AssistantHistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHistory(history: List<AssistantHistoryItem>) {
        val json = gson.toJson(history.take(MAX_HISTORY_ITEMS))
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val PREFS_NAME = "aira_preferences"
        private const val KEY_TIMEOUT_MINUTES = "inactivity_timeout_minutes"
        private const val KEY_VOICE_RESPONSE_ENABLED = "voice_response_enabled"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_SELECTED_MODEL = "selected_speech_model"
        private const val KEY_WAKE_PHRASE = "wake_phrase"
        private const val KEY_HISTORY = "conversation_history"

        const val DEFAULT_TIMEOUT_MINUTES = 10
        const val DEFAULT_MODEL_NAME = "vosk-model-small-en-us-0.15"
        const val DEFAULT_WAKE_PHRASE = "Hey Assistant"
        private const val MAX_HISTORY_ITEMS = 100
    }
}
