package com.siliconcircuits.toaststack.demo.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ToastPosition
import com.siliconcircuits.toaststack.ToastStackState

@Composable
fun TypesTab(toastState: ToastStackState) {
    var counter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Tap a type to show it")
        Spacer(modifier = Modifier.height(4.dp))

        TypeCard(
            label = "Success",
            description = "Operation completed",
            icon = Icons.Default.CheckCircle,
            color = Color(0xFF2E7D32),
            onClick = {
                counter++
                toastState.success(
                    "File uploaded successfully",
                    title = "Success"
                )
            }
        )

        TypeCard(
            label = "Error",
            description = "Something went wrong",
            icon = Icons.Default.Close,
            color = Color(0xFFC62828),
            onClick = {
                counter++
                toastState.error(
                    "Connection timed out. Please retry.",
                    title = "Error"
                )
            }
        )

        TypeCard(
            label = "Warning",
            description = "Needs attention",
            icon = Icons.Default.Warning,
            color = Color(0xFFF57F17),
            onClick = {
                counter++
                toastState.warning(
                    "Storage is almost full",
                    title = "Warning"
                )
            }
        )

        TypeCard(
            label = "Info",
            description = "Helpful notice",
            icon = Icons.Default.Info,
            color = Color(0xFF1565C0),
            onClick = {
                counter++
                toastState.info(
                    "A new update is available",
                    title = "Info"
                )
            }
        )

        TypeCard(
            label = "Default",
            description = "Neutral message",
            icon = null,
            color = MaterialTheme.colorScheme.inverseSurface,
            onClick = {
                counter++
                toastState.show(message = "This is a default toast")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

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

// -- Shared composables used across tabs --

@Composable
internal fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TypeCard(
    label: String,
    description: String,
    icon: ImageVector?,
    color: Color,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
