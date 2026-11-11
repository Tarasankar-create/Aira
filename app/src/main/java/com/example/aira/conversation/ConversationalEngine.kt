package com.example.aira.conversation

import android.content.Context
import android.util.Log

interface ConversationalEngine {
    val isModelLoaded: Boolean
    suspend fun initialize(): Boolean
    suspend fun generateResponse(
        history: List<ConversationMessage>,
        userPrompt: String
    ): AssistantResponse
    fun release()
}

/**
 * Clean offline conversational engine implementation.
 * Decoupled from Android system actions: generates textual responses and typed [ToolRequest]s
 * which must pass through [ToolValidator] before execution.
 */
class RuleBasedConversationalEngine(private val context: Context) : ConversationalEngine {

    companion object {
        private const val TAG = "RuleBasedConvEngine"
    }

    private var initialized = false

    override val isModelLoaded: Boolean
        get() = initialized

    override suspend fun initialize(): Boolean {
        initialized = true
        Log.i(TAG, "Initialized ConversationalEngine")
        return true
    }

    override suspend fun generateResponse(
        history: List<ConversationMessage>,
        userPrompt: String
    ): AssistantResponse {
        val query = userPrompt.trim().lowercase()

        // Match conversational greetings
        if (query.matches(Regex("^(hi|hello|hey|namaste|kem cho|good morning|good evening|good afternoon).*"))) {
            return AssistantResponse(
                text = "Hello! I am Aira, your private offline assistant. How can I help you today?"
            )
        }

        if (query.contains("who are you") || query.contains("what can you do")) {
            return AssistantResponse(
                text = "I am Aira, an offline voice assistant. I can open apps, make calls, toggle the flashlight, check battery, and tell you the time and date in English, Hindi, and Odia without internet."
            )
        }

        // Check for device control intent requests
        if (query.contains("flashlight") || query.contains("torch")) {
            val enable = !query.contains("off") && !query.contains("stop")
            return AssistantResponse(
                text = if (enable) "Turning on the flashlight." else "Turning off the flashlight.",
                toolRequests = listOf(
                    ToolRequest(
                        toolName = "set_flashlight",
                        parameters = mapOf("enabled" to enable.toString())
                    )
                )
            )
        }

        if (query.contains("battery") || query.contains("charge")) {
            return AssistantResponse(
                text = "Checking battery level.",
                toolRequests = listOf(ToolRequest(toolName = "check_battery"))
            )
        }

        if (query.contains("time") || query.contains("what time")) {
            return AssistantResponse(
                text = "Checking current time.",
                toolRequests = listOf(ToolRequest(toolName = "tell_time"))
            )
        }

        if (query.contains("date") || query.contains("what date") || query.contains("today")) {
            return AssistantResponse(
                text = "Checking today's date.",
                toolRequests = listOf(ToolRequest(toolName = "tell_date"))
            )
        }

        // Default response
        return AssistantResponse(
            text = "I heard: \"$userPrompt\". You can ask me to open apps, toggle flashlight, or check battery."
        )
    }

    override fun release() {
        initialized = false
    }
}
