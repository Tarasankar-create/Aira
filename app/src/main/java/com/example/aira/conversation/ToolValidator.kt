package com.example.aira.conversation

import com.example.aira.commands.CommandIntent

object ToolValidator {

    private val ALLOWED_TOOLS = setOf(
        "open_app",
        "set_flashlight",
        "check_battery",
        "open_wifi_settings",
        "open_bluetooth_settings",
        "open_location_settings",
        "change_volume",
        "call_contact",
        "open_dialer",
        "open_website",
        "tell_time",
        "tell_date",
        "stop_listening"
    )

    fun isToolAllowed(toolName: String): Boolean = ALLOWED_TOOLS.contains(toolName.lowercase())

    fun validateAndMap(toolRequest: ToolRequest): CommandIntent? {
        val name = toolRequest.toolName.lowercase()
        val params = toolRequest.parameters

        return when (name) {
            "open_app" -> {
                val app = params["app_name"] ?: params["target"] ?: return null
                if (app.isBlank() || app.length > 64) return null
                CommandIntent.OpenApp(app.trim())
            }

            "set_flashlight" -> {
                val state = params["enabled"] ?: params["state"] ?: "true"
                val enable = state.equals("true", ignoreCase = true) || state.equals("on", ignoreCase = true)
                CommandIntent.SetFlashlight(enable)
            }

            "check_battery" -> CommandIntent.CheckBattery
            "open_wifi_settings" -> CommandIntent.OpenWifiSettings
            "open_bluetooth_settings" -> CommandIntent.OpenBluetoothSettings
            "open_location_settings" -> CommandIntent.OpenLocationSettings

            "change_volume" -> {
                val action = params["action"]?.lowercase() ?: "up"
                val volumeAction = when (action) {
                    "down" -> CommandIntent.VolumeAction.DOWN
                    "mute" -> CommandIntent.VolumeAction.MUTE
                    else -> CommandIntent.VolumeAction.UP
                }
                CommandIntent.ChangeVolume(volumeAction)
            }

            "call_contact" -> {
                val contact = params["contact_name"] ?: params["name"] ?: return null
                if (contact.isBlank() || contact.length > 80) return null
                CommandIntent.CallContact(contact.trim())
            }

            "open_dialer" -> {
                val number = params["phone_number"] ?: params["number"]
                CommandIntent.OpenDialer(number?.trim())
            }

            "open_website" -> {
                val url = params["url"] ?: return null
                if (!url.startsWith("http://") && !url.startsWith("https://")) return null
                CommandIntent.OpenWebsite(url.trim())
            }

            "tell_time" -> CommandIntent.TellTime
            "tell_date" -> CommandIntent.TellDate
            "stop_listening" -> CommandIntent.StopListening

            else -> null
        }
    }
}
