package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.stellarforge.composebooking.ui.components.ServiceList
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    navController: NavController,
    viewModel: ServiceListViewModel = hiltViewModel(),
    onServiceClick: (serviceId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventFlow = viewModel.eventFlow

    val context = LocalContext.current // Snackbar'da string kaynağı için
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    LaunchedEffect(key1 = eventFlow) {
        eventFlow.collect { event ->
            when (event) {
                is ServiceListViewEvent.NavigateToLogin -> {
                    Timber.d("ServiceListScreen: NavigateToLogin event received, navigating to LoginScreen.")
                    navController.navigate(ScreenRoutes.Login.route) {
                        popUpTo(ScreenRoutes.ServiceList.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is ServiceListViewEvent.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = context.getString(event.messageResId),
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
                title = { Text(stringResource(id = R.string.service_list_title)) },
                actions = {
                    // YENİ İŞLETME PROFİLİ DÜZENLEME BUTONU
                    IconButton(onClick = {
                        navController.navigate(ScreenRoutes.BusinessProfile.route)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings, // Veya ManageAccounts, Storefront vb.
                            contentDescription = stringResource(id = R.string.action_edit_business_profile) // strings.xml'e ekle
                        )
                    }

                    // MEVCUT OTURUMU KAPATMA BUTONU
                    IconButton(onClick = { viewModel.signOut() }) { // signOut çağrısı zaten NavigateToLogin event'ini tetiklemeli
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = stringResource(id = R.string.sign_out_button_desc)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->  
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is ServiceListUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(id = R.string.service_list_loading))
                    }
                }

                is ServiceListUiState.Success -> {
                    if (state.services.isEmpty()) { // Servis listesi boşsa
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.service_list_empty), // strings.xml'e ekle
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        ServiceList(
                            services = state.services,
                            onServiceClick = onServiceClick
                        )
                    }
                }

                is ServiceListUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(id = state.messageResId),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
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