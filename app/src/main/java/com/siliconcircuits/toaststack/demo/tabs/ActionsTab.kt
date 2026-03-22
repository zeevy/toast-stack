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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ExperimentalToastStackApi
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastStackState

@OptIn(ExperimentalToastStackApi::class)
@Composable
fun ActionsTab(toastState: ToastStackState) {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Close Button")
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            onClick = {
                counter++
                toastState.show(
                    message = "Tap the X to dismiss me",
                    duration = ToastDuration.Indefinite,
                    showCloseButton = true
                )
            },
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
                    text = "Indefinite with Close",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Shows close button, no auto dismiss",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Action Buttons")
        PlaceholderSection("Single action button (Undo, Retry, View) (Phase 4)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Progress Toasts")
        PlaceholderSection("Determinate progress bar, loading state (Phase 4)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Lifecycle Callbacks")
        PlaceholderSection("onShow, onDismiss with reason, onAction (Phase 4)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Queueing")
        PlaceholderSection("Priority levels, duplicate detection (Phase 4)")

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PlaceholderSection(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
