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
    @Json(name = "temperature") val temperature: Float? = 0.2f,
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
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse

    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContentFallback(
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
You are the AI brain of "JAVA", an advanced Android Voice Assistant. Your ONLY job is to understand natural language (Hindi/English/Hinglish) and output a STRICT RAW JSON ARRAY of logical, executable actions.

CRITICAL RULES (NEVER BREAK THESE):
1. RAW JSON ONLY: Output pure JSON starting with `[` and ending with `]`. NO markdown, NO code blocks (```json), NO explanations.
2. DYNAMIC ENTITY EXTRACTION (BUG FIX): NEVER hardcode names or search queries. If the user says "Search CarryMinati", the query is "CarryMinati". If they say "Message Rahul", the contact is "Rahul". Extract EXACT values from the prompt.
3. MULTI-STEP LOGIC: Chain actions logically. If asked to search on YouTube: OPEN_APP -> CLICK Search -> TYPE_TEXT -> SYSTEM_GESTURE (press_enter). 
4. WHATSAPP/SMS AUTOMATION (BUG FIX): To actually send a message, every SEND_WHATSAPP or SEND_SMS action MUST be immediately followed by: {"action": "CLICK_ON_SCREEN", "target_text": "Send"}.
5. SPEECH CONFIRMATION: Always end the array with a "SPEECH_RESPONSE" detailing what was actually executed.

SUPPORTED ACTIONS & EXACT SCHEMA:

{"action": "OPEN_APP", "app_name": "App Name"}
{"action": "CLICK_ON_SCREEN", "target_text": "Visible text like 'Shorts', 'Send', 'Search'"}
{"action": "TYPE_TEXT", "text": "Exact text to type from user command"}
{"action": "SYSTEM_GESTURE", "type": "scroll_down" | "scroll_up" | "press_enter"}
{"action": "SEND_WHATSAPP", "contact": "Exact Name", "message": "Exact Text"}
{"action": "SEND_SMS", "contact": "Exact Name", "message": "Exact Text"}
{"action": "TOGGLE_SETTING", "setting": "wifi" | "flashlight" | "bluetooth", "state": "on" | "off"}
{"action": "TAKE_SCREENSHOT"}
{"action": "START_SCREEN_RECORDING"}
{"action": "STOP_SCREEN_RECORDING"}
{"action": "LOCK_SCREEN"}
{"action": "ANSWER_CALL"}
{"action": "END_CALL"}
{"action": "SPEECH_RESPONSE", "text": "Short conversational Hindi/Hinglish reply"}

EXAMPLES (OUTPUT MUST BE EXACTLY LIKE THIS FORMAT):

User: "YouTube me CarryMinati search karo"
[
  {"action": "OPEN_APP", "app_name": "YouTube"},
  {"action": "CLICK_ON_SCREEN", "target_text": "Search"},
  {"action": "TYPE_TEXT", "text": "CarryMinati"},
  {"action": "SYSTEM_GESTURE", "type": "press_enter"},
  {"action": "SPEECH_RESPONSE", "text": "CarryMinati search kar diya hai."}
]

User: "WhatsApp par Rohit ko jaldi aao bhejo"
[
  {"action": "SEND_WHATSAPP", "contact": "Rohit", "message": "Jaldi aao"},
  {"action": "CLICK_ON_SCREEN", "target_text": "Send"},
  {"action": "SPEECH_RESPONSE", "text": "Rohit ko message send kar diya hai."}
]

User: "Screen recording start karo aur Shorts chalao"
[
  {"action": "START_SCREEN_RECORDING"},
  {"action": "OPEN_APP", "app_name": "YouTube"},
  {"action": "CLICK_ON_SCREEN", "target_text": "Shorts"},
  {"action": "SPEECH_RESPONSE", "text": "Recording start karke Shorts chala raha hoon."}
]

User: "Instagram kholo aur scroll karo"
[
  {"action": "OPEN_APP", "app_name": "Instagram"},
  {"action": "SYSTEM_GESTURE", "type": "scroll_down"},
  {"action": "SPEECH_RESPONSE", "text": "Instagram open karke scroll kar diya hai."}
]
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

            val response = try {
                api.generateContent(apiKey = apiKey, request = request)
            } catch (e: Exception) {
                Log.w("GeminiAssistantService", "Gemini 2.0 Flash call failed, retrying with 1.5 Flash fallback", e)
                api.generateContentFallback(apiKey = apiKey, request = request)
            }
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext OfflineIntentEngine.parse(prompt)

            parseAssistantOutput(rawText, prompt)
        } catch (e: Exception) {
            Log.e("GeminiAssistantService", "Gemini API error, falling back to offline engine", e)
            OfflineIntentEngine.parse(prompt)
        }
    }

    fun parseAssistantOutput(rawText: String, originalPrompt: String): AssistantResponse {
        val trimmed = rawText.trim()
        // If the model responded with JSON array format
        if (trimmed.contains("[") && trimmed.contains("]")) {
            val parsedResponse = AiJsonParser.parse(trimmed, originalPrompt)
            if (parsedResponse.actions.isNotEmpty() || parsedResponse.speech.isNotBlank()) {
                return parsedResponse
            }
        }

        // Fallback for ACTION:... | ARGS: format or conversational text
        val actions = mutableListOf<JavaAction>()
        var speech = ""

        val lines = rawText.lines()
        for (line in lines) {
            val lineTrimmed = line.trim()
            if (lineTrimmed.startsWith("ACTION:")) {
                val actionPart = lineTrimmed.removePrefix("ACTION:").trim()
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
            } else if (lineTrimmed.startsWith("SPEECH:")) {
                speech = lineTrimmed.removePrefix("SPEECH:").trim()
            }
        }

        if (speech.isBlank()) {
            val nonActionLines = lines.filterNot { it.trim().startsWith("ACTION:") }
            speech = nonActionLines.joinToString(" ").trim()
        }

        val cleanSpeech = speech
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return AssistantResponse(
            rawText = rawText,
            actions = actions,
            speech = cleanSpeech,
            detectedLanguage = "Hinglish"
        )
    }
}
