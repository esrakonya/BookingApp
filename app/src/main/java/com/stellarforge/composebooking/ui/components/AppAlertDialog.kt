package com.stellarforge.composebooking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * A highly customized, modern, and sleek Alert Dialog component.
 * Replaces the standard Android AlertDialog with a design that fits the app's theme.
 *
 * **Design Logic:**
 * - Uses [MaterialTheme.colorScheme.surfaceContainerHigh] for the card background to ensure
 *   it stands out in both Light and Dark modes without being tinted pink/blue.
 * - Applies Semantic Colors (Error/Primary) only to the Icon and Buttons for a clean look.
 *
 * @param onDismissRequest Called when the user tries to dismiss the dialog.
 * @param onConfirm Called when the confirmation button is clicked.
 * @param title The dialog title.
 * @param description The body text of the dialog.
 * @param icon The vector icon displayed at the top.
 * @param confirmText Label for the positive action button.
 * @param dismissText Label for the negative action button.
 * @param isDestructive If true, the icon and confirm button use 'Error' colors (Red).
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    description: String,
    icon: ImageVector,
    confirmText: String,
    dismissText: String,
    isDestructive: Boolean = false
) {
    // 1. Determine Action Colors based on destructive state
    // Action Color: Used for the Button Background and Icon Tint
    val actionColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    // Bubble Color: Used for the circle behind the icon
    val iconBubbleColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp), // Modern, soft corners
            colors = CardDefaults.cardColors(
                // CRITICAL FIX: Use a neutral container color (White/Gray).
                // Do NOT use 'errorContainer' for the whole card, as it looks too aggressive (pink).
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2. Icon Bubble
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(color = iconBubbleColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = actionColor, // Icon inherits the action color (Red/Blue)
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 5. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Dismiss Button (Outlined - Neutral)
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = dismissText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Confirm Button (Filled - Colored)
                    Button(
                        onClick = {
                            onDismissRequest()
                            onConfirm()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = actionColor // Button is Red if destructive, Blue otherwise
                        )
                    ) {
                        Text(
                            text = confirmText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single-button Info Dialog with customizable content.
 * Ideal for displaying details like Contact Info, Help, or Status updates.
 *
 * @param content A Composable lambda allowing injection of any custom UI (Rows, Columns, etc.).
 */
@Composable
fun AppInfoDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector,
    buttonText: String,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Icon Bubble (Neutral/Primary style for info)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Custom Content Area
                content()

                Spacer(modifier = Modifier.height(24.dp))

                // 4. Close Button (Outlined for a cleaner, non-intrusive look)
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        text = buttonText,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}