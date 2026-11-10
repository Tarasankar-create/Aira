package com.example.aira.commands

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import com.example.aira.utils.PermissionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneActions(private val context: Context) {

    companion object {
        private const val TAG = "AiraPhoneActions"
    }

    fun setFlashlight(enable: Boolean): CommandResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            return CommandResult(
                isSuccess = false,
                spokenResponse = "Camera service is not available.",
                displayText = "CameraManager not available",
                actionType = "FLASHLIGHT"
            )
        }

        return try {
            val cameraIds = cameraManager.cameraIdList
            var torchId: String? = null
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    torchId = id
                    break
                }
            }

            if (torchId != null) {
                cameraManager.setTorchMode(torchId, enable)
                val statusText = if (enable) "Turning on flashlight." else "Turning off flashlight."
                CommandResult(
                    isSuccess = true,
                    spokenResponse = statusText,
                    displayText = if (enable) "Flashlight turned ON" else "Flashlight turned OFF",
                    actionType = "FLASHLIGHT"
                )
            } else {
                CommandResult(
                    isSuccess = false,
                    spokenResponse = "No flashlight hardware found on this device.",
                    displayText = "Flashlight hardware unavailable",
                    actionType = "FLASHLIGHT"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting flashlight state", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not control flashlight.",
                displayText = "Flashlight error: ${e.localizedMessage}",
                actionType = "FLASHLIGHT"
            )
        }
    }

    fun checkBattery(): CommandResult {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)

            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val chargingText = if (isCharging) " and charging" else ""
                val spoken = "Your battery is at $batteryPct percent$chargingText."
                CommandResult(
                    isSuccess = true,
                    spokenResponse = spoken,
                    displayText = "Battery: $batteryPct%$chargingText",
                    actionType = "BATTERY"
                )
            } else {
                CommandResult(
                    isSuccess = false,
                    spokenResponse = "Cannot read battery status.",
                    displayText = "Battery status intent null",
                    actionType = "BATTERY"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not check battery level.",
                displayText = "Error checking battery: ${e.localizedMessage}",
                actionType = "BATTERY"
            )
        }
    }

    fun openWifiSettings(): CommandResult = openSettingsIntent(
        Settings.ACTION_WIFI_SETTINGS,
        "Opening Wi-Fi settings.",
        "Opened Wi-Fi Settings",
        "WIFI"
    )

    fun openBluetoothSettings(): CommandResult = openSettingsIntent(
        Settings.ACTION_BLUETOOTH_SETTINGS,
        "Opening Bluetooth settings.",
        "Opened Bluetooth Settings",
        "BLUETOOTH"
    )

    fun openMobileNetworkSettings(): CommandResult = openSettingsIntent(
        Settings.ACTION_DATA_ROAMING_SETTINGS,
        "Opening mobile network settings.",
        "Opened Mobile Network Settings",
        "MOBILE_NETWORK"
    )

    fun openLocationSettings(): CommandResult = openSettingsIntent(
        Settings.ACTION_LOCATION_SOURCE_SETTINGS,
        "Opening location settings.",
        "Opened Location Settings",
        "LOCATION"
    )

    private fun openSettingsIntent(
        action: String,
        spoken: String,
        display: String,
        actionType: String
    ): CommandResult {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                isSuccess = true,
                spokenResponse = spoken,
                displayText = display,
                actionType = actionType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings: $action", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not open settings.",
                displayText = "Error opening $actionType settings: ${e.localizedMessage}",
                actionType = actionType
            )
        }
    }

    fun changeVolume(action: CommandIntent.VolumeAction): CommandResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            return CommandResult(
                isSuccess = false,
                spokenResponse = "Audio service is not available.",
                displayText = "AudioManager not available",
                actionType = "VOLUME"
            )
        }

        return try {
            when (action) {
                CommandIntent.VolumeAction.UP -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    CommandResult(
                        isSuccess = true,
                        spokenResponse = "Volume increased.",
                        displayText = "Increased media volume",
                        actionType = "VOLUME"
                    )
                }
                CommandIntent.VolumeAction.DOWN -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                    CommandResult(
                        isSuccess = true,
                        spokenResponse = "Volume decreased.",
                        displayText = "Decreased media volume",
                        actionType = "VOLUME"
                    )
                }
                CommandIntent.VolumeAction.MUTE -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        AudioManager.FLAG_SHOW_UI
                    )
                    CommandResult(
                        isSuccess = true,
                        spokenResponse = "Media muted.",
                        displayText = "Muted media volume",
                        actionType = "VOLUME"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting volume", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not adjust volume.",
                displayText = "Volume error: ${e.localizedMessage}",
                actionType = "VOLUME"
            )
        }
    }

    fun openDialer(phoneNumber: String?): CommandResult {
        return try {
            val uri = if (phoneNumber.isNullOrBlank()) {
                Uri.parse("tel:")
            } else {
                Uri.parse("tel:$phoneNumber")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val spoken = if (phoneNumber.isNullOrBlank()) "Opening dialer." else "Dialing $phoneNumber."
            CommandResult(
                isSuccess = true,
                spokenResponse = spoken,
                displayText = spoken,
                actionType = "DIALER"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening dialer", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not open dialer.",
                displayText = "Dialer error: ${e.localizedMessage}",
                actionType = "DIALER"
            )
        }
    }

    fun callContact(contactName: String): CommandResult {
        if (!PermissionManager.hasReadContactsPermission(context)) {
            return CommandResult(
                isSuccess = false,
                spokenResponse = "Contacts permission is required to search contacts. Please grant it in settings.",
                displayText = "Contacts permission required",
                actionType = "CALL_CONTACT"
            )
        }

        val cleanQuery = contactName.trim().lowercase(Locale.getDefault())
        if (cleanQuery.isBlank()) {
            return CommandResult(
                isSuccess = false,
                spokenResponse = "Who would you like to call?",
                displayText = "Contact name empty",
                actionType = "CALL_CONTACT"
            )
        }

        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            var exactMatch: Pair<String, String>? = null
            var partialMatch: Pair<String, String>? = null

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val name = if (nameIdx != -1) c.getString(nameIdx) ?: "" else ""
                    val number = if (numIdx != -1) c.getString(numIdx) ?: "" else ""
                    val lowerName = name.lowercase(Locale.getDefault()).trim()
                    if (lowerName == cleanQuery) {
                        exactMatch = Pair(name, number)
                        break
                    } else if (partialMatch == null && (lowerName.contains(cleanQuery) || cleanQuery.contains(lowerName))) {
                        partialMatch = Pair(name, number)
                    }
                }
            }

            val target = exactMatch ?: partialMatch
            if (target != null && target.second.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(target.second.trim())}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandResult(
                    isSuccess = true,
                    spokenResponse = "Calling ${target.first}.",
                    displayText = "Calling ${target.first}: ${target.second}",
                    actionType = "CALL_CONTACT"
                )
            } else {
                CommandResult(
                    isSuccess = false,
                    spokenResponse = "Could not find a contact named $contactName in your contacts.",
                    displayText = "Contact '$contactName' not found",
                    actionType = "CALL_CONTACT"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts for: $contactName", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not search contacts.",
                displayText = "Contact error: ${e.localizedMessage}",
                actionType = "CALL_CONTACT"
            )
        }
    }

    fun openWebsite(rawUrl: String): CommandResult {
        return try {
            val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                "https://$rawUrl"
            } else {
                rawUrl
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult(
                isSuccess = true,
                spokenResponse = "Opening website.",
                displayText = "Opened $formattedUrl",
                actionType = "WEBSITE"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening website", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Could not open website.",
                displayText = "Error opening URL: ${e.localizedMessage}",
                actionType = "WEBSITE"
            )
        }
    }

    fun getTime(): CommandResult {
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = formatter.format(Date())
        return CommandResult(
            isSuccess = true,
            spokenResponse = "The current time is $formattedTime.",
            displayText = "Current Time: $formattedTime",
            actionType = "TIME"
        )
    }

    fun getDate(): CommandResult {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val formattedDate = formatter.format(Date())
        return CommandResult(
            isSuccess = true,
            spokenResponse = "Today is $formattedDate.",
            displayText = "Today: $formattedDate",
            actionType = "DATE"
        )
    }
}
