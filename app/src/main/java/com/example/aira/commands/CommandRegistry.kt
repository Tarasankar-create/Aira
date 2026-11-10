package com.example.aira.commands

class CommandRegistry {

    fun parse(rawText: String): CommandIntent {
        val query = rawText.lowercase().trim()
            .replace(Regex("""[,?!]"""), "")
            .removeSuffix(".")
            .trim()

        if (query.isEmpty()) {
            return CommandIntent.Unknown("")
        }

        // 1. Stop Listening
        if (matchesAny(query, "stop listening", "turn off listening", "disable listening", "go to sleep", "stop aira", "cancel listening")) {
            return CommandIntent.StopListening
        }

        // 2. Flashlight ON / OFF
        if (matchesAny(query, "turn on flashlight", "turn on the flashlight", "flashlight on", "turn on torch", "torch on", "enable flashlight")) {
            return CommandIntent.SetFlashlight(true)
        }
        if (matchesAny(query, "turn off flashlight", "turn off the flashlight", "flashlight off", "turn off torch", "torch off", "disable flashlight")) {
            return CommandIntent.SetFlashlight(false)
        }

        // 3. Battery
        if (matchesAny(query, "battery", "battery level", "check battery", "what is the battery level", "what's the battery level", "how much battery", "battery percentage", "check battery level")) {
            return CommandIntent.CheckBattery
        }

        // 4. Time & Date
        if (matchesAny(query, "what time is it", "tell me the time", "current time", "what is the time", "what's the time", "time")) {
            return CommandIntent.TellTime
        }
        if (matchesAny(query, "what date is it", "today's date", "current date", "what is today's date", "what's today's date", "what day is it", "date")) {
            return CommandIntent.TellDate
        }

        // 5. Volume Actions
        if (matchesAny(query, "volume up", "increase volume", "turn up volume", "raise volume", "louder")) {
            return CommandIntent.ChangeVolume(CommandIntent.VolumeAction.UP)
        }
        if (matchesAny(query, "volume down", "decrease volume", "turn down volume", "lower volume", "quieter")) {
            return CommandIntent.ChangeVolume(CommandIntent.VolumeAction.DOWN)
        }
        if (matchesAny(query, "mute", "mute media", "mute volume", "silence", "silence media")) {
            return CommandIntent.ChangeVolume(CommandIntent.VolumeAction.MUTE)
        }

        // 6. Settings
        if (matchesAny(query, "open wifi", "open wifi settings", "wifi settings", "show wifi")) {
            return CommandIntent.OpenWifiSettings
        }
        if (matchesAny(query, "open bluetooth", "open bluetooth settings", "bluetooth settings", "show bluetooth")) {
            return CommandIntent.OpenBluetoothSettings
        }
        if (matchesAny(query, "open mobile network", "mobile network settings", "cellular settings", "network settings")) {
            return CommandIntent.OpenMobileNetworkSettings
        }
        if (matchesAny(query, "open location settings", "location settings", "gps settings", "open gps")) {
            return CommandIntent.OpenLocationSettings
        }

        // 7. Dialer / Call
        if (matchesAny(query, "open dialer", "open phone", "phone dialer", "open keypad") || query == "call" || query == "dial") {
            return CommandIntent.OpenDialer()
        }
        val callMatch = Regex("""^(?:make a call to|make a phone call to|phone|dial|call)(?:\s+to)?\s+(.+)""").find(query)
        if (callMatch != null) {
            val target = callMatch.groupValues[1].trim()
            if (target.isNotEmpty()) {
                val isPhoneNumber = target.any { it.isDigit() } &&
                        target.all { it.isDigit() || it.isWhitespace() || it in "+-()#*" }
                return if (isPhoneNumber) {
                    val number = target.filter { !it.isWhitespace() }
                    CommandIntent.OpenDialer(number)
                } else {
                    CommandIntent.CallContact(target)
                }
            }
        }

        // 8. Open Website
        val websiteMatch = Regex("""(?:open website|go to|browse)\s+(.+)""").find(query)
        if (websiteMatch != null) {
            val url = websiteMatch.groupValues[1].trim()
            return CommandIntent.OpenWebsite(url)
        }
        if (query.contains(".com") || query.contains(".org") || query.contains(".net") || query.contains(".io")) {
            val words = query.split(" ")
            val domainWord = words.firstOrNull { it.contains(".") }
            if (domainWord != null) {
                return CommandIntent.OpenWebsite(domainWord)
            }
        }

        // 9. Open App ("open whatsapp", "launch youtube", "start camera")
        val appPrefixes = listOf("open ", "launch ", "start ", "run ")
        for (prefix in appPrefixes) {
            if (query.startsWith(prefix)) {
                val appName = query.substring(prefix.length).trim()
                if (appName.isNotEmpty()) {
                    return CommandIntent.OpenApp(appName)
                }
            }
        }

        return CommandIntent.Unknown(rawQuery = rawText)
    }

    private fun matchesAny(input: String, vararg patterns: String): Boolean {
        for (pattern in patterns) {
            if (input == pattern || input.startsWith("$pattern ") || input.endsWith(" $pattern")) {
                return true
            }
        }
        return false
    }
}
