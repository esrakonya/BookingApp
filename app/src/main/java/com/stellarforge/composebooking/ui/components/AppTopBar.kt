package com.stellarforge.composebooking.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stellarforge.composebooking.R

/**
 * A standardized Top App Bar (Header) used throughout the application.
 * It automatically adapts its color for Light and Dark modes to ensure visibility.
 *
 * @param title The text displayed in the center of the bar.
 * @param canNavigateBack If true, displays a back arrow icon.
 * @param navigateUp Callback triggered when the back arrow is clicked.
 * @param actions Optional block for adding action buttons (e.g., Save, Sign Out) to the right side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    navigateUp: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            // Use 'surfaceColorAtElevation' to create a subtle separation from the background.
            // In Light Mode: It appears white/light grey.
            // In Dark Mode: It becomes a lighter grey than the black background.
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.shadow(4.dp), // Adds depth
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.action_navigate_back)
                    )
                }
            }
        },
        actions = actions
    )
}