package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.ServiceList
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceListScreen(
    viewModel: ServiceListViewModel = hiltViewModel(),
    onServiceClick: (serviceId: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventFlow = viewModel.eventFlow

    LaunchedEffect(key1 = eventFlow) {
        eventFlow.collect { event ->
            when (event) {
                is ServiceListViewEvent.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.service_list_title)) },
                actions = {
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = stringResource(id = R.string.sign_out_button_desc)
                        )
                    }
                }
            )
        }
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
                    ServiceList(
                        services = state.services,
                        onServiceClick = onServiceClick
                    )
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