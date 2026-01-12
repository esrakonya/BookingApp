package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.AppSnackbarHost
import com.stellarforge.composebooking.ui.components.CustomerServiceListItem
import com.stellarforge.composebooking.ui.components.EmptyState
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.components.NetworkLogoImage
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
@Composable
fun ServiceListScreen(
    navController: NavController,
    viewModel: ServiceListViewModel = hiltViewModel(),
    onServiceClick: (serviceId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val businessProfile by viewModel.businessProfile.collectAsState()

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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {

                        // --- 1. HEADER ---
                        if (businessProfile != null) {
                            item {
                                StorefrontHeader(profile = businessProfile!!)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // --- 2. SERVICE LIST ---
                        if (state.services.isEmpty()) {
                            item {
                                EmptyState(
                                    messageResId = R.string.service_list_empty,
                                    modifier = Modifier.padding(top = 32.dp)
                                )
                            }
                        } else {
                            items(items = state.services, key = { it.id }) { service ->
                                Box(
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    CustomerServiceListItem(
                                        service = service,
                                        onClick = { onServiceClick(service.id) }
                                    )
                                }
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

@Composable
fun StorefrontHeader(profile: BusinessProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    NetworkLogoImage(
                        url = profile.logoUrl,
                        size = 64.dp,
                        borderColor = Color.Transparent
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = profile.businessName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Address
                if (!profile.address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp).offset(y = 2.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profile.address!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}