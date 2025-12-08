package com.stellarforge.composebooking.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * A reusable placeholder component displayed when a list or screen has no content.
 *
 * **Design:**
 * Minimalist layout with a centered icon and a descriptive text message.
 * Supports customization of the icon and message to fit different contexts (e.g., "No Services", "No Bookings").
 *
 * @param messageResId The resource ID of the localized text to display.
 * @param modifier Modifier to be applied to the container.
 * @param icon The icon to display above the text (defaults to a neutral Info icon).
 */
@Composable
fun EmptyState(
    @StringRes messageResId: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) // Subtle, inactive color
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = messageResId),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}