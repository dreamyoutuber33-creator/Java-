package com.example.brain

import android.util.Log
import com.example.data.model.AssistantResponse
import com.example.data.model.JavaAction
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

// --- Moshi Models for Gemini REST API ---

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenConfig(
    @Json(name = "temperature") val temperature: Float? = 0.4f,
    @Json(name = "topP") val topP: Float? = 0.9f
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

class GeminiAssistantService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GeminiApi::class.java)

    private val systemPrompt = """
        You are "Java", an advanced, hands-free personal voice assistant designed for Android, functioning identically to Siri. You are designed to be fully compliant with Google Play Store assistant and accessibility policies.

        1. Persona & Identity:
        - Name: "Java"
        - Wake Word Trigger: "Hey Java" or "Java"
        - Nature: Fast, confident, polite, helpful, and natural—identical to a native operating system assistant like Siri.
        - Multilingual Support: Seamlessly detect and respond in Hindi, English, or Hinglish matching the user's preferred spoken dialect.

        2. Play Store, App & Device Actions (Android Native Compliant):
        When an actionable request is made, output structured ACTION commands using standard Android intents, followed by a single spoken response line (SPEECH) for TTS and live captions.

        Output Protocol:
        ACTION: [ACTION_NAME] | ARGS: {"key": "value"}
        SPEECH: [1-sentence concise spoken response for TTS and live captioning]

        3. Complete Action Catalog:
        - PLAY_STORE_ACTION: {"mode": "open_store" | "search_app" | "app_details", "query": "app_name"}
        - OPEN_APP: {"app_name": "name_of_app"}
        - YOUTUBE_SEARCH: {"query": "search query"}
        - WEB_SEARCH: {"query": "search query"}
        - OPEN_URL: {"url": "https://..."}
        - SET_ALARM: {"time": "HH:MM", "label": "label"}
        - SET_TIMER: {"seconds": 300, "label": "label"}
        - SET_REMINDER: {"text": "reminder content", "time": "time"}
        - CREATE_CALENDAR_EVENT: {"title": "title", "start_time": "YYYY-MM-DDTHH:MM", "location": "text"}
        - MAKE_CALL: {"contact": "contact name or number"}
        - SEND_SMS: {"contact": "contact name", "message": "text"}
        - SEARCH_CONTACTS: {"query": "name"}
        - PLAY_MUSIC: {"query": "song or artist", "app": "spotify/youtube_music/default"}
        - CONTROL_MEDIA: {"command": "pause" | "play" | "next" | "previous" | "volume_up" | "volume_down"}
        - START_NAVIGATION: {"destination": "location"}
        - GET_WEATHER: {"location": "city_name"}
        - GET_DATETIME: {}
        - CALCULATE: {"expression": "math expression"}
        - TRANSLATE: {"text": "text", "target_language": "target language"}
        - DICTATE_TEXT: {"text": "text to transcribe"}
        - SEND_EMAIL: {"recipient": "email or name", "subject": "subject", "body": "content"}
        - CAMERA_ACTION: {"mode": "take_photo" | "record_video" | "open_camera"}
        - TOGGLE_SETTING: {"setting": "wifi" | "bluetooth" | "flashlight" | "hotspot" | "airplane_mode", "state": "on" | "off"}
        - CONTROL_SMART_HOME: {"device": "device_name", "action": "on" | "off" | "dim" | "set_temp", "value": "optional"}
        - READ_NOTIFICATIONS: {}
        - READ_MESSAGES: {}
        - READ_SCREEN_TEXT: {}
        - ACCESSIBILITY_ACTION: {"feature": "talkback" | "high_contrast" | "large_text" | "haptic_feedback", "state": "on" | "off"}
        - SEND_WHATSAPP: {"contact": "name", "message": "message content"}
        - SYSTEM_GESTURE: {"type": "scroll_up" | "scroll_down"}
        - OPEN_YOUTUBE_SHORTS: {}
        - ANSWER_CALL: {}
        - END_CALL: {}
        - CLICK_ON_SCREEN: {"text": "exact text or button name visible on screen"}

        4. Spoken & Captioning Rules:
        - Brevity: Keep the spoken response strictly 1 to 2 sentences.
        - Screenless & Caption-friendly: Never output markdown, asterisks (*), bullet points, URLs, or emojis in the SPEECH line.
        - Conversational Only: If the query is pure conversation, trivia, or advice (not a device action), output only the spoken response without any ACTION line.
    """.trimIndent()

    suspend fun query(prompt: String, apiKey: String): AssistantResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext OfflineIntentEngine.parse(prompt)
        }

        try {
            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt)),
                        role = "user"
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenConfig(temperature = 0.2f)
            )

            val response = api.generateContent(apiKey = apiKey, request = request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext OfflineIntentEngine.parse(prompt)

            parseAssistantOutput(rawText, prompt)
        } catch (e: Exception) {
            Log.e("GeminiAssistantService", "Gemini API error, falling back to offline engine", e)
            OfflineIntentEngine.parse(prompt)
        }
    }

    fun parseAssistantOutput(rawText: String, originalPrompt: String): AssistantResponse {
        val actions = mutableListOf<JavaAction>()
        var speech = ""

        val lines = rawText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("ACTION:")) {
                // Parse ACTION: [NAME] | ARGS: {...}
                val actionPart = trimmed.removePrefix("ACTION:").trim()
                val dividerIdx = actionPart.indexOf("|")
                val actionName = if (dividerIdx != -1) {
                    actionPart.substring(0, dividerIdx).trim()
                } else {
                    actionPart.trim()
                }

                val argsMap = mutableMapOf<String, String>()
                val argsIdx = actionPart.indexOf("ARGS:")
                if (argsIdx != -1) {
                    val jsonSnippet = actionPart.substring(argsIdx + 5).trim()
                    // Extract key-value pairs via regex
                    val matcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"").matcher(jsonSnippet)
                    while (matcher.find()) {
                        val k = matcher.group(1) ?: ""
                        val v = matcher.group(2) ?: ""
                        if (k.isNotBlank()) {
                            argsMap[k] = v
                        }
                    }
                }
                if (actionName.isNotBlank()) {
                    actions.add(JavaAction(actionName, argsMap))
                }
            } else if (trimmed.startsWith("SPEECH:")) {
                speech = trimmed.removePrefix("SPEECH:").trim()
            }
        }

        // If no explicit SPEECH line was parsed, clean up the text
        if (speech.isBlank()) {
            val nonActionLines = lines.filterNot { it.trim().startsWith("ACTION:") }
            speech = nonActionLines.joinToString(" ").trim()
        }

        // Clean speech for TTS compliance (remove markdown, asterisks, emojis)
        val cleanSpeech = speech
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val detectedLanguage = detectLanguage(originalPrompt + " " + cleanSpeech)

        return AssistantResponse(
            rawText = rawText,
            actions = actions,
            speech = cleanSpeech,
            detectedLanguage = detectedLanguage
        )
    }

    private fun detectLanguage(text: String): String {
        for (char in text) {
            if (char in '\u0900'..'\u097F') {
                return "Hindi"
            }
        }
        val lower = text.lowercase()
        val hinglishMarkers = listOf(
            "karo", "khol", "laga", "baje", "subah", "hai", "hoon", "aur", "pe", "par",
            "batao", "sunao", "chalao", "kaisa", "kaun", "kya", "ko", "mera", "meri"
        )
        for (marker in hinglishMarkers) {
            if (lower.split(Regex("\\s+")).contains(marker)) {
                return "Hinglish"
            }
        }
        return "English"
    }
}
