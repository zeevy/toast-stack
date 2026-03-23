package com.siliconcircuits.toaststack.demo.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastStackState
import com.siliconcircuits.toaststack.ToastStackStyle
import com.siliconcircuits.toaststack.ToastType
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StyleTab(toastState: ToastStackState) {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Custom Colors")
        Spacer(modifier = Modifier.height(4.dp))

        StyleDemoCard(
            title = "Purple",
            subtitle = "Custom background color",
            onClick = {
                counter++
                toastState.show(
                    message = "Purple styled toast",
                    title = "Custom #$counter",
                    type = ToastType.Success,
                    style = ToastStackStyle(
                        backgroundColor = Color(0xFF6A1B9A),
                        contentColor = Color.White
                    )
                )
            }
        )

        StyleDemoCard(
            title = "Teal",
            subtitle = "Custom background + icon tint",
            onClick = {
                counter++
                toastState.show(
                    message = "Teal styled toast",
                    title = "Custom #$counter",
                    type = ToastType.Info,
                    style = ToastStackStyle(
                        backgroundColor = Color(0xFF00695C),
                        contentColor = Color.White,
                        iconTint = Color(0xFFB2DFDB)
                    )
                )
            }
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        StyleDemoCard(
            title = "Bordered",
            subtitle = "With border stroke",
            onClick = {
                counter++
                toastState.show(
                    message = "Toast with a border",
                    title = "Bordered #$counter",
                    duration = ToastDuration.Long,
                    style = ToastStackStyle(
                        borderColor = primaryColor,
                        borderWidth = 2.dp
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Custom Duration")
        Spacer(modifier = Modifier.height(4.dp))

        StyleDemoCard(
            title = "3 Second Toast",
            subtitle = "Using ToastDuration.Custom(3000)",
            onClick = {
                counter++
                toastState.show(
                    message = "Visible for exactly 3 seconds",
                    title = "Custom Duration #$counter",
                    duration = ToastDuration.Custom(3000)
                )
            }
        )

        StyleDemoCard(
            title = "Half Second Flash",
            subtitle = "Using ToastDuration(500.milliseconds)",
            onClick = {
                counter++
                toastState.show(
                    message = "Gone in a flash!",
                    duration = ToastDuration(500.milliseconds)
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Chaining API")
        Spacer(modifier = Modifier.height(4.dp))

        StyleDemoCard(
            title = "Handle Dismiss",
            subtitle = "Dismiss via ToastHandle after 2s",
            onClick = {
                counter++
                val handle = toastState.show(
                    message = "I'll be dismissed by code in 2s",
                    title = "Handle #$counter",
                    duration = ToastDuration.Indefinite
                )
                // Dismiss after a delay using the handle.
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(2000)
                    handle.dismiss()
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StyleDemoCard(
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
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
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
