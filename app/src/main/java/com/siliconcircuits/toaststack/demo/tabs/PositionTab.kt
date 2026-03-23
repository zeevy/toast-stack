package com.siliconcircuits.toaststack.demo.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastPosition
import com.siliconcircuits.toaststack.ToastStackState

@Composable
fun PositionTab(toastState: ToastStackState) {
    var counter by remember { mutableIntStateOf(0) }

    val topRow = listOf(
        ToastPosition.TopStart to "Top Start",
        ToastPosition.TopCenter to "Top Center",
        ToastPosition.TopEnd to "Top End"
    )
    val bottomRow = listOf(
        ToastPosition.BottomStart to "Bottom Start",
        ToastPosition.BottomCenter to "Bottom Center",
        ToastPosition.BottomEnd to "Bottom End"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Tap a position to show a toast there")

        Spacer(modifier = Modifier.height(12.dp))

        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topRow.forEach { (position, label) ->
                PositionButton(
                    label = label,
                    onClick = {
                        counter++
                        toastState.show(
                            message = "$label toast #$counter",
                            position = position,
                            duration = ToastDuration.Long
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PositionButton(
                label = "Center",
                onClick = {
                    counter++
                    toastState.show(
                        message = "Center toast #$counter",
                        position = ToastPosition.Center,
                        duration = ToastDuration.Long
                    )
                },
                modifier = Modifier.fillMaxWidth(0.34f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bottomRow.forEach { (position, label) ->
                PositionButton(
                    label = label,
                    onClick = {
                        counter++
                        toastState.show(
                            message = "$label toast #$counter",
                            position = position,
                            duration = ToastDuration.Long
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        FilledTonalButton(
            onClick = { toastState.dismissAll() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Dismiss All", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PositionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
