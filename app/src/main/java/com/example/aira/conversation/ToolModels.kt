package com.example.aira.conversation

import java.util.UUID

data class ToolRequest(
    val toolName: String,
    val parameters: Map<String, String> = emptyMap(),
    val requestId: String = UUID.randomUUID().toString()
)

data class ToolResult(
    val requestId: String,
    val success: Boolean,
    val output: String,
    val error: String? = null
)

data class AssistantResponse(
    val text: String,
    val toolRequests: List<ToolRequest> = emptyList()
)
