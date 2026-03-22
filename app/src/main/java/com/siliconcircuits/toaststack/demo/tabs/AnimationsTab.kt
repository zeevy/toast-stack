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
import com.siliconcircuits.toaststack.SwipeDismissDirection
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastStackState

@OptIn(ExperimentalToastStackApi::class)
@Composable
fun AnimationsTab(toastState: ToastStackState) {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Swipe to Dismiss")
        Spacer(modifier = Modifier.height(4.dp))

        SwipeCard(
            title = "Swipe Both",
            subtitle = "Default - swipe left or right",
            onClick = {
                counter++
                toastState.show(
                    message = "Swipe me either way",
                    duration = ToastDuration.Long,
                    swipeDismiss = SwipeDismissDirection.Both
                )
            }
        )

        SwipeCard(
            title = "Swipe Left Only",
            subtitle = "Can only dismiss by swiping left",
            onClick = {
                counter++
                toastState.show(
                    message = "Swipe me left only",
                    duration = ToastDuration.Long,
                    swipeDismiss = SwipeDismissDirection.Left
                )
            }
        )

        SwipeCard(
            title = "Swipe Right Only",
            subtitle = "Can only dismiss by swiping right",
            onClick = {
                counter++
                toastState.show(
                    message = "Swipe me right only",
                    duration = ToastDuration.Long,
                    swipeDismiss = SwipeDismissDirection.Right
                )
            }
        )

        SwipeCard(
            title = "No Swipe",
            subtitle = "Swipe disabled, use close button",
            onClick = {
                counter++
                toastState.show(
                    message = "Can't swipe me!",
                    duration = ToastDuration.Indefinite,
                    swipeDismiss = SwipeDismissDirection.None,
                    showCloseButton = true
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Entry Animations")
        PlaceholderNote("Slide, fade, scale + fade (Phase 3)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Exit Animations")
        PlaceholderNote("Slide out, fade, shrink + collapse (Phase 3)")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Stack Animations")
        PlaceholderNote("Spring reorder, stagger effect (Phase 3)")

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SwipeCard(
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

@Composable
private fun PlaceholderNote(text: String) {
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
