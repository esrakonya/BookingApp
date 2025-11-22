package com.stellarforge.composebooking.ui.screens.businessprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    navController: NavController,
    viewModel: BusinessProfileViewModel = hiltViewModel()
) {
    // ViewModel'dan state'leri ve form alanlarını al
    val uiState by viewModel.uiState.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val contactEmail by viewModel.contactEmail.collectAsState()
    val contactPhone by viewModel.contactPhone.collectAsState()
    val address by viewModel.address.collectAsState()
    val logoUrl by viewModel.logoUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                BusinessProfileEvent.NavigateToLogin -> {
                    navController.navigate(ScreenRoutes.Login.route) {
                        popUpTo(0)
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.updateSuccessMessage, uiState.updateErrorMessage, uiState.loadErrorMessage) {
        val message = uiState.updateSuccessMessage ?: uiState.updateErrorMessage ?: uiState.loadErrorMessage
        if (message != null) {
            scope.launch { snackbarHostState.showSnackbar(message) }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(paddingValues) // Scaffold'un padding'ini uygula
        ) {
            // Sadece ilk yüklemede tam ekran loading göster,
            // arka planda tazeleme sırasında içeriği gizleme.
            if (uiState.isLoadingProfile && uiState.profileData == null) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // Ana içerik Column'u, kaydırılabilir ve boşluklu
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(24.dp) // Kartlar arası dikey boşluk
                ) {
                    // Temel Bilgiler Kartı
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
                            // TODO: ViewModel'dan gelen validasyon sonucuna göre isError ve supportingText ekle
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

                    // İletişim Bilgileri Kartı
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

                    // Konum Bilgileri Kartı
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

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

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
 * Form bölümlerini sarmalamak için yeniden kullanılabilir bir Card bileşeni.
 * Başlık ve ikon ile görsel hiyerarşi sağlar.
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
                    fontWeight = FontWeight.SemiBold // SemiBold daha yumuşak bir vurgu sağlar
                )
            }
            // Kartın içeriği (TextField'lar vb.) bu ColumnScope içinde render edilir.
            content()
        }
    }
}