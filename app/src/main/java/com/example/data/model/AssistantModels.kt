package com.example.data.model

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class JavaAction(
    val actionName: String,
    val args: Map<String, String>
) {
    fun toProtocolString(): String {
        val argsString = args.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ", "
        ) { "\"${it.key}\": \"${it.value}\"" }
        return "ACTION: $actionName | ARGS: $argsString"
    }
}

data class AssistantResponse(
    val rawText: String,
    val actions: List<JavaAction>,
    val speech: String,
    val detectedLanguage: String, // "English", "Hindi", "Hinglish"
    val timestamp: Long = System.currentTimeMillis()
)

data class ConversationTurn(
    val id: String = System.currentTimeMillis().toString(),
    val userQuery: String,
    val response: AssistantResponse,
    val isAutoExecuted: Boolean = false,
    val executionResult: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

object ActionCatalog {
    const val PLAY_STORE_ACTION = "PLAY_STORE_ACTION"
    const val OPEN_APP = "OPEN_APP"
    const val YOUTUBE_SEARCH = "YOUTUBE_SEARCH"
    const val WEB_SEARCH = "WEB_SEARCH"
    const val OPEN_URL = "OPEN_URL"
    const val SET_ALARM = "SET_ALARM"
    const val SET_TIMER = "SET_TIMER"
    const val SET_REMINDER = "SET_REMINDER"
    const val CREATE_CALENDAR_EVENT = "CREATE_CALENDAR_EVENT"
    const val MAKE_CALL = "MAKE_CALL"
    const val SEND_SMS = "SEND_SMS"
    const val SEARCH_CONTACTS = "SEARCH_CONTACTS"
    const val PLAY_MUSIC = "PLAY_MUSIC"
    const val CONTROL_MEDIA = "CONTROL_MEDIA"
    const val START_NAVIGATION = "START_NAVIGATION"
    const val GET_WEATHER = "GET_WEATHER"
    const val GET_DATETIME = "GET_DATETIME"
    const val CALCULATE = "CALCULATE"
    const val TRANSLATE = "TRANSLATE"
    const val DICTATE_TEXT = "DICTATE_TEXT"
    const val SEND_EMAIL = "SEND_EMAIL"
    const val CAMERA_ACTION = "CAMERA_ACTION"
    const val TOGGLE_SETTING = "TOGGLE_SETTING"
    const val CONTROL_SMART_HOME = "CONTROL_SMART_HOME"
    const val READ_NOTIFICATIONS = "READ_NOTIFICATIONS"
    const val READ_MESSAGES = "READ_MESSAGES"
    const val READ_SCREEN_TEXT = "READ_SCREEN_TEXT"
    const val ACCESSIBILITY_ACTION = "ACCESSIBILITY_ACTION"
}
