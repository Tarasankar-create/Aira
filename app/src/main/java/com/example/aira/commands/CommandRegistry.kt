package com.example.aira.commands

class CommandRegistry {

    fun parse(rawText: String): CommandIntent {
        val query = rawText.lowercase().trim()
            .replace(Regex("""[,?!।|]"""), "")
            .removeSuffix(".")
            .trim()

        if (query.isEmpty()) {
            return CommandIntent.Unknown("")
        }

        // 1. Stop Listening (English, Hindi, Odia)
        val isExactStop = query in setOf("बंद करो", "बंद कर दो", "ବନ୍ଦ କର", "stop", "cancel")
        if (isExactStop || matchesAny(
                query,
                "stop listening", "turn off listening", "disable listening", "go to sleep", "stop aira", "cancel listening",
                "सुनना बंद करो", "सो जाओ", "ruk jao", "chup ho jao",
                "ଶୁଣିବା ବନ୍ଦ କର", "ଶୋଇଯାଅ", "ରହିଯାଅ"
            )
        ) {
            return CommandIntent.StopListening
        }

        // 2. Flashlight ON / OFF
        if (matchesAny(
                query,
                "turn on flashlight", "turn on the flashlight", "flashlight on", "turn on torch", "torch on", "enable flashlight",
                "टॉर्च चालू करो", "टॉर्च ऑन करो", "फ्लैशलाइट चालू करो", "टॉर्च जलाओ", "टॉर्च चालू कर दो", "torch chalu karo", "flashlight on karo",
                "ଟର୍ଚ୍ଚ ଚାଲୁ କର", "ଟର୍ଚ୍ଚ ଲଗାଅ", "ଟର୍ଚ୍ଚ ଅନ୍ କର", "ଟର୍ଚ୍ଚ ଜଳାଅ", "torch chalu kara"
            )
        ) {
            return CommandIntent.SetFlashlight(true)
        }
        if (matchesAny(
                query,
                "turn off flashlight", "turn off the flashlight", "flashlight off", "turn off torch", "torch off", "disable flashlight",
                "टॉर्च बंद करो", "फ्लैशलाइट बंद करो", "टॉर्च बुझाओ", "टॉर्च बंद कर दो", "torch band karo", "flashlight off karo",
                "ଟର୍ଚ୍ଚ ବନ୍ଦ କର", "ଟର୍ଚ୍ଚ ଲିଭାଅ", "ଟର୍ଚ୍ଚ ଅଫ୍ କର", "torch band kara"
            )
        ) {
            return CommandIntent.SetFlashlight(false)
        }

        // 3. Battery
        if (matchesAny(
                query,
                "battery", "battery level", "check battery", "what is the battery level", "what's the battery level", "how much battery", "battery percentage", "check battery level",
                "बैटरी", "बैटरी कितनी है", "बैटरी चेक करो", "बैटरी बताओ", "चार्ज कितना है", "battery kitni hai", "battery check karo",
                "ବ୍ୟାଟେରୀ", "ବ୍ୟାଟେରୀ କେତେ ଅଛି", "ବ୍ୟାଟେରୀ ଚେକ୍ କର", "ଚାର୍ଜ କେତେ ଅଛି", "battery kete achi"
            )
        ) {
            return CommandIntent.CheckBattery
        }

        // 4. Time & Date
        if (matchesAny(
                query,
                "what time is it", "tell me the time", "current time", "what is the time", "what's the time", "time",
                "समय क्या हुआ", "टाइम क्या है", "समय बताओ", "टाइम बताओ", "कितना बजा है", "samay kya hai", "time kya hai",
                "ସମୟ କେତେ", "ସମୟ କୁହ", "ଟାଇମ୍ କେତେ", "କେତେ ବାଜିଛି", "samaya kete"
            )
        ) {
            return CommandIntent.TellTime
        }
        if (matchesAny(
                query,
                "what date is it", "today's date", "current date", "what is today's date", "what's today's date", "what day is it", "date",
                "तारीख क्या है", "आज क्या तारीख है", "आज का दिन", "तारीख बताओ", "aaj kya tarikh hai",
                "ଆଜି ତାରିଖ କେତେ", "ତାରିଖ କେତେ", "ତାରିଖ କୁହ", "ଆଜି କି ଦିନ", "aaji tarikha kete"
            )
        ) {
            return CommandIntent.TellDate
        }

        // 5. Volume Actions
        if (matchesAny(
                query,
                "volume up", "increase volume", "turn up volume", "raise volume", "louder",
                "आवाज़ बढ़ाओ", "आवाज तेज करो", "आवाज बढ़ाओ", "awaz badhao",
                "ଶବ୍ଦ ବଢ଼ାଅ", "ଆୱାଜ ବଢ଼ାଅ", "awaj badha"
            )
        ) {
            return CommandIntent.ChangeVolume(CommandIntent.VolumeAction.UP)
        }
        if (matchesAny(
                query,
                "volume down", "decrease volume", "turn down volume", "lower volume", "quieter",
                "आवाज़ कम करो", "आवाज धीमी करो", "आवाज कम करो", "awaz kam karo",
                "ଶବ୍ଦ କମାଅ", "ଆୱାଜ କମାଅ", "awaj kama"
            )
        ) {
            return CommandIntent.ChangeVolume(CommandIntent.VolumeAction.DOWN)
        }
        if (matchesAny(
                query,
                "mute", "mute media", "mute volume", "silence", "silence media",
                "म्यूट करो", "चुप करो", "आवाज़ बंद करो", "silent karo",
                "ମ୍ୟୁଟ୍ କର", "ଶବ୍ଦ ବନ୍ଦ କର"
            )
        ) {
            return CommandIntent.ChangeVolume(CommandIntent.VolumeAction.MUTE)
        }

        // 6. Settings
        if (matchesAny(
                query,
                "open wifi", "open wifi settings", "wifi settings", "show wifi",
                "वाईफाई खोलो", "वाईफाई सेटिंग", "wifi kholo",
                "ୱାଇଫାଇ ଖୋଲ", "ୱାଇଫାଇ ସେଟିଂ"
            )
        ) {
            return CommandIntent.OpenWifiSettings
        }
        if (matchesAny(
                query,
                "open bluetooth", "open bluetooth settings", "bluetooth settings", "show bluetooth",
                "ब्लूटूथ खोलो", "ब्लूटूथ सेटिंग", "bluetooth kholo",
                "ବ୍ଲୁଟୁଥ୍ ଖୋଲ", "ବ୍ଲୁଟୁଥ୍ ସେଟିଂ"
            )
        ) {
            return CommandIntent.OpenBluetoothSettings
        }
        if (matchesAny(
                query,
                "open mobile network", "mobile network settings", "cellular settings", "network settings",
                "मोबाइल नेटवर्क खोलो", "नेटवर्क सेटिंग खोलो",
                "ମୋବାଇଲ ନେଟୱର୍କ ଖୋଲ", "ନେଟୱର୍କ ସେଟିଂ ଖୋଲ"
            )
        ) {
            return CommandIntent.OpenMobileNetworkSettings
        }
        if (matchesAny(
                query,
                "open location settings", "location settings", "gps settings", "open gps",
                "लोकेशन खोलो", "जीपीएस खोलो", "location kholo",
                "ଲୋକେସନ ଖୋଲ", "ଜିପିଏସ ଖୋଲ"
            )
        ) {
            return CommandIntent.OpenLocationSettings
        }

        // 7. Dialer / Call
        val isExactDialerWord = query in setOf("call", "dial", "कॉल", "କଲ୍", "phone")
        if (isExactDialerWord || matchesAny(
                query,
                "open dialer", "open phone", "phone dialer", "open keypad",
                "फोन खोलो", "डायलर खोलो", "phone kholo",
                "ଫୋନ୍ ଖୋଲ", "ଡାଏଲର ଖୋଲ"
            )
        ) {
            return CommandIntent.OpenDialer()
        }

        // Regex for calling contacts/numbers in English, Hindi, and Odia
        // English: "call [to] X", "make a call to X", "dial X"
        val callMatchEn = Regex("""^(?:make a call to|make a phone call to|phone|dial|call)(?:\s+to)?\s+(.+)""").find(query)
        if (callMatchEn != null) {
            val target = callMatchEn.groupValues[1].trim()
            return parseCallTarget(target)
        }

        // Hindi: "कॉल करो X", "X को कॉल करो", "X को फोन करो", "फोन लगाओ X"
        val callMatchHiPrefix = Regex("""^(?:कॉल करो|फोन करो|फोन लगाओ)\s+(.+)""").find(query)
        if (callMatchHiPrefix != null) {
            val target = callMatchHiPrefix.groupValues[1].trim()
            return parseCallTarget(target)
        }
        val callMatchHiSuffix = Regex("""^(.+?)\s+(?:को कॉल करो|को फोन करो|को फोन लगाओ)$""").find(query)
        if (callMatchHiSuffix != null) {
            val target = callMatchHiSuffix.groupValues[1].trim()
            return parseCallTarget(target)
        }

        // Odia: "କଲ୍ କର X", "X କୁ କଲ୍ କର", "X କୁ ଫୋନ୍ କର"
        val callMatchOrPrefix = Regex("""^(?:କଲ୍ କର|ଫୋନ୍ କର)\s+(.+)""").find(query)
        if (callMatchOrPrefix != null) {
            val target = callMatchOrPrefix.groupValues[1].trim()
            return parseCallTarget(target)
        }
        val callMatchOrSuffix = Regex("""^(.+?)\s+(?:କୁ କଲ୍ କର|କୁ ଫୋନ୍ କର)$""").find(query)
        if (callMatchOrSuffix != null) {
            val target = callMatchOrSuffix.groupValues[1].trim()
            return parseCallTarget(target)
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

        // 9. Open App
        // English prefix: "open whatsapp", "launch youtube"
        val appPrefixes = listOf("open ", "launch ", "start ", "run ", "खोलो ", "चालू करो ", "ଖୋଲ ")
        for (prefix in appPrefixes) {
            if (query.startsWith(prefix)) {
                val appName = query.substring(prefix.length).trim()
                if (appName.isNotEmpty()) {
                    return CommandIntent.OpenApp(appName)
                }
            }
        }

        // Hindi/Odia suffixes: "यूट्यूब खोलो", "ୟୁଟ୍ୟୁବ୍ ଖୋଲ", "whatsapp चालू करो"
        val appSuffixes = listOf(" खोलो", " चालू करो", " ଖୋଲ")
        for (suffix in appSuffixes) {
            if (query.endsWith(suffix)) {
                val appName = query.removeSuffix(suffix).trim()
                if (appName.isNotEmpty()) {
                    return CommandIntent.OpenApp(appName)
                }
            }
        }

        return CommandIntent.Unknown(rawQuery = rawText)
    }

    private fun parseCallTarget(target: String): CommandIntent {
        if (target.isEmpty()) return CommandIntent.OpenDialer()
        val isPhoneNumber = target.any { it.isDigit() } &&
                target.all { it.isDigit() || it.isWhitespace() || it in "+-()#*" }
        return if (isPhoneNumber) {
            val number = target.filter { !it.isWhitespace() }
            CommandIntent.OpenDialer(number)
        } else {
            CommandIntent.CallContact(target)
        }
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
