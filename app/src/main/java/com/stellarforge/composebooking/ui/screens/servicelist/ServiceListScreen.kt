package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.AppSnackbarHost
import com.stellarforge.composebooking.ui.components.CustomerServiceListItem
import com.stellarforge.composebooking.ui.components.EmptyState
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The primary **Storefront** screen for Customers.
 *
 * **Purpose:**
 * Displays the list of available services offered by the business.
 * This is the landing page for a logged-in customer.
 *
 * **Features:**
 * - **Dynamic Branding:** The TopBar title reflects the Business Name fetched from the profile.
 * - **Real-time Catalog:** Listens to the service stream; updates instantly if the owner changes prices/details.
 * - **Navigation:** Tapping a service initiates the Booking Flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    navController: NavController,
    viewModel: ServiceListViewModel = hiltViewModel(),
    onServiceClick: (serviceId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Business Name for the Top Bar (Fetched dynamically)
    val businessName by viewModel.businessName.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Handle One-time Events (Navigation, Snackbar)
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is ServiceListViewEvent.NavigateToLogin -> {
                    Timber.d("Navigating to Login")
                    navController.navigate(ScreenRoutes.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
                is ServiceListViewEvent.ShowSnackbar -> {
                    scope.launch {
                        // Use "error" label if it's a critical failure (like sign out fail)
                        snackbarHostState.showSnackbar(
                            message = context.getString(event.messageResId),
                            actionLabel = "error",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Display Business Name if loaded, otherwise generic title
                    Text(businessName ?: stringResource(id = R.string.service_list_title))
                }
            )
        },
        // CUSTOM SNACKBAR HOST
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },

        bottomBar = {
            AppBottomNavigationBar(navController = navController, userRole = "customer")
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            when (val state = uiState) {
                // 1. Loading State
                is ServiceListUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // 2. Success State
                is ServiceListUiState.Success -> {
                    if (state.services.isEmpty()) {
                        // Empty List
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.service_list_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Service List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items = state.services, key = { it.id }) { service ->
                                CustomerServiceListItem(
                                    service = service,
                                    onClick = { onServiceClick(service.id) }
                                )
                            }
                        }
                    }
                }

                // 3. Error State
                is ServiceListUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(
                            messageResId = R.string.service_list_empty,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onRetryClicked() }) {
                            Text(stringResource(R.string.retry_button_label))
                        }
                    }
                }
            }
        }
    }
}