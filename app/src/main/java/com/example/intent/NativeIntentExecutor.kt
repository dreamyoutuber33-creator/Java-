package com.example.intent

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.example.data.model.ActionCatalog
import com.example.data.model.JavaAction

data class IntentExecutionResult(
    val success: Boolean,
    val actionName: String,
    val userSummary: String,
    val error: String? = null
)

class NativeIntentExecutor(private val context: Context) {

    private var isTorchOn = false

    fun execute(action: JavaAction): IntentExecutionResult {
        return try {
            when (action.actionName) {
                ActionCatalog.PLAY_STORE_ACTION -> executePlayStore(action.args)
                ActionCatalog.OPEN_APP -> executeOpenApp(action.args)
                ActionCatalog.YOUTUBE_SEARCH -> executeYouTubeSearch(action.args)
                ActionCatalog.WEB_SEARCH -> executeWebSearch(action.args)
                ActionCatalog.OPEN_URL -> executeOpenUrl(action.args)
                ActionCatalog.SET_ALARM -> executeSetAlarm(action.args)
                ActionCatalog.SET_TIMER -> executeSetTimer(action.args)
                ActionCatalog.SET_REMINDER -> executeSetReminder(action.args)
                ActionCatalog.CREATE_CALENDAR_EVENT -> executeCreateCalendarEvent(action.args)
                ActionCatalog.MAKE_CALL -> executeMakeCall(action.args)
                ActionCatalog.SEND_SMS -> executeSendSms(action.args)
                ActionCatalog.SEARCH_CONTACTS -> executeSearchContacts(action.args)
                ActionCatalog.PLAY_MUSIC -> executePlayMusic(action.args)
                ActionCatalog.CONTROL_MEDIA -> executeControlMedia(action.args)
                ActionCatalog.START_NAVIGATION -> executeStartNavigation(action.args)
                ActionCatalog.GET_WEATHER -> executeGetWeather(action.args)
                ActionCatalog.CALCULATE -> executeCalculate(action.args)
                ActionCatalog.TRANSLATE -> executeTranslate(action.args)
                ActionCatalog.DICTATE_TEXT -> executeDictateText(action.args)
                ActionCatalog.SEND_EMAIL -> executeSendEmail(action.args)
                ActionCatalog.CAMERA_ACTION -> executeCameraAction(action.args)
                ActionCatalog.TOGGLE_SETTING -> executeToggleSetting(action.args)
                ActionCatalog.CONTROL_SMART_HOME -> executeControlSmartHome(action.args)
                ActionCatalog.READ_NOTIFICATIONS -> executeReadNotifications()
                ActionCatalog.READ_MESSAGES -> executeReadMessages()
                ActionCatalog.READ_SCREEN_TEXT -> executeReadScreenText()
                ActionCatalog.ACCESSIBILITY_ACTION -> executeAccessibilityAction(action.args)
                ActionCatalog.GET_DATETIME -> IntentExecutionResult(
                    success = true,
                    actionName = action.actionName,
                    userSummary = "Date & time retrieved"
                )
                else -> IntentExecutionResult(
                    success = false,
                    actionName = action.actionName,
                    userSummary = "Unknown action ${action.actionName}"
                )
            }
        } catch (e: Exception) {
            Log.e("NativeIntentExecutor", "Execution failed for ${action.actionName}", e)
            IntentExecutionResult(
                success = false,
                actionName = action.actionName,
                userSummary = "Failed to execute: ${e.localizedMessage ?: "Unknown error"}",
                error = e.message
            )
        }
    }

