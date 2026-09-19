package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.brain.GeminiAssistantService
import com.example.data.model.AssistantResponse
import com.example.data.model.AssistantState
import com.example.data.model.ConversationTurn
import com.example.data.model.JavaAction
import com.example.intent.IntentExecutionResult
import com.example.intent.NativeIntentExecutor
import com.example.service.JavaBackgroundService
import com.example.speech.VoiceEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val state: AssistantState = AssistantState.IDLE,
    val currentUtterance: String = "",
    val currentResponse: AssistantResponse? = null,
    val lastExecutionResult: IntentExecutionResult? = null,
    val autoExecuteSecondsLeft: Int? = null,
    val isAutoExecuteEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val audioRms: Float = 0f,
    val partialText: String = "",
    val history: List<ConversationTurn> = emptyList(),
    val errorMessage: String? = null,
    val apiKey: String = "",
    val isMasterActive: Boolean = false,
    val allowBackgroundExecution: Boolean = true,
    val isBackgroundServiceActive: Boolean = false,
    val isVoiceAssistantActive: Boolean = false
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        AssistantUiState(
            apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        )
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val intentExecutor = NativeIntentExecutor(application.applicationContext)
    private val geminiService = GeminiAssistantService()

    val voiceEngine = VoiceEngine(
        context = application.applicationContext,
        onSpeechRecognized = { recognizedText ->
            handleSpeechInput(recognizedText)
        },
        onListeningStateChanged = { isListening ->
            _uiState.update {
                it.copy(
                    state = if (isListening) AssistantState.LISTENING else if (it.state == AssistantState.LISTENING) AssistantState.IDLE else it.state
                )
            }
        },
        onSpeakingStateChanged = { isSpeaking ->
            _uiState.update {
                it.copy(
                    state = if (isSpeaking) AssistantState.SPEAKING else if (it.state == AssistantState.SPEAKING) AssistantState.IDLE else it.state
                )
            }
        }
    )

    private var autoExecuteJob: Job? = null

    init {
        viewModelScope.launch {
            voiceEngine.rmsLevel.collect { rms ->
                _uiState.update { it.copy(audioRms = rms) }
            }
        }
        viewModelScope.launch {
            voiceEngine.partialText.collect { partial ->
                _uiState.update { it.copy(partialText = partial) }
            }
        }
        viewModelScope.launch {
            JavaBackgroundService.isRunning.collect { running ->
                _uiState.update { it.copy(isBackgroundServiceActive = running) }
            }
        }

        val prefs = application.getSharedPreferences("java_assistant_prefs", Context.MODE_PRIVATE)
        val savedMaster = prefs.getBoolean("pref_master_switch", false)
        val savedAllowBg = prefs.getBoolean("pref_allow_background_execution", true)

        _uiState.update {
            it.copy(
                isMasterActive = savedMaster,
                allowBackgroundExecution = savedAllowBg,
                isVoiceAssistantActive = savedMaster
            )
        }

        if (savedMaster) {
            if (savedAllowBg) {
                JavaBackgroundService.start(application)
            } else {
                voiceEngine.startContinuousListening()
            }
        }
    }

    fun onVoiceOrbClick() {
        if (!_uiState.value.isMasterActive) {
            // Turn on Master continuous listening
            setMasterActive(true)
        } else {
            when (_uiState.value.state) {
                AssistantState.SPEAKING -> {
                    voiceEngine.stopSpeaking()
                }
                else -> {
                    // Quick toggle Master OFF
                    setMasterActive(false)
                }
            }
        }
    }

    fun requestVoiceInput() {
        if (!_uiState.value.isMasterActive) {
            setMasterActive(true)
        }
    }

    fun handleSpeechInput(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // Strip wake word trigger if present ("Hey Java" or "Java")
        val processedInput = stripWakeWord(trimmed)
        processQuery(processedInput)
    }

    fun processQuery(userQuery: String) {
        if (userQuery.isBlank()) return

        cancelAutoExecute()
        voiceEngine.stopSpeaking()

        _uiState.update {
            it.copy(
                state = AssistantState.PROCESSING,
                currentUtterance = userQuery,
                errorMessage = null,
                lastExecutionResult = null
            )
        }

        viewModelScope.launch {
            try {
                val apiKey = _uiState.value.apiKey
                val response = geminiService.query(userQuery, apiKey)

                _uiState.update {
                    it.copy(
                        currentResponse = response,
                        state = AssistantState.SPEAKING
                    )
                }

                // Vocalize Java's spoken line
                if (response.speech.isNotBlank()) {
                    voiceEngine.speak(response.speech, response.detectedLanguage)
                }

                // Record turn in history
                val turn = ConversationTurn(
                    userQuery = userQuery,
                    response = response
                )
                _uiState.update {
                    it.copy(history = listOf(turn) + it.history)
                }

                // If actionable intent exists and auto-execute is enabled, start countdown
                if (response.actions.isNotEmpty() && _uiState.value.isAutoExecuteEnabled) {
                    startAutoExecuteCountdown(response.actions.first())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        state = AssistantState.ERROR,
                        errorMessage = e.message ?: "Failed to process request"
                    )
                }
            }
        }
    }

    private fun startAutoExecuteCountdown(action: JavaAction) {
        autoExecuteJob?.cancel()
        autoExecuteJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _uiState.update { it.copy(autoExecuteSecondsLeft = sec) }
                delay(1000)
            }
            _uiState.update { it.copy(autoExecuteSecondsLeft = null) }
            executeAction(action, isAuto = true)
        }
    }

    fun cancelAutoExecute() {
        autoExecuteJob?.cancel()
        autoExecuteJob = null
        _uiState.update { it.copy(autoExecuteSecondsLeft = null) }
    }

    fun executeAction(action: JavaAction, isAuto: Boolean = false) {
        cancelAutoExecute()
        val result = intentExecutor.execute(action)
        _uiState.update { current ->
            val updatedHistory = current.history.map { turn ->
                if (turn.response.actions.contains(action)) {
                    turn.copy(
                        isAutoExecuted = isAuto,
                        executionResult = result.userSummary
                    )
                } else turn
            }
            current.copy(
                lastExecutionResult = result,
                history = updatedHistory
            )
        }
    }

    fun replaySpeech() {
        val speech = _uiState.value.currentResponse?.speech.orEmpty()
        val lang = _uiState.value.currentResponse?.detectedLanguage ?: "English"
        if (speech.isNotBlank()) {
            voiceEngine.speak(speech, lang)
        }
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
        _uiState.update { it.copy(state = AssistantState.IDLE) }
    }

    fun toggleAutoExecute(enabled: Boolean) {
        _uiState.update { it.copy(isAutoExecuteEnabled = enabled) }
        if (!enabled) cancelAutoExecute()
    }

    fun setMasterActive(enable: Boolean) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("java_assistant_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_master_switch", enable).apply()

        _uiState.update {
            it.copy(
                isMasterActive = enable,
                isVoiceAssistantActive = enable
            )
        }

        if (enable) {
            if (_uiState.value.allowBackgroundExecution) {
                voiceEngine.stopContinuousListening()
                JavaBackgroundService.start(app)
            } else {
                JavaBackgroundService.stop(app)
                voiceEngine.startContinuousListening()
            }
        } else {
            // Completely stop SpeechRecognizer, destroy service, and release microphone
            JavaBackgroundService.stop(app)
            voiceEngine.stopContinuousListening()
            voiceEngine.stopSpeaking()
            _uiState.update { it.copy(state = AssistantState.IDLE, partialText = "") }
        }
    }

    fun setAllowBackgroundExecution(allow: Boolean) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("java_assistant_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pref_allow_background_execution", allow).apply()

        _uiState.update { it.copy(allowBackgroundExecution = allow) }

        if (_uiState.value.isMasterActive) {
            if (allow) {
                voiceEngine.stopContinuousListening()
                JavaBackgroundService.start(app)
            } else {
                JavaBackgroundService.stop(app)
                voiceEngine.startContinuousListening()
            }
        }
    }

    fun onActivityResume() {
        // If Master Switch is ON but Background Execution is DISABLED, start/resume listening while Activity is visible
        if (_uiState.value.isMasterActive && !_uiState.value.allowBackgroundExecution) {
            voiceEngine.startContinuousListening()
        }
    }

    fun onActivityPause() {
        // If Master Switch is ON but Background Execution is DISABLED, stop listening when Activity is not visible
        if (_uiState.value.isMasterActive && !_uiState.value.allowBackgroundExecution) {
            voiceEngine.stopContinuousListening()
            _uiState.update { it.copy(state = AssistantState.IDLE) }
        }
    }

    fun toggleVoiceAssistant(enable: Boolean) {
        setMasterActive(enable)
    }

    fun toggleBackgroundService(enable: Boolean) {
        setAllowBackgroundExecution(enable)
    }

    fun setSpeechRate(rate: Float) {
        voiceEngine.speechRate = rate
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        voiceEngine.speechPitch = pitch
        _uiState.update { it.copy(speechPitch = pitch) }
    }

    fun updateApiKey(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey.trim()) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    private fun stripWakeWord(text: String): String {
        var clean = text
        val patterns = listOf(
            Regex("^(hey\\s+java|java|namaste\\s+java|sun\\s+java|o\\s+java)[,\\s]*", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            clean = clean.replace(pattern, "").trim()
        }
        return if (clean.isBlank()) text else clean
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
    }
}
