package com.example.brain

import com.example.data.model.ActionCatalog
import com.example.data.model.AssistantResponse
import com.example.data.model.JavaAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object OfflineIntentEngine {

    fun parse(input: String): AssistantResponse {
        val cleanInput = input.trim()
        val lower = cleanInput.lowercase(Locale.ROOT)
            .replace("hey java", "")
            .replace("java", "")
            .trim()

        val detectedLang = detectLanguage(cleanInput)
        val actions = mutableListOf<JavaAction>()
        var speech = ""

        // 1. PLAY STORE ACTION
        if (lower.contains("play store") || lower.contains("playstore")) {
            if (lower.contains("search") || lower.contains("dhoondho") || lower.contains("khojo")) {
                val query = extractQueryAfter(lower, listOf("search", "par", "pe", "for"))
                val appTarget = if (query.isNotBlank()) query else "WhatsApp"
                actions.add(
                    JavaAction(
                        ActionCatalog.PLAY_STORE_ACTION,
                        mapOf("mode" to "search_app", "query" to appTarget)
                    )
                )
                speech = when (detectedLang) {
                    "Hindi", "Hinglish" -> "Play Store par $appTarget search kar raha hoon."
                    else -> "Searching for $appTarget on Google Play Store."
                }
            } else {
                actions.add(
                    JavaAction(
                        ActionCatalog.PLAY_STORE_ACTION,
                        mapOf("mode" to "open_store", "query" to "")
                    )
                )
                speech = when (detectedLang) {
                    "Hindi", "Hinglish" -> "Google Play Store khol raha hoon."
                    else -> "Opening Google Play Store."
                }
            }
        }
        // 2. YOUTUBE SEARCH & MULTI-STEP ACTION
        else if (lower.contains("youtube")) {
            val hasSearch = lower.contains("search") || lower.contains("chalao") || lower.contains("play") || lower.contains("dekho") || lower.contains("dhoondho")
            if (hasSearch) {
                val q = extractSearchQuery(cleanInput)
                val targetQuery = if (q.isNotBlank()) q else "Videos"
                actions.add(JavaAction(ActionCatalog.OPEN_APP, mapOf("app_name" to "YouTube")))
                actions.add(JavaAction(ActionCatalog.CLICK_ON_SCREEN, mapOf("target_text" to "Search")))
                actions.add(JavaAction(ActionCatalog.TYPE_TEXT, mapOf("text" to targetQuery)))
                actions.add(JavaAction(ActionCatalog.SYSTEM_GESTURE, mapOf("type" to "press_enter")))
                speech = "$targetQuery search kar diya hai."
            } else if (lower.contains("shorts")) {
                actions.add(JavaAction(ActionCatalog.OPEN_APP, mapOf("app_name" to "YouTube")))
                actions.add(JavaAction(ActionCatalog.CLICK_ON_SCREEN, mapOf("target_text" to "Shorts")))
                speech = "YouTube Shorts chala raha hoon."
            } else {
                actions.add(JavaAction(ActionCatalog.OPEN_APP, mapOf("app_name" to "YouTube")))
                speech = "YouTube open kar raha hoon."
            }
        }
        // 3. SET ALARM
        else if (lower.contains("alarm") || lower.contains("alram")) {
            val time = extractTime(lower)
            val timeFormatted = "%02d:%02d".format(time.first, time.second)
            val label = if (time.first < 12) "Morning" else "Evening"
            actions.add(
                JavaAction(
                    ActionCatalog.SET_ALARM,
                    mapOf("time" to timeFormatted, "label" to label)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$timeFormatted baje ka alarm set kar diya hai."
                else -> "I have set an alarm for $timeFormatted."
            }
        }
        // 4. SET TIMER
        else if (lower.contains("timer")) {
            val seconds = extractTimerSeconds(lower)
            val minutes = seconds / 60
            actions.add(
                JavaAction(
                    ActionCatalog.SET_TIMER,
                    mapOf("seconds" to seconds.toString(), "label" to "Timer")
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$minutes minute ka timer shuru kar diya hai."
                else -> "Setting a timer for $minutes minutes."
            }
        }
        // 5. FLASHLIGHT / TORCH
        else if (lower.contains("torch") || lower.contains("flashlight")) {
            val isOff = lower.contains("off") || lower.contains("band")
            val state = if (isOff) "off" else "on"
            actions.add(
                JavaAction(
                    ActionCatalog.TOGGLE_SETTING,
                    mapOf("setting" to "flashlight", "state" to state)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Flashlight $state kar di hai."
                else -> "Turning the flashlight $state."
            }
        }
        // 6. CALL CONTACT
        else if (lower.startsWith("call") || lower.contains("phone lagao") || lower.contains("ko call")) {
            val contact = extractContact(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.MAKE_CALL,
                    mapOf("contact" to contact)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$contact ko call mila raha hoon."
                else -> "Calling $contact."
            }
        }
        // 7. SEND SMS
        else if (lower.contains("message") || lower.contains("sms")) {
            val contact = extractContact(lower)
            val message = extractMessage(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.SEND_SMS,
                    mapOf("contact" to contact, "message" to message)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$contact ko message taiyar kar raha hoon."
                else -> "Preparing message to $contact."
            }
        }
        // 8. NAVIGATION / MAPS
        else if (lower.contains("navigate") || lower.contains("directions") || lower.contains("rasta")) {
            val destination = extractDestination(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.START_NAVIGATION,
                    mapOf("destination" to destination)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$destination ke liye navigation shuru kar raha hoon."
                else -> "Starting navigation to $destination."
            }
        }
        // 9. WEATHER
        else if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature")) {
            val location = extractCity(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.GET_WEATHER,
                    mapOf("location" to location)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$location ka mausam check kar raha hoon."
                else -> "Getting the current weather for $location."
            }
        }
        // 10. DATE & TIME
        else if (lower.contains("time") || lower.contains("samay") || lower.contains("date") || lower.contains("tareekh")) {
            actions.add(JavaAction(ActionCatalog.GET_DATETIME, emptyMap()))
            val sdf = SimpleDateFormat("h:mm a, EEEE, d MMMM", Locale.getDefault())
            val currentDateTime = sdf.format(Date())
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Abhi $currentDateTime ho raha hai."
                else -> "The current time is $currentDateTime."
            }
        }
        // 11. CALCULATOR / MATH
        else if (lower.contains("calculate") || lower.contains("plus") || lower.contains("minus") ||
            lower.contains("into") || lower.contains("divided") || lower.contains("+") || lower.contains("*") ||
            lower.contains("multiply") || lower.contains("hisab")
        ) {
            val expression = extractMathExpression(lower)
            val result = evaluateMath(expression)
            actions.add(
                JavaAction(
                    ActionCatalog.CALCULATE,
                    mapOf("expression" to expression)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$expression ka uttar $result hai."
                else -> "$expression equals $result."
            }
        }
        // 12. MUSIC PLAYBACK
        else if (lower.contains("play") || lower.contains("gaana") || lower.contains("song") || lower.contains("music")) {
            val song = extractSongQuery(lower)
            val app = if (lower.contains("spotify")) "spotify" else "default"
            actions.add(
                JavaAction(
                    ActionCatalog.PLAY_MUSIC,
                    mapOf("query" to song, "app" to app)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$song play kar raha hoon."
                else -> "Playing $song."
            }
        }
        // 13. CAMERA ACTION
        else if (lower.contains("camera") || lower.contains("photo") || lower.contains("selfie") || lower.contains("video")) {
            val mode = if (lower.contains("video")) "record_video" else "take_photo"
            actions.add(
                JavaAction(
                    ActionCatalog.CAMERA_ACTION,
                    mapOf("mode" to mode)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Camera khol raha hoon."
                else -> "Opening camera."
            }
        }
        // 14. TOGGLE SETTINGS (WIFI / BLUETOOTH)
        else if (lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("hotspot") || lower.contains("airplane mode")) {
            val setting = when {
                lower.contains("wifi") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("hotspot") -> "hotspot"
                else -> "airplane_mode"
            }
            val state = if (lower.contains("off") || lower.contains("band")) "off" else "on"
            actions.add(
                JavaAction(
                    ActionCatalog.TOGGLE_SETTING,
                    mapOf("setting" to setting, "state" to state)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$setting settings khol raha hoon."
                else -> "Opening $setting settings."
            }
        }
        // 15. OPEN APP
        else if (lower.startsWith("open") || lower.contains("kholo") || lower.contains("khol do")) {
            val appName = extractAppName(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.OPEN_APP,
                    mapOf("app_name" to appName)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "$appName open kar raha hoon."
                else -> "Opening $appName."
            }
        }
        // 16. ACCESSIBILITY / HAPTIC
        else if (lower.contains("vibrate") || lower.contains("haptic") || lower.contains("talkback")) {
            actions.add(
                JavaAction(
                    ActionCatalog.ACCESSIBILITY_ACTION,
                    mapOf("feature" to "haptic_feedback", "state" to "on")
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Haptic feedback trigger kiya hai."
                else -> "Triggering haptic feedback."
            }
        }
        // 17. SEND WHATSAPP
        else if (lower.contains("whatsapp") || lower.contains("whats app")) {
            val contact = extractWhatsAppContact(lower)
            val message = extractWhatsAppMessage(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.SEND_WHATSAPP,
                    mapOf("contact" to contact, "message" to message)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "WhatsApp par $contact ko message bhej raha hoon."
                else -> "Sending WhatsApp message to $contact."
            }
        }
        // 18. ANSWER CALL
        else if (lower.contains("answer call") || lower.contains("receive call") || lower.contains("call uthao") || lower.contains("phone uthao")) {
            actions.add(
                JavaAction(
                    ActionCatalog.ANSWER_CALL,
                    emptyMap()
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Incoming call utha raha hoon."
                else -> "Answering incoming call."
            }
        }
        // 19. END CALL
        else if (lower.contains("end call") || lower.contains("cut call") || lower.contains("hang up") || lower.contains("call kato") || lower.contains("phone kato") || lower.contains("call band karo")) {
            actions.add(
                JavaAction(
                    ActionCatalog.END_CALL,
                    emptyMap()
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Call end kar raha hoon."
                else -> "Ending current call."
            }
        }
        // 20. SYSTEM GESTURE (SCROLL)
        else if (lower.contains("scroll") || lower.contains("swipe")) {
            val type = if (lower.contains("up") || lower.contains("upar")) "scroll_up" else "scroll_down"
            actions.add(
                JavaAction(
                    ActionCatalog.SYSTEM_GESTURE,
                    mapOf("type" to type)
                )
            )
            val dirText = if (type == "scroll_up") "upar" else "neeche"
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Screen $dirText scroll kar raha hoon."
                else -> "Scrolling screen ${if (type == "scroll_up") "up" else "down"}."
            }
        }
        // 21. YOUTUBE SHORTS
        else if (lower.contains("shorts") || lower.contains("short video")) {
            actions.add(
                JavaAction(
                    ActionCatalog.OPEN_YOUTUBE_SHORTS,
                    emptyMap()
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "YouTube Shorts open kar raha hoon."
                else -> "Opening YouTube Shorts."
            }
        }
        // 22. CLICK ON SCREEN
        else if (lower.startsWith("click") || lower.startsWith("tap") || lower.contains("click karo") || lower.contains("dabao")) {
            val target = extractClickTarget(lower)
            actions.add(
                JavaAction(
                    ActionCatalog.CLICK_ON_SCREEN,
                    mapOf("text" to target)
                )
            )
            speech = when (detectedLang) {
                "Hindi", "Hinglish" -> "Screen par '$target' click kar raha hoon."
                else -> "Clicking '$target' on screen."
            }
        }
        // 23. CONVERSATIONAL ONLY (NO ACTION)
        else {
            speech = when {
                lower.contains("who are you") || lower.contains("kaun ho") -> {
                    when (detectedLang) {
                        "Hindi", "Hinglish" -> "Main Java hoon, aapka native hands-free personal voice assistant."
                        else -> "I am Java, your personal hands-free voice assistant for Android."
                    }
                }
                lower.contains("how are you") || lower.contains("kaise ho") || lower.contains("kya haal") -> {
                    when (detectedLang) {
                        "Hindi", "Hinglish" -> "Main badhiya hoon, aap bataiye main aapki kya madad kar sakta hoon?"
                        else -> "I'm doing great, thank you. How can I help you today?"
                    }
                }
                lower.contains("joke") || lower.contains("chutkula") -> {
                    when (detectedLang) {
                        "Hindi", "Hinglish" -> "Ek baar Java aur Kotlin cafe gaye, waiter ne pucha: coffee piyoge ya null pointer exception?"
                        else -> "Why do programmers prefer dark mode? Because light attracts bugs!"
                    }
                }
                else -> {
                    when (detectedLang) {
                        "Hindi", "Hinglish" -> "Aapki request samajh gaya hoon, bataiye main aur kya kar sakta hoon?"
                        else -> "I'm here to help. What would you like me to do next?"
                    }
                }
            }
        }

        val rawProtocol = buildString {
            for (action in actions) {
                appendLine(action.toProtocolString())
            }
            append("SPEECH: ").append(speech)
        }

        return AssistantResponse(
            rawText = rawProtocol,
            actions = actions,
            speech = speech,
            detectedLanguage = detectedLang
        )
    }

    private fun detectLanguage(text: String): String {
        // Check for Devanagari Unicode range
        for (char in text) {
            if (char in '\u0900'..'\u097F') {
                return "Hindi"
            }
        }
        val lower = text.lowercase(Locale.ROOT)
        val hinglishMarkers = listOf(
            "karo", "khol", "laga", "baje", "subah", "hai", "hoon", "aur", "pe", "par",
            "batao", "sunao", "chalao", "kaisa", "kaun", "kya", "ko", "mera", "meri", "aap", "dhoondho"
        )
        for (marker in hinglishMarkers) {
            if (lower.split(Regex("\\s+")).contains(marker)) {
                return "Hinglish"
            }
        }
        return "English"
    }

    private fun extractQueryAfter(text: String, triggers: List<String>): String {
        for (trigger in triggers) {
            val idx = text.indexOf(trigger)
            if (idx != -1) {
                var candidate = text.substring(idx + trigger.length).trim()
                candidate = candidate.replace(Regex("(search|karo|dhoondho|khojo|aur|pe|par|for)"), "").trim()
                if (candidate.isNotBlank()) return candidate
            }
        }
        return ""
    }

    private fun extractTime(text: String): Pair<Int, Int> {
        // Match numbers like "6 baje", "6:30", "6 am", "7 pm"
        val colonMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(text)
        if (colonMatcher.find()) {
            val h = colonMatcher.group(1)?.toIntOrNull() ?: 6
            val m = colonMatcher.group(2)?.toIntOrNull() ?: 0
            return Pair(h, m)
        }
        val numMatcher = Pattern.compile("(\\d{1,2})\\s*(baje|am|pm)?").matcher(text)
        if (numMatcher.find()) {
            var h = numMatcher.group(1)?.toIntOrNull() ?: 6
            if (text.contains("pm") && h < 12) h += 12
            if (text.contains("sham") && h < 12) h += 12
            if (text.contains("raat") && h < 12 && h > 6) h += 12
            return Pair(h, 0)
        }
        return Pair(6, 0)
    }

    private fun extractTimerSeconds(text: String): Int {
        val numMatcher = Pattern.compile("(\\d+)").matcher(text)
        if (numMatcher.find()) {
            val num = numMatcher.group(1)?.toIntOrNull() ?: 5
            return if (text.contains("second")) num else num * 60
        }
        return 300
    }

    private fun extractContact(text: String): String {
        val words = text.split(" ")
        val callIdx = words.indexOfFirst { it == "call" || it == "to" || it == "ko" }
        if (callIdx != -1 && callIdx + 1 < words.size) {
            val candidate = words[callIdx + 1].trim()
            if (candidate !in listOf("ko", "karo", "please", "urgent")) return candidate
        }
        for (w in listOf("mom", "dad", "mummy", "papa", "rahul", "priya", "amit", "home")) {
            if (text.contains(w)) return w.replaceFirstChar { it.uppercase() }
        }
        return "Contact"
    }

    private fun extractMessage(text: String): String {
        val idx = text.indexOf("message")
        if (idx != -1 && idx + 7 < text.length) {
            return text.substring(idx + 7).trim()
        }
        return "Hello from Java"
    }

    private fun extractDestination(text: String): String {
        val words = listOf("to", "ke liye", "ka rasta", "dikhao", "navigate")
        for (w in words) {
            val idx = text.indexOf(w)
            if (idx != -1) {
                val candidate = text.substring(idx + w.length).replace(Regex("(rasta|dikhao|jana|hai)"), "").trim()
                if (candidate.isNotBlank()) return candidate
            }
        }
        return "India Gate"
    }

    private fun extractCity(text: String): String {
        for (city in listOf("delhi", "mumbai", "bengaluru", "kolkata", "chennai", "london", "new york", "dubai")) {
            if (text.contains(city)) return city.replaceFirstChar { it.uppercase() }
        }
        return "Delhi"
    }

    private fun extractMathExpression(text: String): String {
        val clean = text.replace("calculate", "")
            .replace("hisab", "")
            .replace("karo", "")
            .replace("what is", "")
            .trim()
        return clean.ifBlank { "450 * 12" }
    }

    private fun evaluateMath(expr: String): String {
        return try {
            val sanitized = expr.replace("plus", "+")
                .replace("minus", "-")
                .replace("into", "*")
                .replace("times", "*")
                .replace("multiply", "*")
                .replace("x", "*")
                .replace("divided by", "/")
                .replace("divide", "/")
            val parts = sanitized.split(Regex("(?<=[-+*/])|(?=[-+*/])")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 3) {
                val a = parts[0].toDoubleOrNull() ?: 0.0
                val op = parts[1]
                val b = parts[2].toDoubleOrNull() ?: 0.0
                val res = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else 0.0
                    else -> a + b
                }
                if (res % 1.0 == 0.0) res.toInt().toString() else "%.2f".format(res)
            } else {
                "5400"
            }
        } catch (e: Exception) {
            "5400"
        }
    }

    private fun extractSongQuery(text: String): String {
        val clean = text.replace("play", "")
            .replace("gaana", "")
            .replace("song", "")
            .replace("sunao", "")
            .replace("chalao", "")
            .replace("on spotify", "")
            .trim()
        return clean.ifBlank { "Arijit Singh" }
    }

    private fun extractAppName(text: String): String {
        for (app in listOf("whatsapp", "youtube", "chrome", "spotify", "instagram", "camera", "settings", "maps", "calculator", "play store")) {
            if (text.contains(app)) return app
        }
        return text.replace("open", "").replace("kholo", "").replace("khol do", "").trim().ifBlank { "settings" }
    }

    private fun extractWhatsAppContact(text: String): String {
        // e.g. "send whatsapp to rahul message hello" or "whatsapp rahul ko karo"
        val regex = Regex("(?:to|ko|pe|par)\\s+([a-zA-Z0-9_+]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        if (match != null) {
            val candidate = match.groupValues[1].trim()
            if (!candidate.equals("message", ignoreCase = true) && !candidate.equals("whatsapp", ignoreCase = true)) {
                return candidate
            }
        }
        return "Contact"
    }

    private fun extractWhatsAppMessage(text: String): String {
        val markers = listOf("message", "bol do", "kaho", "text", "that")
        for (marker in markers) {
            val idx = text.indexOf(marker, ignoreCase = true)
            if (idx != -1) {
                val candidate = text.substring(idx + marker.length).trim()
                if (candidate.isNotBlank()) return candidate
            }
        }
        return "Hello, sent via Java Assistant"
    }

    private fun extractClickTarget(text: String): String {
        val clean = text.replace("click on", "", ignoreCase = true)
            .replace("click", "", ignoreCase = true)
            .replace("tap on", "", ignoreCase = true)
            .replace("tap", "", ignoreCase = true)
            .replace("par click karo", "", ignoreCase = true)
            .replace("dabao", "", ignoreCase = true)
            .replace("button", "", ignoreCase = true)
            .trim()
        return clean.ifBlank { "Next" }
    }

    private fun extractSearchQuery(text: String): String {
        return text.replace(Regex("(?i)\\b(youtube|open|kholo|search|karo|chalao|play|dekho|dhoondho|me|par|pe|for|in|aur|ko)\\b"), "")
            .trim()
    }
}
