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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.siliconcircuits.toaststack.ExperimentalToastStackApi
import com.siliconcircuits.toaststack.ToastDuration
import com.siliconcircuits.toaststack.ToastStackState
import com.siliconcircuits.toaststack.ToastStackStyle
import com.siliconcircuits.toaststack.ToastType

@OptIn(ExperimentalToastStackApi::class)
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

        SectionLabel("Typography")
        PlaceholderCard("Custom font and text style overrides")

        Spacer(modifier = Modifier.height(16.dp))

        SectionLabel("Shape")
        PlaceholderCard("Custom corner radius and card shape")

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

@Composable
private fun PlaceholderCard(description: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
