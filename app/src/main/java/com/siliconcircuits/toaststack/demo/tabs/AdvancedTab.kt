package com.siliconcircuits.toaststack.demo.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ExperimentalToastStackApi
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastStackState
import com.siliconcircuits.toaststack.ToastStackStyle

@OptIn(ExperimentalToastStackApi::class)
@Composable
fun AdvancedTab(toastState: ToastStackState) {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Custom Content")
        Spacer(modifier = Modifier.height(4.dp))

        AdvCard(
            title = "Custom Layout",
            subtitle = "Fully custom composable inside toast",
            onClick = {
                counter++
                toastState.showCustom(
                    duration = ToastDuration.Long,
                    style = ToastStackStyle(
                        backgroundColor = Color(0xFF1B5E20)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🎉",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Custom Toast #$counter",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "This is a fully custom layout",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        )

        AdvCard(
            title = "Custom with Close",
            subtitle = "Custom content + swipe + close button",
            onClick = {
                counter++
                toastState.showCustom(
                    duration = ToastDuration.Indefinite,
                    showCloseButton = true
                ) {
                    Text(
                        "Tap X or swipe to dismiss this custom toast",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Haptic & Sound")
        Spacer(modifier = Modifier.height(4.dp))

        AdvCard(
            title = "Haptic Feedback",
            subtitle = "Vibrates when toast appears",
            onClick = {
                counter++
                toastState.show(
                    message = "Feel the vibration!",
                    title = "Haptic #$counter",
                    duration = ToastDuration.Short,
                    hapticEnabled = true
                )
            }
        )

        AdvCard(
            title = "Sound Alert",
            subtitle = "Plays notification sound on appear",
            onClick = {
                counter++
                toastState.show(
                    message = "Listen for the sound!",
                    title = "Sound #$counter",
                    duration = ToastDuration.Short,
                    soundEnabled = true
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Accessibility")
        Spacer(modifier = Modifier.height(4.dp))

        AdvCard(
            title = "Type Announcement",
            subtitle = "TalkBack says 'Error notification: ...'",
            onClick = {
                counter++
                toastState.error(
                    "Connection failed. Check your network.",
                    title = "Network Error"
                )
            }
        )

        AdvCard(
            title = "Reduced Motion",
            subtitle = "Enable 'Remove animations' in system settings to test",
            onClick = {
                counter++
                toastState.info(
                    "Animations will simplify to a short fade if 'Remove animations' is enabled in Accessibility settings"
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AdvCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