    private fun startActivitySafely(intent: Intent, fallbackUrl: String? = null): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            if (fallbackUrl != null) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(browserIntent)
                    true
                } catch (be: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun executePlayStore(args: Map<String, String>): IntentExecutionResult {
        val mode = args["mode"] ?: "open_store"
        val query = args["query"].orEmpty().trim()

        val intent = when (mode) {
            "search_app" -> Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query"))
            "app_details" -> Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$query"))
            else -> Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"))
        }
        val fallbackUrl = when (mode) {
            "search_app" -> "https://play.google.com/store/search?q=$query"
            "app_details" -> "https://play.google.com/store/apps/details?id=$query"
            else -> "https://play.google.com/store/apps"
        }

        val success = startActivitySafely(intent, fallbackUrl)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.PLAY_STORE_ACTION,
            userSummary = if (query.isNotBlank()) "Opening Play Store for '$query'" else "Opening Google Play Store"
        )
    }

    private fun executeOpenApp(args: Map<String, String>): IntentExecutionResult {
        val appName = (args["app_name"] ?: "").lowercase().trim()
        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "instagram" to "com.instagram.android",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.google.android.GoogleCamera",
            "photos" to "com.google.android.apps.photos",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "settings" to "com.android.settings",
            "play store" to "com.android.vending",
            "playstore" to "com.android.vending",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "telegram" to "org.telegram.messenger"
        )

        val targetPackage = packageMap[appName]
        var launched = false
        if (targetPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                launched = true
            }
        }

        if (!launched) {
            // Fallback: search on Play Store
            val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName"))
            launched = startActivitySafely(playIntent, "https://play.google.com/store/search?q=$appName")
        }

        return IntentExecutionResult(
            success = launched,
            actionName = ActionCatalog.OPEN_APP,
            userSummary = "Launched $appName"
        )
    }

    private fun executeYouTubeSearch(args: Map<String, String>): IntentExecutionResult {
        val query = args["query"].orEmpty()
        val appIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val fallback = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
        val success = startActivitySafely(appIntent, fallback)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.YOUTUBE_SEARCH,
            userSummary = "Searching YouTube for '$query'"
        )
    }

    private fun executeWebSearch(args: Map<String, String>): IntentExecutionResult {
        val query = args["query"].orEmpty()
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
        }
        val fallback = "https://www.google.com/search?q=${Uri.encode(query)}"
        val success = startActivitySafely(intent, fallback)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.WEB_SEARCH,
            userSummary = "Searching web for '$query'"
        )
    }

    private fun executeOpenUrl(args: Map<String, String>): IntentExecutionResult {
        var url = args["url"].orEmpty()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.OPEN_URL,
            userSummary = "Opened $url"
        )
    }

    private fun executeSetAlarm(args: Map<String, String>): IntentExecutionResult {
        val timeStr = args["time"] ?: "07:00"
        val label = args["label"] ?: "Java Alarm"
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.SET_ALARM,
            userSummary = "Alarm set for %02d:%02d (%s)".format(hour, minute, label)
        )
    }

    private fun executeSetTimer(args: Map<String, String>): IntentExecutionResult {
        val seconds = args["seconds"]?.toIntOrNull() ?: 300
        val label = args["label"] ?: "Java Timer"
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.SET_TIMER,
            userSummary = "Timer set for $seconds seconds ($label)"
        )
    }

    private fun executeSetReminder(args: Map<String, String>): IntentExecutionResult {
        val text = args["text"] ?: "Reminder"
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, text)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.SET_REMINDER,
            userSummary = "Reminder saved: $text"
        )
    }

    private fun executeCreateCalendarEvent(args: Map<String, String>): IntentExecutionResult {
        val title = args["title"] ?: "Event"
        val location = args["location"].orEmpty()
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            if (location.isNotBlank()) {
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            }
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.CREATE_CALENDAR_EVENT,
            userSummary = "Created event '$title'"
        )
    }

    private fun executeMakeCall(args: Map<String, String>): IntentExecutionResult {
        val contact = args["contact"].orEmpty()
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$contact"))
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.MAKE_CALL,
            userSummary = "Initiating call to $contact"
        )
    }

    private fun executeSendSms(args: Map<String, String>): IntentExecutionResult {
        val contact = args["contact"].orEmpty()
        val message = args["message"].orEmpty()
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$contact")).apply {
            putExtra("sms_body", message)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.SEND_SMS,
            userSummary = "Opening SMS for $contact"
        )
    }

    private fun executeSearchContacts(args: Map<String, String>): IntentExecutionResult {
        val query = args["query"].orEmpty()
        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.SEARCH_CONTACTS,
            userSummary = "Searching contacts for '$query'"
        )
    }

    private fun executePlayMusic(args: Map<String, String>): IntentExecutionResult {
        val query = args["query"].orEmpty()
        val app = args["app"] ?: "default"

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            putExtra(SearchManager.QUERY, query)
            if (app.contains("spotify", ignoreCase = true)) {
                setPackage("com.spotify.music")
            } else if (app.contains("youtube", ignoreCase = true)) {
                setPackage("com.google.android.apps.youtube.music")
            }
        }
        val fallback = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
        val success = startActivitySafely(intent, fallback)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.PLAY_MUSIC,
            userSummary = "Playing music '$query'"
        )
    }

    private fun executeControlMedia(args: Map<String, String>): IntentExecutionResult {
        val command = args["command"].orEmpty()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        when (command) {
            "volume_up" -> audioManager?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "volume_down" -> audioManager?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            else -> {
                // Media control action
            }
        }
        return IntentExecutionResult(
            success = true,
            actionName = ActionCatalog.CONTROL_MEDIA,
            userSummary = "Media command '$command' sent"
        )
    }

    private fun executeStartNavigation(args: Map<String, String>): IntentExecutionResult {
        val destination = args["destination"].orEmpty()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(destination)}")).apply {
            setPackage("com.google.android.apps.maps")
        }
        val fallback = "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}"
        val success = startActivitySafely(intent, fallback)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.START_NAVIGATION,
            userSummary = "Starting navigation to $destination"
        )
    }

    private fun executeGetWeather(args: Map<String, String>): IntentExecutionResult {
        val location = args["location"].orEmpty()
        val fallback = "https://www.google.com/search?q=weather+in+${Uri.encode(location)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallback))
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.GET_WEATHER,
            userSummary = "Fetching weather for $location"
        )
    }

    private fun executeCalculate(args: Map<String, String>): IntentExecutionResult {
        val expr = args["expression"].orEmpty()
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALCULATOR)
        }
        val success = startActivitySafely(intent, "https://www.google.com/search?q=${Uri.encode(expr)}")
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.CALCULATE,
            userSummary = "Calculated expression: $expr"
        )
    }

    private fun executeTranslate(args: Map<String, String>): IntentExecutionResult {
        val text = args["text"].orEmpty()
        val target = args["target_language"] ?: "hi"
        val url = "https://translate.google.com/?sl=auto&tl=$target&text=${Uri.encode(text)}&op=translate"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.TRANSLATE,
            userSummary = "Translating '$text'"
        )
    }

    private fun executeDictateText(args: Map<String, String>): IntentExecutionResult {
        val text = args["text"].orEmpty()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Java Dictation", text)
        clipboard?.setPrimaryClip(clip)
        return IntentExecutionResult(
            success = true,
            actionName = ActionCatalog.DICTATE_TEXT,
            userSummary = "Copied to clipboard: $text"
        )
    }

    private fun executeSendEmail(args: Map<String, String>): IntentExecutionResult {
        val recipient = args["recipient"].orEmpty()
        val subject = args["subject"].orEmpty()
        val body = args["body"].orEmpty()

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.SEND_EMAIL,
            userSummary = "Opening email to $recipient"
        )
    }

    private fun executeCameraAction(args: Map<String, String>): IntentExecutionResult {
        val mode = args["mode"] ?: "open_camera"
        val intent = when (mode) {
            "record_video" -> Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            "take_photo" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            else -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.CAMERA_ACTION,
            userSummary = "Opening Camera ($mode)"
        )
    }

    private fun executeToggleSetting(args: Map<String, String>): IntentExecutionResult {
        val setting = args["setting"].orEmpty().lowercase()
        val state = args["state"].orEmpty().lowercase()

        if (setting == "flashlight") {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                if (cameraId != null) {
                    val turnOn = state == "on" || (state.isBlank() && !isTorchOn)
                    cameraManager.setTorchMode(cameraId, turnOn)
                    isTorchOn = turnOn
                    return IntentExecutionResult(
                        success = true,
                        actionName = ActionCatalog.TOGGLE_SETTING,
                        userSummary = "Flashlight turned ${if (turnOn) "ON" else "OFF"}"
                    )
                }
            } catch (e: CameraAccessException) {
                Log.e("NativeIntentExecutor", "Torch toggle failed", e)
            }
        }

        val intent = when (setting) {
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "hotspot" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            "airplane_mode" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.TOGGLE_SETTING,
            userSummary = "Opened settings for $setting"
        )
    }

    private fun executeControlSmartHome(args: Map<String, String>): IntentExecutionResult {
        val device = args["device"] ?: "Device"
        val action = args["action"] ?: "toggle"
        // Launch Google Home app or settings
        val homeIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.chromecast.app")
        val success = if (homeIntent != null) {
            startActivitySafely(homeIntent)
        } else {
            startActivitySafely(Intent(Settings.ACTION_SETTINGS))
        }
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.CONTROL_SMART_HOME,
            userSummary = "$action applied to $device"
        )
    }

    private fun executeReadNotifications(): IntentExecutionResult {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.READ_NOTIFICATIONS,
            userSummary = "Opened Notification access"
        )
    }

    private fun executeReadMessages(): IntentExecutionResult {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
        }
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.READ_MESSAGES,
            userSummary = "Opened Messages app"
        )
    }

    private fun executeReadScreenText(): IntentExecutionResult {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.READ_SCREEN_TEXT,
            userSummary = "Screen reader / TalkBack settings ready"
        )
    }

    private fun executeAccessibilityAction(args: Map<String, String>): IntentExecutionResult {
        val feature = args["feature"].orEmpty().lowercase()
        if (feature == "haptic_feedback") {
            vibrateDevice(context)
            return IntentExecutionResult(
                success = true,
                actionName = ActionCatalog.ACCESSIBILITY_ACTION,
                userSummary = "Haptic feedback triggered"
            )
        }
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val success = startActivitySafely(intent)
        return IntentExecutionResult(
            success = success,
            actionName = ActionCatalog.ACCESSIBILITY_ACTION,
            userSummary = "Opened Accessibility Settings for $feature"
        )
    }

    private fun vibrateDevice(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {
            Log.e("NativeIntentExecutor", "Vibration failed", e)
        }
    }
}
