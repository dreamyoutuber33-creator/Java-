package com.example.brain

import android.util.Log
import com.example.data.model.ActionCatalog
import com.example.data.model.AssistantResponse
import com.example.data.model.JavaAction
import org.json.JSONArray
import org.json.JSONException

object AiJsonParser {
    private const val TAG = "AiJsonParser"

    /**
     * Safely parses raw LLM output into an AssistantResponse.
     * Sanitizes markdown backtick fences (```json ... ```), trailing commentary,
     * or non-JSON prefixes/suffixes.
     */
    fun parse(rawOutput: String, originalQuery: String): AssistantResponse {
        var cleaned = rawOutput.trim()

        // Strip any markdown fences
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```JSON").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        // Substring between outermost '[' and ']' if surrounded by conversational text
        val startBracket = cleaned.indexOf('[')
        val endBracket = cleaned.lastIndexOf(']')
        if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
            cleaned = cleaned.substring(startBracket, endBracket + 1).trim()
        }

        val actions = mutableListOf<JavaAction>()
        var speechResponse = ""

        try {
            val jsonArray = JSONArray(cleaned)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val actionName = obj.optString("action", "").trim().uppercase()
                if (actionName.isBlank()) continue

                val args = mutableMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key != "action") {
                        args[key] = obj.optString(key, "")
                    }
                }

                if (actionName == ActionCatalog.SPEECH_RESPONSE) {
                    speechResponse = args["text"].orEmpty()
                } else {
                    actions.add(JavaAction(actionName, args))
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to parse AI JSON commands: ${e.message}. Raw: $rawOutput")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing AI commands", e)
        }

        val cleanSpeech = speechResponse
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        return AssistantResponse(
            rawText = rawOutput,
            actions = actions,
            speech = cleanSpeech,
            detectedLanguage = detectLanguage(originalQuery + " " + cleanSpeech)
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
            "batao", "sunao", "chalao", "kaisa", "kaun", "kya", "ko", "mera", "meri", "bolo", "bhejo", "band"
        )
        for (marker in hinglishMarkers) {
            if (lower.split(Regex("\\s+")).contains(marker)) {
                return "Hinglish"
            }
        }
        return "English"
    }
}
