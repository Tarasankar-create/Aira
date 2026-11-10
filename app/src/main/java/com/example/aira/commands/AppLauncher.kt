package com.example.aira.commands

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log

class AppLauncher(private val context: Context) {

    companion object {
        private const val TAG = "AiraAppLauncher"

        // Common app aliases mapping to expected keywords or package fragments
        private val COMMON_APP_HINTS = mapOf(
            "whatsapp" to listOf("whatsapp"),
            "youtube" to listOf("youtube"),
            "chrome" to listOf("chrome"),
            "settings" to listOf("settings"),
            "camera" to listOf("camera"),
            "calculator" to listOf("calc", "calculator"),
            "phone" to listOf("dialer", "phone", "call"),
            "messages" to listOf("message", "messaging", "sms", "mms"),
            "contacts" to listOf("contact", "contacts", "people"),
            "gmail" to listOf("gmail", "email", "mail"),
            "gallery" to listOf("gallery", "photo", "photos"),
            "maps" to listOf("maps", "map"),
            "clock" to listOf("clock", "alarm"),
            "calendar" to listOf("calendar")
        )
    }

    fun launchAppByName(requestedName: String): CommandResult {
        val query = requestedName.lowercase().trim()
        val pm = context.packageManager

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(launcherIntent, 0)

        if (resolveInfos.isEmpty()) {
            Log.w(TAG, "No launchable activities returned by PackageManager. Check <queries> in manifest.")
        }

        // 1. Exact match on app label
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label == query) {
                return launchPackage(info.activityInfo.packageName, info.loadLabel(pm).toString())
            }
        }

        // 2. Partial match on app label (e.g. "Google Chrome" contains "chrome")
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(query) || query.contains(label)) {
                return launchPackage(info.activityInfo.packageName, info.loadLabel(pm).toString())
            }
        }

        // 3. Search via known aliases/hints
        val hints = COMMON_APP_HINTS[query]
        if (hints != null) {
            for (hint in hints) {
                for (info in resolveInfos) {
                    val label = info.loadLabel(pm).toString().lowercase()
                    val pkg = info.activityInfo.packageName.lowercase()
                    if (label.contains(hint) || pkg.contains(hint)) {
                        return launchPackage(info.activityInfo.packageName, info.loadLabel(pm).toString())
                    }
                }
            }
        }

        // 4. Fallback search by package name contains query
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName.lowercase()
            if (pkg.contains(query)) {
                return launchPackage(info.activityInfo.packageName, info.loadLabel(pm).toString())
            }
        }

        val capitalizedName = requestedName.replaceFirstChar { it.uppercase() }
        return CommandResult(
            isSuccess = false,
            spokenResponse = "$capitalizedName is not installed on your device.",
            displayText = "Could not find application: $capitalizedName",
            actionType = "OPEN_APP"
        )
    }

    private fun launchPackage(packageName: String, appLabel: String): CommandResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                context.startActivity(intent)
                CommandResult(
                    isSuccess = true,
                    spokenResponse = "Opening $appLabel.",
                    displayText = "Launched $appLabel ($packageName)",
                    actionType = "OPEN_APP"
                )
            } else {
                CommandResult(
                    isSuccess = false,
                    spokenResponse = "Cannot open $appLabel.",
                    displayText = "No launch intent found for $packageName",
                    actionType = "OPEN_APP"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            CommandResult(
                isSuccess = false,
                spokenResponse = "Failed to open $appLabel.",
                displayText = "Error launching $packageName: ${e.localizedMessage}",
                actionType = "OPEN_APP"
            )
        }
    }
}
