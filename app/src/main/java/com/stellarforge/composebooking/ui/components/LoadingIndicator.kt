package com.stellarforge.composebooking.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A custom, branded loading animation replacing the default CircularProgressIndicator.
 *
 * **UX Philosophy:**
 * Instead of a generic spinner, this component uses a "Heartbeat" (Pulse) animation
 * with the app's primary icon. This reinforces branding and makes wait times feel
 * shorter and more organic.
 *
 * @param modifier Modifier to be applied to the container (usually [Modifier.fillMaxSize]).
 * @param icon The icon to display in the center (Defaults to the App's main calendar icon).
 * @param size The base size of the animated circle.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.EventAvailable, // Matches the Splash Screen Icon
    size: Dp = 80.dp
) {
    // 1. Define the Infinite Transition
    val infiniteTransition = rememberInfiniteTransition(label = "PulseLoader")

    // 2. Scale Animation (Heartbeat: 0.85x -> 1.15x)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    // 3. Alpha Animation (Glow: 0.7 -> 1.0)
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    // 4. Render UI
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Animated Container
        Surface(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .alpha(alpha)
                .shadow(elevation = 6.dp, shape = CircleShape), // Soft shadow for depth
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer, // Softer brand color
        ) {
            // Icon
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Loading...",
                    tint = MaterialTheme.colorScheme.primary, // Strong brand color
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    }
}