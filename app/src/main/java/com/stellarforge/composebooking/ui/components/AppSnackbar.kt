package com.stellarforge.composebooking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enum defining the visual style of the notification.
 */
enum class SnackbarType {
    SUCCESS, ERROR, INFO
}

/**
 * A customized [SnackbarHost] implementation that renders a styled card based on the message type.
 *
 * **Integration:**
 * Pass this composable to the `snackbarHost` parameter of a [Scaffold].
 *
 * **How to trigger styles:**
 * When calling `snackbarHostState.showSnackbar`, use the `actionLabel` parameter to specify the type:
 * - `actionLabel = "error"` -> Renders Red styling (Critical Failure)
 * - `actionLabel = "success"` -> Renders Green styling (Operation Complete)
 * - `actionLabel = null` -> Renders Default Dark Grey styling (Information)
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState
) {
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier
            .padding(16.dp) // Margin from screen edges
            .imePadding() // Moves up when keyboard opens
    ) { data ->
        // Logic: Map the 'actionLabel' string to a strongly typed Enum
        val type = when (data.visuals.actionLabel?.lowercase()) {
            "error" -> SnackbarType.ERROR
            "success" -> SnackbarType.SUCCESS
            else -> SnackbarType.INFO
        }

        AppSnackbar(
            message = data.visuals.message,
            type = type,
            onDismiss = { data.dismiss() }
        )
    }
}

/**
 * The visual component representing the Custom Snackbar.
 * Designed as a floating card with semantic colors and icons.
 */
@Composable
fun AppSnackbar(
    message: String,
    type: SnackbarType,
    onDismiss: () -> Unit
) {
    // Determine Color Palette & Icon based on Type
    val (backgroundColor, contentColor, icon) = when (type) {
        SnackbarType.SUCCESS -> Triple(
            Color(0xFFE8F5E9), // Light Green Background
            Color(0xFF2E7D32), // Dark Green Text/Icon
            Icons.Default.CheckCircle
        )
        SnackbarType.ERROR -> Triple(
            Color(0xFFFFEBEE), // Light Red Background
            Color(0xFFC62828), // Dark Red Text/Icon
            Icons.Default.Error
        )
        SnackbarType.INFO -> Triple(
            Color(0xFF323232), // Dark Grey Background (Material Standard)
            Color.White,       // White Text
            Icons.Default.Info
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        // Add a subtle border for Success/Error types to make them pop
        border = if (type != SnackbarType.INFO) BorderStroke(1.dp, contentColor.copy(alpha = 0.1f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Message Text
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}