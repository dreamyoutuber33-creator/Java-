package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionCatalog
import com.example.data.model.JavaAction
import com.example.intent.IntentExecutionResult
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OrbBlue
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbIndigo
import com.example.ui.theme.OrbPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@Composable
fun ActionProtocolCard(
    action: JavaAction,
    autoExecuteSecondsLeft: Int?,
    lastResult: IntentExecutionResult?,
    onExecute: (JavaAction) -> Unit,
    onCancelAutoExecute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_protocol_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated.copy(alpha = 0.95f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(OrbIndigo.copy(alpha = 0.5f), OrbPurple.copy(alpha = 0.3f), DarkBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Title & Protocol Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = OrbIndigo.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getActionIcon(action.actionName),
                            contentDescription = null,
                            tint = OrbCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Native Android Intent",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondaryDark,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = action.actionName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = OrbCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp
                            )
                        )
                    }
                }

                // Output Protocol Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = "INTENT PROTOCOL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = OrbBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Exact Structured Protocol String
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = action.toProtocolString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.testTag("action_protocol_string")
                    )
                }
            }

            // Arguments Chips Row
            if (action.args.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    action.args.entries.take(3).forEach { (key, value) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OrbPurple.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrbPurple.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$key: $value",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondaryDark,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Feedback from execution
            if (lastResult != null && lastResult.actionName == action.actionName) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (lastResult.success) Color(0xFF065F46).copy(alpha = 0.35f) else Color(0xFF7F1D1D).copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (lastResult.success) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lastResult.success) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (lastResult.success) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lastResult.userSummary,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (lastResult.success) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Execution Button & Auto-execute countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (autoExecuteSecondsLeft != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(OrbCyan.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = autoExecuteSecondsLeft.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OrbCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-executing…",
                            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondaryDark)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onCancelAutoExecute,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = { onExecute(action) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrbBlue,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("execute_intent_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Execute Intent",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private fun getActionIcon(actionName: String): ImageVector {
    return when (actionName) {
        ActionCatalog.PLAY_STORE_ACTION -> Icons.Default.Shop
        ActionCatalog.OPEN_APP -> Icons.Default.Android
        ActionCatalog.YOUTUBE_SEARCH -> Icons.Default.VideoLibrary
        ActionCatalog.SET_ALARM, ActionCatalog.SET_TIMER -> Icons.Default.Alarm
        ActionCatalog.MAKE_CALL -> Icons.Default.Call
        ActionCatalog.START_NAVIGATION -> Icons.Default.Navigation
        ActionCatalog.GET_WEATHER -> Icons.Default.WbSunny
        ActionCatalog.CAMERA_ACTION -> Icons.Default.CameraAlt
        ActionCatalog.TOGGLE_SETTING -> Icons.Default.FlashlightOn
        ActionCatalog.PLAY_MUSIC -> Icons.Default.PlayCircleFilled
        else -> Icons.Default.Settings
    }
}
