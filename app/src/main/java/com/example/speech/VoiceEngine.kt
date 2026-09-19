package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    var isContinuousListening: Boolean = false
    private var isSpeakingNow: Boolean = false

    // Natural conversational pace and pitch tuning
    var speechRate: Float = 1.05f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }
    var speechPitch: Float = 1.02f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    private val restartRunnable = Runnable {
        if (isContinuousListening && !isSpeakingNow) {
            startListeningInternal()
        }
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

            // Select natural high-quality voice
            selectNaturalVoice(Locale.ENGLISH)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeakingNow = true
                    mainHandler.removeCallbacks(restartRunnable)
                    stopListeningInternal()
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    isSpeakingNow = false
                    onSpeakingStateChanged(false)
                    if (isContinuousListening) {
                        scheduleRestart(450L)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeakingNow = false
                    onSpeakingStateChanged(false)
                    if (isContinuousListening) {
                        scheduleRestart(500L)
                    }
                }
            })
        } else {
            Log.e("VoiceEngine", "TextToSpeech init failed with code $status")
        }
    }

    /**
     * Selects a high-quality, human-like voice, prioritizing network-enhanced voices
     * such as en-in-x-end-network or hi-in-x-hid-network for natural conversational cadence.
     */
    private fun selectNaturalVoice(locale: Locale) {
        val tts = textToSpeech ?: return
        try {
            val voices = tts.voices
            if (!voices.isNullOrEmpty()) {
                val langCode = locale.language.lowercase(Locale.ROOT)

                // 1. Search for high-quality network voice matching locale
                val bestVoice = voices.find { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    val voiceLang = voice.locale.language.lowercase(Locale.ROOT)
                    voiceLang == langCode && (name.contains("network") || name.contains("high_quality"))
                } ?: voices.find { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    name.contains("en-in-x-end-network") ||
                            name.contains("hi-in-x-hid-network") ||
                            name.contains("en-us-x-sfg-network")
                } ?: voices.find { voice ->
                    voice.locale.language.equals(langCode, ignoreCase = true)
                }

                if (bestVoice != null) {
                    tts.voice = bestVoice
                    Log.d("VoiceEngine", "Selected natural TTS voice: ${bestVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error setting natural voice", e)
        }
    }

    fun startContinuousListening() {
        isContinuousListening = true
        mainHandler.removeCallbacks(restartRunnable)
        startListeningInternal()
    }

    fun stopContinuousListening() {
        isContinuousListening = false
        mainHandler.removeCallbacks(restartRunnable)
        stopListeningInternal()
    }

    fun startListening() {
        startContinuousListening()
    }

    fun stopListening() {
        stopContinuousListening()
    }

    private fun startListeningInternal() {
        if (isSpeakingNow) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("VoiceEngine", "Speech recognition not available on device")
            return
        }

        try {
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

            speechRecognizer?.startListening(intent)
            onListeningStateChanged(true)
            _partialText.value = "Listening continuously…"
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error starting speech recognition", e)
            onListeningStateChanged(false)
            if (isContinuousListening && !isSpeakingNow) {
                recreateSpeechRecognizer()
                scheduleRestart(800L)
            }
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error stopping speech recognition", e)
        }
        onListeningStateChanged(false)
        _rmsLevel.value = 0f
    }

    private fun recreateSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognizerListener()
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error recreating SpeechRecognizer", e)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacks(restartRunnable)
        if (isContinuousListening && !isSpeakingNow) {
            mainHandler.postDelayed(restartRunnable, delayMs)
        }
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
                selectNaturalVoice(locale)
            } else {
                textToSpeech?.language = Locale.ENGLISH
                selectNaturalVoice(Locale.ENGLISH)
            }
        } catch (e: Exception) {
            textToSpeech?.language = Locale.ENGLISH
            selectNaturalVoice(Locale.ENGLISH)
        }

        textToSpeech?.setSpeechRate(speechRate)
        textToSpeech?.setPitch(speechPitch)

        val utteranceId = "JavaSpeech_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
            isSpeakingNow = false
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e("VoiceEngine", "Error stopping TTS", e)
        }
    }

    private fun setupRecognizerListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _partialText.value = "Listening continuously for you…"
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
                Log.d("VoiceEngine", "SpeechRecognizer onError: $error")
                _rmsLevel.value = 0f
                onListeningStateChanged(false)
                _partialText.value = ""

                if (isContinuousListening && !isSpeakingNow) {
                    when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        SpeechRecognizer.ERROR_CLIENT -> {
                            recreateSpeechRecognizer()
                            scheduleRestart(800L)
                        }
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            // Immediate loop restart for continuous silence/pause
                            scheduleRestart(250L)
                        }
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        SpeechRecognizer.ERROR_SERVER -> {
                            scheduleRestart(1200L)
                        }
                        else -> {
                            scheduleRestart(500L)
                        }
                    }
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

                // In continuous listening mode, restart listener if not speaking
                if (isContinuousListening && !isSpeakingNow) {
                    scheduleRestart(350L)
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
            isContinuousListening = false
            mainHandler.removeCallbacksAndMessages(null)
            speechRecognizer?.cancel()
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
