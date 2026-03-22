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
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastStackState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        SectionLabel("Action Buttons")
        Spacer(modifier = Modifier.height(4.dp))

        DemoCard(
            title = "Single Action",
            subtitle = "Toast with an Undo button",
            onClick = {
                counter++
                toastState.show("Item deleted", duration = ToastDuration.Long)
                    .withAction("Undo") {
                        toastState.success("Item restored")
                    }
            }
        )

        DemoCard(
            title = "Chained Callbacks",
            subtitle = "withAction + onDismiss chaining",
            onClick = {
                counter++
                toastState.show("Changes saved", duration = ToastDuration.Long)
                    .withAction("View") {
                        toastState.info("Opening details...")
                    }
                    .onDismiss { reason ->
                        toastState.show(
                            "Dismissed: ${reason.name}",
                            duration = ToastDuration.Short
                        )
                    }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Close Button")
        Spacer(modifier = Modifier.height(4.dp))

        DemoCard(
            title = "Indefinite + Close",
            subtitle = "Stays until close button is tapped",
            onClick = {
                counter++
                toastState.show(
                    "Tap the X to dismiss",
                    duration = ToastDuration.Indefinite,
                    showCloseButton = true
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Loading & Progress")
        Spacer(modifier = Modifier.height(4.dp))

        DemoCard(
            title = "Loading Toast",
            subtitle = "Indeterminate spinner, auto dismiss after 3s",
            onClick = {
                counter++
                val handle = toastState.loading("Processing your request...")
                MainScope().launch {
                    delay(3000)
                    handle.dismiss()
                    toastState.success("Request completed")
                }
            }
        )

        DemoCard(
            title = "Progress Toast",
            subtitle = "Determinate progress bar 0% to 100%",
            onClick = {
                counter++
                val handle = toastState.loading("Uploading files...")
                MainScope().launch {
                    for (i in 1..10) {
                        delay(400)
                        handle.updateProgress(i / 10f, "$i of 10 files")
                    }
                    delay(500)
                    handle.dismiss()
                    toastState.success("Upload complete")
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("Lifecycle")
        Spacer(modifier = Modifier.height(4.dp))

        DemoCard(
            title = "onShow Callback",
            subtitle = "Fires after enter animation completes",
            onClick = {
                counter++
                toastState.show("Watch for the second toast", duration = ToastDuration.Long)
                    .onShow {
                        toastState.info("onShow fired!")
                    }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DemoCard(
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
