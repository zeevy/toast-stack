@file:Suppress("unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER")

package com.siliconcircuits.toaststack.samples

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siliconcircuits.toaststack.DismissReason
import com.siliconcircuits.toaststack.SwipeDismissDirection
import com.siliconcircuits.toaststack.ToastAnimation
import com.siliconcircuits.toaststack.ToastAnimationConfig
import com.siliconcircuits.toaststack.ToastColorSource
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastPosition
import com.siliconcircuits.toaststack.ToastPriority
import com.siliconcircuits.toaststack.ToastStack
import com.siliconcircuits.toaststack.ToastStackState
import com.siliconcircuits.toaststack.ToastStackStyle
import com.siliconcircuits.toaststack.ToastType
import com.siliconcircuits.toaststack.build
import com.siliconcircuits.toaststack.showErrorToast
import com.siliconcircuits.toaststack.showSuccessToast
import com.siliconcircuits.toaststack.showToastAndAwait
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// Every Kotlin sample in README.md, copied as it is, so a sample that stops
// compiling fails the build. A test checks that each README sample is here.
// Names the samples take from the reader's app are stubbed at the bottom.

private fun quickStart() {
    // One liners from anywhere (ViewModel, callback, service)
    ToastStack.success("File saved")
    ToastStack.error("Upload failed")
    ToastStack.warning("Low battery")
    ToastStack.info("Update available")
    ToastStack.show("Plain message")
}

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ToastStack.configure(
            defaultPosition = ToastPosition.TopCenter,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 98.dp,
                bottom = 8.dp,
            ),
            defaultDuration = ToastDuration.Short,
            maxVisible = 5,
            defaultSwipeDismiss = SwipeDismissDirection.Both,
            defaultAnimation = ToastAnimation.Slide,
            defaultAnimationConfig = ToastAnimationConfig(),
            deduplicationWindowMs = 0, // e.g. 3000 to merge repeats into one card
            globalStyle = null, // or a ToastStackStyle for app-wide look
            colorSource = ToastColorSource.AppTheme,
            theme = { content -> MyAppTheme { content() } },
        )
    }
}

private fun chaining() {
    ToastStack.show("Item deleted", duration = ToastDuration.Long)
        ?.withAction("Undo") { restoreItem() }
        ?.onDismiss { reason -> log("dismissed: $reason") }
}

private class SuspendSample : ViewModel() {
    fun sample() {
        viewModelScope.launch {
            val reason = ToastStack.showAndAwait(
                "Confirm delete?",
                showCloseButton = true
            )
            if (reason == DismissReason.Action) undoDelete()
        }
    }
}

private fun titleAndDescription() {
    ToastStack.error(
        "Connection timed out. Please check your network.",
        title = "Network Error"
    )
}

private fun customDuration() {
    ToastStack.show("Gone in 3 seconds", duration = ToastDuration(3.seconds))
    ToastStack.show("Custom millis", duration = ToastDuration.Custom(1500))
}

private fun loadingAndProgress() {
    val handle = ToastStack.loading("Uploading files...")

    // Update progress
    handle?.updateProgress(0.5f, "5 of 10 files")

    // Complete
    handle?.dismiss()
    ToastStack.success("Upload complete")
}

private fun actionButtons() {
    ToastStack.show("Message sent")
        ?.withAction("Undo") {
            unsendMessage()
        }
        ?.withSecondaryAction("View") { openMessage() }
}

private fun customContent() {
    toastState.showCustom(
        duration = ToastDuration.Long,
        showCloseButton = true
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.avatar), contentDescription = null)
            Column {
                Text("Custom Layout", fontWeight = FontWeight.Bold)
                Text("Any composable content you want")
            }
        }
    }
}

private fun builderDsl() {
    toastState.build {
        message = "Connection lost"
        type = ToastType.Error
        duration = ToastDuration.Long
        showCloseButton = true
        hapticEnabled = true
        actionLabel = "Retry"
        onAction = { reconnect() }
        onDismiss = { reason -> log(reason) }
    }
}

class MyViewModel : ViewModel() {
    fun onSaveComplete() {
        showSuccessToast("Document saved")
    }

    fun onError(message: String) {
        showErrorToast(message, title = "Error")
    }

    fun confirmDelete() {
        showToastAndAwait(
            "Item will be deleted",
            showCloseButton = true
        ) { reason ->
            if (reason == DismissReason.Timeout) performDelete()
        }
    }
}

private fun stringResources() {
    ToastStack.show(R.string.file_saved)
    ToastStack.error(R.string.upload_failed, fileName)
}

private fun perToastStyling() {
    ToastStack.show(
        message = "Custom look",
        style = ToastStackStyle(
            backgroundColor = Color(0xFF6A1B9A),
            contentColor = Color.White,
            borderColor = Color.White,
            borderWidth = 2.dp,
            shape = RoundedCornerShape(24.dp)
        )
    )
}

private fun animations() {
    // Per toast animation override
    ToastStack.show(
        "Fade in",
        animation = ToastAnimation.Fade,
        animationConfig = ToastAnimationConfig(
            enterDurationMillis = 500,
            exitDurationMillis = 300
        )
    )
}

private fun priorityQueue() {
    // Normal toasts queue when maxVisible is reached
    ToastStack.show("Queued", priority = ToastPriority.Normal)

    // Urgent toasts bypass the queue and show immediately
    ToastStack.show("Critical!", priority = ToastPriority.Urgent)
}

private fun duplicateDetection() {
    // Global host (auto overlay): set it in Application.onCreate()
    ToastStack.configure(deduplicationWindowMs = 3000)

    // Or on your own state
    val state = ToastStackState(deduplicationWindowMs = 3000)
    state.show("Same message")
    state.show("Same message") // No new card. The first card shows "Same message (x2)"
}

private fun hapticAndSound() {
    ToastStack.show(
        "Feel the vibration",
        hapticEnabled = true,  // Vibrates on appear
        soundEnabled = true    // Plays notification sound
    )
}

// --- Stubs for names from the reader's app ---

private val toastState = ToastStackState()
private val fileName = "report.pdf"
private fun restoreItem() {}
private fun log(message: Any) {}
private fun undoDelete() {}
private fun unsendMessage() {}
private fun openMessage() {}
private fun reconnect() {}
private fun performDelete() {}

@Composable
private fun MyAppTheme(content: @Composable () -> Unit) = content()

private object R {
    object string {
        const val file_saved = 1
        const val upload_failed = 2
    }

    object drawable {
        const val avatar = 3
    }
}
