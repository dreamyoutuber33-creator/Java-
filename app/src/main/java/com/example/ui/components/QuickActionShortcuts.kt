package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OrbCyan
import com.example.ui.theme.OrbPurple
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

data class QuickPrompt(
    val title: String,
    val prompt: String,
    val language: String
)

@Composable
fun QuickActionShortcuts(
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePrompts = listOf(
        QuickPrompt("Play Store WhatsApp", "Hey Java, Play Store par WhatsApp search karo", "Hinglish"),
        QuickPrompt("Play Store Open", "Hey Java, Play Store open karo", "Hinglish"),
        QuickPrompt("YouTube Guruji", "Hey Java, YouTube open karo aur technical guruji search karo", "Hinglish"),
        QuickPrompt("Alarm 6 AM", "Hey Java, kal subah 6 baje ka alarm laga do", "Hinglish"),
        QuickPrompt("Flashlight", "Hey Java, turn on flashlight", "English"),
        QuickPrompt("Calculator", "Hey Java, calculate 450 * 12", "English"),
        QuickPrompt("Weather Delhi", "Hey Java, Delhi ka mausam kaisa hai", "Hinglish"),
        QuickPrompt("Navigate", "Hey Java, navigate to India Gate", "English"),
        QuickPrompt("Spotify Music", "Hey Java, play Arijit Singh on Spotify", "English"),
        QuickPrompt("Who Are You?", "Hey Java, tum kaun ho?", "Hinglish")
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "TRY ASKING JAVA",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondaryDark,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_action_row")
        ) {
            items(samplePrompts) { item ->
                Surface(
                    onClick = { onSelectPrompt(item.prompt) },
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.testTag("quick_chip_${item.title.replace(" ", "_")}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = OrbCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.prompt,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
