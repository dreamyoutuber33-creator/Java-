package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.brain.OfflineIntentEngine
import com.example.intent.NativeIntentExecutor
import com.example.speech.VoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JavaBackgroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceEngine: VoiceEngine? = null
    private var intentExecutor: NativeIntentExecutor? = null

    override fun onCreate() {
        super.onCreate()
        intentExecutor = NativeIntentExecutor(applicationContext)
        createNotificationChannel()

        voiceEngine = VoiceEngine(
            context = applicationContext,
            onSpeechRecognized = { recognizedText ->
                handleBackgroundSpeech(recognizedText)
            },
            onListeningStateChanged = { isListening ->
                _isListeningState.value = isListening
                updateNotification(if (isListening) "Listening hands-free…" else "Standing by for “Hey Java”")
            },
            onSpeakingStateChanged = { isSpeaking ->
                _isSpeakingState.value = isSpeaking
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopBackgroundService()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_LISTENING -> {
                voiceEngine?.startListening()
            }
            ACTION_START -> {
                startForegroundWithNotification()
                _isRunning.value = true
                voiceEngine?.isContinuousListening = true
                voiceEngine?.startListening()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Java Assistant Active • Ready hands-free")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun handleBackgroundSpeech(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        serviceScope.launch {
            _lastCommandText.value = trimmed
            updateNotification("Processing: “$trimmed”")

            // Process query via offline engine for zero-latency background execution
            val response = OfflineIntentEngine.parse(trimmed)

            // Speak response
            voiceEngine?.speak(response.speech, response.detectedLanguage)

            // Execute any structured intent
            if (response.actions.isNotEmpty()) {
                for (action in response.actions) {
                    val result = intentExecutor?.execute(action)
                    updateNotification(result?.userSummary ?: response.speech)
                }
            } else {
                updateNotification(response.speech)
            }
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerIntent = Intent(this, JavaBackgroundService::class.java).apply {
            action = ACTION_TRIGGER_LISTENING
        }
        val triggerPendingIntent = PendingIntent.getService(
            this,
            1,
            triggerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JavaBackgroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Java Voice Assistant")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "Ask Java", triggerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildNotification(statusText)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Java Voice Assistant Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Java voice recognition and hands-free intents active in the background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopBackgroundService() {
        _isRunning.value = false
        _isListeningState.value = false
        voiceEngine?.stopListening()
        voiceEngine?.stopSpeaking()
        voiceEngine?.destroy()
        voiceEngine = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceScope.cancel()
        voiceEngine?.destroy()
        voiceEngine = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "java_assistant_background_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TRIGGER_LISTENING = "com.example.service.ACTION_TRIGGER_LISTENING"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isListeningState = MutableStateFlow(false)
        val isListeningState: StateFlow<Boolean> = _isListeningState.asStateFlow()

        private val _isSpeakingState = MutableStateFlow(false)
        val isSpeakingState: StateFlow<Boolean> = _isSpeakingState.asStateFlow()

        private val _lastCommandText = MutableStateFlow("")
        val lastCommandText: StateFlow<String> = _lastCommandText.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, JavaBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JavaBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerListening(context: Context) {
            val intent = Intent(context, JavaBackgroundService::class.java).apply {
                action = ACTION_TRIGGER_LISTENING
            }
            context.startService(intent)
        }
    }
}
