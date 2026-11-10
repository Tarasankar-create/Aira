package com.example.aira.data.model

import java.util.UUID

data class AssistantHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val userCommand: String,
    val assistantResponse: String,
    val isSuccess: Boolean = true,
    val actionType: String = "GENERAL"
)
