package com.stellarforge.composebooking.ui.screens.businessprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.LoadingIndicator

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

    // Başarı veya hata mesajları için Snackbar gösterimini yöneten LaunchedEffect'ler
    LaunchedEffect(uiState.updateSuccessMessage) {
        uiState.updateSuccessMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUpdateMessages() // Mesaj gösterildikten sonra temizle
        }
    }
    LaunchedEffect(uiState.updateErrorMessage) {
        uiState.updateErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            viewModel.clearUpdateMessages()
        }
    }
    LaunchedEffect(uiState.loadErrorMessage) {
        uiState.loadErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            // Yükleme hatası mesajını temizlemek için bir mekanizma eklenebilir
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.screen_title_business_profile)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // "Kaydet" butonu, her zaman altta ve görünür olacak şekilde
            Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Button(
                    onClick = { viewModel.saveBusinessProfile() },
                    enabled = !uiState.isUpdatingProfile && !uiState.isLoadingProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uiState.isUpdatingProfile) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            stringResource(R.string.button_save_profile),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
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
                        .padding(16.dp),
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
                }
            }
        }
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