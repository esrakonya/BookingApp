package com.stellarforge.composebooking.ui.screens.customerprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppAlertDialog
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.AppInfoDialog
import com.stellarforge.composebooking.ui.components.AppTopBar
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.utils.formatToUsPhone
import kotlinx.coroutines.flow.collectLatest

/**
 * The main "My Account" screen for Customers.
 *
 * **Purpose:**
 * Serves as the central hub for the customer to view their profile summary,
 * access business contact details, and manage their account settings.
 *
 * **Features:**
 * - **Profile Summary:** Displays current user details.
 * - **Navigation:** Links to Edit Profile and other settings.
 * - **Business Info:** Popup dialog with store details.
 * - **Logout:** Secure session termination.
 */
@Composable
fun CustomerProfileScreen(
    navController: NavController,
    viewModel: CustomerProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // State for Dialogs
    var showContactDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Handle One-time Events (Navigation)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                CustomerProfileEvent.NavigateToLogin -> {
                    navController.navigate(ScreenRoutes.Login.route) {
                        popUpTo(0)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(id = R.string.screen_title_account),
                canNavigateBack = false
            )
        },
        bottomBar = {
            AppBottomNavigationBar(navController = navController, userRole = "customer")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Header Section (Profile Summary)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Placeholder
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                // Display first letter of name or "?" if unknown
                                text = (uiState.userName ?: "?").take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name & Email
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.userName ?: stringResource(R.string.valued_customer),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Edit Action Button
                    IconButton(onClick = {
                        navController.navigate(ScreenRoutes.EditCustomerProfile.route)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 2. Menu Items
            item {
                // Personal Info -> Goes to Edit Profile
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.menu_personal_info),
                    subtitle = stringResource(R.string.menu_personal_info_desc),
                    onClick = {
                        navController.navigate(ScreenRoutes.EditCustomerProfile.route)
                    }
                )
            }

            item {
                // Business Contact -> Opens Dialog
                ProfileMenuItem(
                    icon = Icons.Default.Store,
                    title = stringResource(R.string.menu_business_contact),
                    subtitle = stringResource(R.string.menu_business_contact_desc),
                    onClick = { showContactDialog = true }
                )
            }

            // 3. Footer (Sign Out & Version)
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Button
                TextButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_sign_out),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Version Label
                Text(
                    text = stringResource(R.string.version_label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // --- DIALOGS ---

        // 1. Business Contact Info Dialog
        if (showContactDialog) {
            val profile = uiState.businessProfile

            AppInfoDialog(
                onDismissRequest = { showContactDialog = false },
                icon = Icons.Default.Store,
                title = profile?.businessName ?: stringResource(R.string.contact_dialog_title),
                buttonText = stringResource(R.string.action_close),
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Phone
                        ContactRow(
                            icon = Icons.Default.Phone,
                            text = profile?.contactPhone?.takeIf { it.isNotBlank() }?.formatToUsPhone()
                                ?: stringResource(R.string.contact_no_phone)
                        )

                        // Email
                        if (!profile?.contactEmail.isNullOrBlank()) {
                            ContactRow(
                                icon = Icons.Default.Email,
                                text = profile?.contactEmail!!
                            )
                        }

                        // Address
                        if (!profile?.address.isNullOrBlank()) {
                            ContactRow(
                                icon = Icons.Default.LocationOn,
                                text = profile?.address!!
                            )
                        }
                    }
                }
            )
        }

        // 2. Sign Out Confirmation Dialog
        if (showSignOutDialog) {
            AppAlertDialog(
                onDismissRequest = { showSignOutDialog = false },
                onConfirm = { viewModel.signOut() },
                title = stringResource(R.string.dialog_sign_out_title),
                description = stringResource(R.string.dialog_sign_out_confirmation),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                confirmText = stringResource(R.string.profile_sign_out),
                dismissText = stringResource(R.string.action_cancel),
                isDestructive = true
            )
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}