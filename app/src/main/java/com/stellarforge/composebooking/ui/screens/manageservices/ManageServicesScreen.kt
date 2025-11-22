package com.stellarforge.composebooking.ui.screens.manageservices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.EmptyState
import com.stellarforge.composebooking.ui.components.ErrorState
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.components.OwnerServiceListItem
import com.stellarforge.composebooking.ui.components.ServiceListItem
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServicesScreen(
    navController: NavController,
    viewModel: ManageServicesViewModel = hiltViewModel(),
    onAddService: () -> Unit,
    onEditService: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.manage_services_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_navigate_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddService) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.manage_services_add_service))
            }
        },
        bottomBar = {
            AppBottomNavigationBar(
                navController = navController,
                userRole = "owner"
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            when {
                // 1. DURUM: Bir hata varsa, her şeyden önce hatayı göster.
                uiState.errorResId != null -> {
                    ErrorState(
                        messageResId = uiState.errorResId!!,
                        onRetry = { viewModel.loadServicesForOwner() }, // Yeniden yüklemeyi tetikle
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 2. DURUM: Hata yoksa ve hala yükleniyorsa, yükleme göstergesini göster.
                // (Özellikle ilk açılışta bu durum geçerlidir).
                uiState.isLoading -> {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // 3. DURUM: Yükleme bittiyse ve servis listesi boşsa, "Boş Durum" ekranını göster.
                uiState.services.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }

                // 4. DURUM (Mutlu Yol): Yükleme bittiyse ve servisler varsa, listeyi göster.
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = uiState.services, key = { it.id }) { service ->
                            // Senin paylaştığın `ServiceListItem` Composable'ını çağırıyoruz.
                            ServiceListItem(
                                service = service,
                                onEditClick = { onEditService(service.id) },
                                onDeleteClick = { viewModel.deleteService(service.id) },
                                isBeingDeleted = service.id == uiState.isDeletingServiceId
                            )
                        }
                    }
                }
            }
        }
    }
}