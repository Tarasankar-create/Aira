package com.example.aira.commands

import android.content.Context
import android.util.Log

class CommandProcessor(
    private val context: Context,
    private val appLauncher: AppLauncher = AppLauncher(context),
    private val phoneActions: PhoneActions = PhoneActions(context),
    private val registry: CommandRegistry = CommandRegistry()
) {
    companion object {
        private const val TAG = "AiraCommandProcessor"
    }

    fun processCommand(rawText: String): CommandResult {
        Log.d(TAG, "Processing command input: '$rawText'")
        val intent = registry.parse(rawText)

        return when (intent) {
            is CommandIntent.OpenApp -> {
                appLauncher.launchAppByName(intent.targetAppName)
            }
            is CommandIntent.SetFlashlight -> {
                phoneActions.setFlashlight(intent.enable)
            }
            is CommandIntent.CheckBattery -> {
                phoneActions.checkBattery()
            }
            is CommandIntent.OpenWifiSettings -> {
                phoneActions.openWifiSettings()
            }
            is CommandIntent.OpenBluetoothSettings -> {
                phoneActions.openBluetoothSettings()
            }
            is CommandIntent.OpenMobileNetworkSettings -> {
                phoneActions.openMobileNetworkSettings()
            }
            is CommandIntent.OpenLocationSettings -> {
                phoneActions.openLocationSettings()
            }
            is CommandIntent.ChangeVolume -> {
                phoneActions.changeVolume(intent.action)
            }
            is CommandIntent.OpenDialer -> {
                phoneActions.openDialer(intent.phoneNumber)
            }
            is CommandIntent.CallContact -> {
                phoneActions.callContact(intent.contactName)
            }
            is CommandIntent.OpenWebsite -> {
                phoneActions.openWebsite(intent.url)
            }
            is CommandIntent.TellTime -> {
                phoneActions.getTime()
            }
            is CommandIntent.TellDate -> {
                phoneActions.getDate()
            }
            is CommandIntent.StopListening -> {
                CommandResult(
                    isSuccess = true,
                    spokenResponse = "Listening is turned off.",
                    displayText = "Stopped listening.",
                    shouldStopListening = true,
                    actionType = "STOP_LISTENING"
                )
            }
            is CommandIntent.Unknown -> {
                val query = intent.rawQuery.trim()
                if (query.isBlank()) {
                    CommandResult(
                        isSuccess = false,
                        spokenResponse = "I didn't catch that.",
                        displayText = "No speech detected",
                        actionType = "UNKNOWN"
                    )
                } else {
                    CommandResult(
                        isSuccess = false,
                        spokenResponse = "I'm sorry, I don't know how to $query yet.",
                        displayText = "Unknown command: $query",
                        actionType = "UNKNOWN"
                    )
                }
            }
        }
    }
}
