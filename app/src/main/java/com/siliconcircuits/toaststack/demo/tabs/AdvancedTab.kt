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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ExperimentalToastStackApi
import com.siliconcircuits.toaststack.ToastStackState

@OptIn(ExperimentalToastStackApi::class)
@Composable
fun AdvancedTab(toastState: ToastStackState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Custom Content")
        PlaceholderItem("Arbitrary @Composable lambda as toast content (Phase 5)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Stacking Modes")
        PlaceholderItem("Expanded mode - all toasts visible with spacing (Phase 5)")
        PlaceholderItem("Compact mode - newest visible, older ones peek behind (Phase 5)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Haptic Feedback")
        PlaceholderItem("Vibration on toast appearance per type (Phase 5)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Accessibility")
        PlaceholderItem("TalkBack announcements, reduced motion (Phase 5)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Sound")
        PlaceholderItem("Notification sound per type, respects DND (Phase 5)")

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PlaceholderItem(text: String) {
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
