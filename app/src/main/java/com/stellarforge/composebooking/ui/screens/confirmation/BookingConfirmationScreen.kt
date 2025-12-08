package com.stellarforge.composebooking.ui.screens.confirmation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stellarforge.composebooking.R

/**
 * A dedicated success screen displayed after a booking is successfully committed.
 *
 * **Purpose:**
 * - Provides immediate positive feedback to the user.
 * - Acts as a navigation "terminal point" to prevent the user from pressing "Back"
 *   and accidentally re-submitting the booking form.
 * - Routes the user back to the Home/Service List.
 */
@Composable
fun BookingConfirmationScreen(
    onNavigateHome: () -> Unit
) {
    // Surface ensures the correct background color is applied based on the Theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success Icon
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(id = R.string.confirmation_title),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(id = R.string.confirmation_title), // e.g. "Success!"
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Message
            Text(
                text = stringResource(id = R.string.confirmation_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Return Button
            Button(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp) // Standard height for better touch target
            ) {
                Text(stringResource(id = R.string.confirmation_home_button))
            }
        }
    }
}