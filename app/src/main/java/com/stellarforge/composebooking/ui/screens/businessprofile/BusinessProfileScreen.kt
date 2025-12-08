package com.stellarforge.composebooking.ui.screens.businessprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.AppSnackbarHost
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Screen for Business Owners to manage their public-facing profile (Storefront).
 *
 * **Features:**
 * - Form inputs for Business Name, Contact Info, Address, and Logo.
 * - Auto-loading of existing profile data.
 * - Validation and Save functionality.
 * - Sign Out option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    navController: NavController,
    viewModel: BusinessProfileViewModel = hiltViewModel()
) {
    // Collect State
    val uiState by viewModel.uiState.collectAsState()

    // Form Fields (Two-way binding via ViewModel StateFlows)
    val businessName by viewModel.businessName.collectAsState()
    val contactEmail by viewModel.contactEmail.collectAsState()
    val contactPhone by viewModel.contactPhone.collectAsState()
    val address by viewModel.address.collectAsState()
    val logoUrl by viewModel.logoUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dialog State
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Handle One-time Events (Navigation)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                BusinessProfileEvent.NavigateToLogin -> {
                    navController.navigate(ScreenRoutes.Login.route) {
                        popUpTo(0) // Clear back stack on logout
                    }
                }
            }
        }
    }

    // Handle Feedback Messages (Success / Error)
    // We listen for any non-null resource ID in the state
    LaunchedEffect(uiState.updateSuccessResId, uiState.updateErrorResId, uiState.loadErrorResId) {
        // Prioritize messages: Success > Update Error > Load Error
        val messageResId = uiState.updateSuccessResId ?: uiState.updateErrorResId ?: uiState.loadErrorResId

        if (messageResId != null) {
            val isError = uiState.updateErrorResId != null || uiState.loadErrorResId != null
            val type = if (isError) "error" else "success"

            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(messageResId),
                    actionLabel = type, // Trigger Red/Green color in AppSnackbar
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.clearUpdateMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.screen_title_business_profile)) },
                actions = {
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.profile_sign_out)
                        )
                    }
                }
            )
        },
        // CUSTOM SNACKBAR
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },

        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                userRole = "owner"
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show full-screen loading only on initial load (if no data yet)
            if (uiState.isLoadingProfile && uiState.profileData == null) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .imePadding(), // Adjust for keyboard
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Basic Info
                    SectionCard(
                        title = stringResource(R.string.header_basic_info),
                        icon = Icons.Default.Business
                    ) {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = viewModel::onBusinessNameChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.label_business_name_required)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            isError = uiState.updateErrorResId == R.string.label_business_name_required // Simple validation highlight
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = viewModel::onLogoUrlChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.label_logo_url_optional)) },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                        )
                    }

                    // 2. Contact Info
                    SectionCard(
                        title = stringResource(R.string.header_contact_info),
                        icon = Icons.Default.ConnectWithoutContact
                    ) {
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = viewModel::onContactEmailChanged,
                            label = { Text(stringResource(R.string.label_contact_email)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = viewModel::onContactPhoneChanged,
                            label = { Text(stringResource(R.string.label_contact_phone)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true
                        )
                    }

                    // 3. Location Info
                    SectionCard(
                        title = stringResource(R.string.header_location_info),
                        icon = Icons.Default.LocationOn
                    ) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = viewModel::onAddressChanged,
                            label = { Text(stringResource(R.string.label_address)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. Save Button
                    Button(
                        onClick = { viewModel.saveBusinessProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !uiState.isUpdatingProfile, // Disable while saving
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isUpdatingProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.loading)) // "Loading..."
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_save_changes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.dialog_sign_out_title)) },
            text = { Text(stringResource(R.string.dialog_sign_out_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    }
                ) {
                    Text(stringResource(R.string.profile_sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/**
 * Reusable Card wrapper for form sections.
 */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}