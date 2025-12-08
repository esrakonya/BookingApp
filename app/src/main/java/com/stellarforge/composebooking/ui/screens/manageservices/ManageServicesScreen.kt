package com.stellarforge.composebooking.ui.screens.manageservices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.AppSnackbarHost
import com.stellarforge.composebooking.ui.components.EmptyState
import com.stellarforge.composebooking.ui.components.ErrorState
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.components.OwnerServiceListItem
import kotlinx.coroutines.launch

/**
 * Screen for Business Owners to manage their Service Catalog.
 *
 * **Features:**
 * - Lists all services (Active & Inactive).
 * - Provides entry points to Add and Edit services.
 * - Allows deletion of services with visual feedback.
 * - Handles loading and empty states gracefully.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServicesScreen(
    navController: NavController,
    viewModel: ManageServicesViewModel = hiltViewModel(),
    onAddService: () -> Unit,
    onEditService: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Error Handling Logic:
    // If there is data on the screen, show errors via Snackbar (e.g., delete failed).
    // If there is NO data, the 'when' block below handles the full-screen ErrorState.
    LaunchedEffect(uiState.errorResId) {
        if (uiState.errorResId != null && uiState.services.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(uiState.errorResId!!),
                    actionLabel = "error", // Triggers Red color in AppSnackbar
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_services_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_navigate_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddService) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.manage_services_add_service)
                )
            }
        },
        // CUSTOM SNACKBAR HOST
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
            when {
                // 1. Initial Load Loading
                uiState.isLoading && uiState.services.isEmpty() -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // 2. Full Screen Error (Only if list is empty, e.g., initial load failed)
                uiState.errorResId != null && uiState.services.isEmpty() -> {
                    ErrorState(
                        messageResId = uiState.errorResId!!,
                        onRetry = { viewModel.loadServicesForOwner() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 3. Empty State (Loaded but no services)
                uiState.services.isEmpty() -> {
                    EmptyState(
                        messageResId = R.string.manage_services_empty_state,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 4. Success State (List Content)
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = uiState.services, key = { it.id }) { service ->
                            OwnerServiceListItem(
                                service = service,
                                onEditClick = { onEditService(service.id) },
                                onDeleteClick = { viewModel.deleteService(service.id) },
                                // Show spinner ONLY on the item being deleted
                                isBeingDeleted = service.id == uiState.isDeletingServiceId
                            )
                        }
                    }

                    // Show a small loading indicator at the top if refreshing with data
                    if (uiState.isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }
        }
    }
}