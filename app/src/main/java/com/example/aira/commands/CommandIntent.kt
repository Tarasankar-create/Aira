package com.example.aira.commands

sealed class CommandIntent {
    data class OpenApp(val targetAppName: String) : CommandIntent()
    data class SetFlashlight(val enable: Boolean) : CommandIntent()
    object CheckBattery : CommandIntent()
    object OpenWifiSettings : CommandIntent()
    object OpenBluetoothSettings : CommandIntent()
    object OpenMobileNetworkSettings : CommandIntent()
    object OpenLocationSettings : CommandIntent()
    enum class VolumeAction { UP, DOWN, MUTE }
    data class ChangeVolume(val action: VolumeAction) : CommandIntent()
    data class OpenDialer(val phoneNumber: String? = null) : CommandIntent()
    data class OpenWebsite(val url: String) : CommandIntent()
    object TellTime : CommandIntent()
    object TellDate : CommandIntent()
    object StopListening : CommandIntent()
    data class Unknown(val rawQuery: String) : CommandIntent()
}

data class CommandResult(
    val isSuccess: Boolean,
    val spokenResponse: String,
    val displayText: String,
    val shouldStopListening: Boolean = false,
    val actionType: String = "GENERAL"
)
