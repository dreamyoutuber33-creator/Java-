package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantState
import com.example.ui.components.ActionProtocolCard
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.LiveCaptionsCard
import com.example.ui.components.QuickActionShortcuts
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SiriVoiceOrb
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OrbBlue
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbMagenta
import com.example.ui.theme.OrbPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var textInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    // Audio permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onVoiceOrbClick()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Microphone permission is needed for hands-free voice commands.")
            }
        }
    }

    // Notification permission request launcher for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.toggleBackgroundService(true)
    }

    val toggleBackgroundService = { enable: Boolean ->
        if (enable) {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotif = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasNotif) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.toggleBackgroundService(true)
                }
            } else {
                viewModel.toggleBackgroundService(true)
            }
        } else {
            viewModel.toggleBackgroundService(false)
        }
    }

    val requestVoiceInteraction = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.onVoiceOrbClick()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = DarkCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle ambient diagonal glow in background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                OrbPurple.copy(alpha = 0.12f),
                                OrbBlue.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(OrbCyan, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Java",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = TextPrimaryDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "“Hey Java” Voice Assistant",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Background Hands-Free Service chip
                        Surface(
                            onClick = { toggleBackgroundService(!uiState.isBackgroundServiceActive) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (uiState.isBackgroundServiceActive) Color(0xFF064E3B) else DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.isBackgroundServiceActive) Color(0xFF10B981) else DarkBorder
                            ),
                            modifier = Modifier.testTag("background_service_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = if (uiState.isBackgroundServiceActive) Color(0xFF34D399) else Color(0xFF64748B),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (uiState.isBackgroundServiceActive) "BG ON" else "BG OFF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (uiState.isBackgroundServiceActive) Color(0xFF6EE7B7) else TextSecondaryDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { showHistory = true },
                            modifier = Modifier.testTag("history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Interaction history",
                                tint = OrbCyan
                            )
                        }
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondaryDark
                            )
                        }
                    }
                }

                // Scrollable Content Area (Orb, Live Captions, Structured Actions)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Siri Voice Orb
                    SiriVoiceOrb(
                        state = uiState.state,
                        audioRms = uiState.audioRms,
                        onClick = requestVoiceInteraction,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Assistant State Subtitle
                    Text(
                        text = when (uiState.state) {
                            AssistantState.LISTENING -> "Listening… Say a command or “Hey Java”"
                            AssistantState.PROCESSING -> "Processing with Java…"
                            AssistantState.SPEAKING -> "Java is speaking…"
                            AssistantState.ERROR -> uiState.errorMessage ?: "Something went wrong"
                            else -> "Tap the orb or say “Hey Java”"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = when (uiState.state) {
                                AssistantState.LISTENING -> OrbCyan
                                AssistantState.SPEAKING -> OrbMagenta
                                AssistantState.ERROR -> Color(0xFFEF4444)
                                else -> TextSecondaryDark
                            },
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.testTag("assistant_state_label")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Animated Sound Wave Visualizer providing immediate audio feedback
                    SoundWaveVisualizer(
                        state = uiState.state,
                        audioRms = uiState.audioRms,
                        onClick = requestVoiceInteraction,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Captions Card (1-2 sentences concise spoken response)
                    LiveCaptionsCard(
                        state = uiState.state,
                        userQuery = uiState.currentUtterance,
                        partialSpeech = uiState.partialText,
                        speechOutput = uiState.currentResponse?.speech.orEmpty(),
                        detectedLanguage = uiState.currentResponse?.detectedLanguage.orEmpty(),
                        onReplaySpeech = { viewModel.replaySpeech() },
                        onStopSpeaking = { viewModel.stopSpeaking() }
                    )

                    // Action Protocol Card (if actionable intent was identified)
                    val actions = uiState.currentResponse?.actions.orEmpty()
                    if (actions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        for (action in actions) {
                            ActionProtocolCard(
                                action = action,
                                autoExecuteSecondsLeft = uiState.autoExecuteSecondsLeft,
                                lastResult = uiState.lastExecutionResult,
                                onExecute = { viewModel.executeAction(it) },
                                onCancelAutoExecute = { viewModel.cancelAutoExecute() }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Action Shortcuts Horizontal Row
                QuickActionShortcuts(
                    onSelectPrompt = { prompt ->
                        viewModel.handleSpeechInput(prompt)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Text Input Prompt Bar (fallback / silent mode)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = requestVoiceInteraction,
                            modifier = Modifier.testTag("mic_input_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.state == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice input",
                                tint = if (uiState.state == AssistantState.LISTENING) OrbMagenta else OrbCyan
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = "Ask Java (e.g., Play Store open karo)",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryDark, fontSize = 14.sp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.handleSpeechInput(textInput)
                                        textInput = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_input_field")
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.handleSpeechInput(textInput)
                                    textInput = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send request",
                                tint = if (textInput.isNotBlank()) OrbCyan else TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Dialogs
            if (showSettings) {
                SettingsDialog(
                    isAutoExecuteEnabled = uiState.isAutoExecuteEnabled,
                    isBackgroundServiceActive = uiState.isBackgroundServiceActive,
                    speechRate = uiState.speechRate,
                    speechPitch = uiState.speechPitch,
                    currentApiKey = uiState.apiKey,
                    onToggleAutoExecute = { viewModel.toggleAutoExecute(it) },
                    onToggleBackgroundService = toggleBackgroundService,
                    onSpeechRateChange = { viewModel.setSpeechRate(it) },
                    onSpeechPitchChange = { viewModel.setSpeechPitch(it) },
                    onUpdateApiKey = { viewModel.updateApiKey(it) },
                    onClearHistory = { viewModel.clearHistory() },
                    onDismiss = { showSettings = false }
                )
            }

            if (showHistory) {
                HistoryBottomSheet(
                    history = uiState.history,
                    onReplay = { speech, lang ->
                        viewModel.voiceEngine.speak(speech, lang)
                    },
                    onDismiss = { showHistory = false }
                )
            }
        }
    }
}
