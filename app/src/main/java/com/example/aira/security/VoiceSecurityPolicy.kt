package com.example.aira.security

import com.example.aira.commands.CommandIntent

/**
 * Three-tier voice security policy for Aira commands:
 * - PUBLIC: May execute without owner voice verification.
 * - OWNER_PROTECTED: Requires offline owner voice verification (e.g. reading contacts/private info).
 * - SENSITIVE: Requires owner voice verification and explicit confirmation.
 */
enum class SecurityLevel {
    PUBLIC,
    OWNER_PROTECTED,
    SENSITIVE
}

object VoiceSecurityPolicy {

    fun getRequiredSecurityLevel(intent: CommandIntent): SecurityLevel {
        return when (intent) {
            is CommandIntent.SetFlashlight,
            is CommandIntent.CheckBattery,
            is CommandIntent.TellTime,
            is CommandIntent.TellDate,
            is CommandIntent.ChangeVolume,
            is CommandIntent.OpenWifiSettings,
            is CommandIntent.OpenBluetoothSettings,
            is CommandIntent.OpenMobileNetworkSettings,
            is CommandIntent.OpenLocationSettings,
            is CommandIntent.OpenWebsite,
            is CommandIntent.StopListening,
            is CommandIntent.Unknown -> SecurityLevel.PUBLIC

            is CommandIntent.OpenApp -> {
                // Public apps (entertainment, search) vs sensitive apps
                val target = intent.targetAppName.lowercase()
                if (target.contains("bank") || target.contains("pay") || target.contains("wallet")) {
                    SecurityLevel.SENSITIVE
                } else {
                    SecurityLevel.PUBLIC
                }
            }

            is CommandIntent.CallContact -> {
                // Reading private contact info & placing calls requires owner voice verification
                SecurityLevel.OWNER_PROTECTED
            }

            is CommandIntent.OpenDialer -> {
                if (intent.phoneNumber.isNullOrBlank()) {
                    SecurityLevel.PUBLIC
                } else {
                    SecurityLevel.OWNER_PROTECTED
                }
            }
        }
    }
}
