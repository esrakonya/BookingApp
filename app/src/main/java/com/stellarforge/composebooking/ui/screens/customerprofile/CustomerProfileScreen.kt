package com.stellarforge.composebooking.ui.screens.customerprofile

import android.widget.Toast
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest

/**
 * The main "My Account" screen for Customers.
 *
 * **Purpose:**
 * This screen serves as the hub for the logged-in customer to view their details
 * and interact with the business meta-information.
 *
 * **Features:**
 * - **Profile Summary:** Displays user's name and email (derived from Auth).
 * - **Business Contact:** Opens a dialog showing the Shop's Phone, Address, and Email (fetched from [FirebaseConstants.TARGET_BUSINESS_OWNER_ID]).
 * - **Menu Options:** Placeholders for editing personal info and app settings.
 * - **Session Management:** Provides a secure "Sign Out" functionality.
 *
 * @param navController Used for navigation actions (Logout -> Login).
 * @param viewModel Hilt-injected [CustomerProfileViewModel] managing the state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    navController: NavController,
    viewModel: CustomerProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showContactDialog by remember { mutableStateOf(false) }

    // Navigation Events
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
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.screen_title_account)) }) },
        bottomBar = { AppBottomNavigationBar(navController = navController, userRole = "customer") }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                // Safe handling of username
                                text = (uiState.userName ?: "?").take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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
                    IconButton(onClick = { /* TODO: Navigate to Edit Profile */ }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 2. Menu Items
            item {
                val context = LocalContext.current
                val msg = stringResource(R.string.feature_coming_soon)
                ProfileMenuItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.menu_personal_info),
                    subtitle = stringResource(R.string.menu_personal_info_desc),
                    onClick = {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
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
                TextButton(
                    onClick = { viewModel.signOut() },
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
                    Text(stringResource(R.string.profile_sign_out), fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = stringResource(R.string.version_label), // "Version 1.0.0"
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Contact Info Dialog
        if (showContactDialog) {
            val profile = uiState.businessProfile
            AlertDialog(
                onDismissRequest = { showContactDialog = false },
                icon = { Icon(Icons.Default.Store, contentDescription = null) },
                title = {
                    // DÜZELTME: profile?. kullanımı
                    Text(text = profile?.businessName ?: stringResource(R.string.contact_dialog_title))
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Phone
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // DÜZELTME: profile?.contactPhone kullanımı
                            Text(text = profile?.contactPhone?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.contact_no_phone))
                        }

                        // Email (Optional)
                        // DÜZELTME: profile?.contactEmail
                        if (!profile?.contactEmail.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // DÜZELTME: Elvis operatörü ile String garantisi
                                Text(text = profile?.contactEmail ?: "")
                            }
                        }

                        // Address (Optional)
                        // DÜZELTME: profile?.address
                        if (!profile?.address.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // DÜZELTME: Elvis operatörü
                                Text(text = profile?.address ?: stringResource(R.string.contact_no_address))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContactDialog = false }) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            )
        }
    }
}

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