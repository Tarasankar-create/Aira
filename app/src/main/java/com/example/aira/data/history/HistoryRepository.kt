package com.example.aira.data.history

import com.example.aira.data.model.AssistantHistoryItem
import com.example.aira.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryRepository(private val preferencesManager: PreferencesManager) {

    private val _historyFlow = MutableStateFlow<List<AssistantHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<AssistantHistoryItem>> = _historyFlow.asStateFlow()

    init {
        _historyFlow.value = preferencesManager.loadHistory()
    }

    fun addHistoryItem(item: AssistantHistoryItem) {
        val updated = listOf(item) + _historyFlow.value
        _historyFlow.value = updated
        preferencesManager.saveHistory(updated)
    }

    fun clearHistory() {
        _historyFlow.value = emptyList()
        preferencesManager.clearHistory()
    }
}
