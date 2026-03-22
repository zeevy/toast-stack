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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ExperimentalToastStackApi
import com.siliconcircuits.toaststack.SwipeDismissDirection
import com.siliconcircuits.toaststack.ToastAnimation
import com.siliconcircuits.toaststack.ToastAnimationConfig
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

        SectionLabel("Entry / Exit Animations")
        Spacer(modifier = Modifier.height(4.dp))

        AnimCard(
            title = "Slide (default)",
            subtitle = "Slides in from edge with fade",
            onClick = {
                counter++
                toastState.show(
                    message = "Slide animation #$counter",
                    duration = ToastDuration.Long,
                    animation = ToastAnimation.Slide
                )
            }
        )

        AnimCard(
            title = "Fade",
            subtitle = "Fades in from transparent",
            onClick = {
                counter++
                toastState.show(
                    message = "Fade animation #$counter",
                    duration = ToastDuration.Long,
                    animation = ToastAnimation.Fade
                )
            }
        )

        AnimCard(
            title = "Scale + Fade",
            subtitle = "Scales up from 80% with fade",
            onClick = {
                counter++
                toastState.show(
                    message = "Scale animation #$counter",
                    duration = ToastDuration.Long,
                    animation = ToastAnimation.ScaleAndFade
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Custom Timing")
        Spacer(modifier = Modifier.height(4.dp))

        AnimCard(
            title = "Slow Enter (800ms)",
            subtitle = "Deliberately slow entrance",
            onClick = {
                counter++
                toastState.show(
                    message = "Slow entrance #$counter",
                    duration = ToastDuration.Long,
                    animation = ToastAnimation.Slide,
                    animationConfig = ToastAnimationConfig(
                        enterDurationMillis = 800
                    )
                )
            }
        )

        AnimCard(
            title = "Fast Snap (100ms)",
            subtitle = "Near instant appear and disappear",
            onClick = {
                counter++
                toastState.show(
                    message = "Fast snap #$counter",
                    duration = ToastDuration.Long,
                    animation = ToastAnimation.Slide,
                    animationConfig = ToastAnimationConfig(
                        enterDurationMillis = 100,
                        exitDurationMillis = 100
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Swipe to Dismiss")
        Spacer(modifier = Modifier.height(4.dp))

        AnimCard(
            title = "Swipe Both (default)",
            subtitle = "Swipe left or right, or fast flick",
            onClick = {
                counter++
                toastState.show(
                    message = "Swipe me either way",
                    duration = ToastDuration.Long,
                    swipeDismiss = SwipeDismissDirection.Both
                )
            }
        )

        AnimCard(
            title = "Swipe Left Only",
            subtitle = "Right swipes are ignored",
            onClick = {
                counter++
                toastState.show(
                    message = "Swipe me left only",
                    duration = ToastDuration.Long,
                    swipeDismiss = SwipeDismissDirection.Left
                )
            }
        )

        AnimCard(
            title = "Swipe Right Only",
            subtitle = "Left swipes are ignored",
            onClick = {
                counter++
                toastState.show(
                    message = "Swipe me right only",
                    duration = ToastDuration.Long,
                    swipeDismiss = SwipeDismissDirection.Right
                )
            }
        )

        AnimCard(
            title = "No Swipe",
            subtitle = "Swipe disabled, close button only",
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

        SectionLabel("Stack Reflow")
        Spacer(modifier = Modifier.height(4.dp))

        AnimCard(
            title = "Show 3 Toasts",
            subtitle = "Watch them stack with stagger effect",
            onClick = {
                counter++
                toastState.show(
                    message = "First toast #$counter",
                    duration = ToastDuration.Long,
                    showCloseButton = true
                )
                toastState.show(
                    message = "Second toast #$counter",
                    duration = ToastDuration.Long,
                    showCloseButton = true
                )
                toastState.show(
                    message = "Third toast #$counter",
                    duration = ToastDuration.Long,
                    showCloseButton = true
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AnimCard(
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
