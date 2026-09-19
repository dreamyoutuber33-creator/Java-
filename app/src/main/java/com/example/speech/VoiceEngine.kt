package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    var isContinuousListening: Boolean = false
    var speechRate: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }
    var speechPitch: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    init {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.language = Locale.ENGLISH
            textToSpeech?.setSpeechRate(speechRate)
            textToSpeech?.setPitch(speechPitch)
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }
            })
        } else {
            Log.e("VoiceEngine", "TextToSpeech init failed with code $status")
        }
    }

    fun startListening() {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceEngine", "Speech recognition not available on device")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognizerListener()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Support Hinglish / Indian English / Hindi recognition
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-US"))
        }

        try {
            speechRecognizer?.startListening(intent)
            onListeningStateChanged(true)
            _partialText.value = "Listening…"
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error starting speech recognition", e)
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error stopping speech recognition", e)
        }
        onListeningStateChanged(false)
        _rmsLevel.value = 0f
    }

    fun speak(text: String, language: String = "English") {
        if (!isTtsInitialized || textToSpeech == null) {
            Log.w("VoiceEngine", "TTS not ready yet")
            return
        }

        stopSpeaking()

        // Configure voice locale matching language
        val locale = when (language) {
            "Hindi" -> Locale.forLanguageTag("hi-IN")
            "Hinglish" -> Locale.forLanguageTag("en-IN")
            else -> Locale.ENGLISH
        }
        try {
            val availability = textToSpeech?.isLanguageAvailable(locale)
            if (availability != TextToSpeech.LANG_MISSING_DATA && availability != TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = locale
            } else {
                textToSpeech?.language = Locale.ENGLISH
            }
        } catch (e: Exception) {
            textToSpeech?.language = Locale.ENGLISH
        }

        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(speechPitch)

        val utteranceId = "JavaSpeech_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error stopping TTS", e)
        }
    }

    private fun setupRecognizerListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _partialText.value = "Listening for you…"
            }

            override fun onBeginningOfSpeech() {
                _partialText.value = "I hear you…"
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsLevel.value = ((rmsdB + 2f).coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _rmsLevel.value = 0f
                onListeningStateChanged(false)
            }

            override fun onError(error: Int) {
                _rmsLevel.value = 0f
                onListeningStateChanged(false)
                _partialText.value = ""
                if (isContinuousListening && error != SpeechRecognizer.ERROR_CLIENT) {
                    // Automatically restart in continuous mode if desired
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull().orEmpty().trim()
                _rmsLevel.value = 0f
                onListeningStateChanged(false)
                _partialText.value = ""
                if (recognized.isNotBlank()) {
                    onSpeechRecognized(recognized)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull().orEmpty()
                if (partial.isNotBlank()) {
                    _partialText.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error destroying VoiceEngine", e)
        }
    }
}
