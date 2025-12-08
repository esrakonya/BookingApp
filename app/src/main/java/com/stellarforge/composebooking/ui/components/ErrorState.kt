package com.stellarforge.composebooking.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stellarforge.composebooking.R

/**
 * A reusable full-screen error component.
 *
 * **Usage:**
 * Display this component when a screen fails to load its primary content (e.g., Network Error, Database Failure).
 * It provides a visual cue and a standardized "Retry" action to allow the user to attempt recovery.
 *
 * @param messageResId The localized resource ID of the error message to display.
 * @param onRetry Callback function triggered when the user taps the button.
 * @param modifier Modifier to be applied to the container.
 */
@Composable
fun ErrorState(
    @StringRes messageResId: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp), // Generous padding for a cleaner look
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual indicator
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f) // Softened Error Color
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error Message
        Text(
            text = stringResource(id = messageResId),
            color = MaterialTheme.colorScheme.onSurface, // Readable text color
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action Button
        Button(
            onClick = onRetry,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(id = R.string.retry_button_label))
        }
    }
}