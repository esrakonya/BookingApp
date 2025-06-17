package com.stellarforge.composebooking.ui.screens.businessprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val uiState by viewModel.uiState.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val contactEmail by viewModel.contactEmail.collectAsState()
    val contactPhone by viewModel.contactPhone.collectAsState()
    val address by viewModel.address.collectAsState()
    val logoUrl by viewModel.logoUrl.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.updateSuccessMessage) {
        uiState.updateSuccessMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUpdateMessages()
        }
    }
    LaunchedEffect(uiState.updateErrorMessage) {
        uiState.updateErrorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            viewModel.clearUpdateMessages()
        }
    }
    LaunchedEffect(uiState.loadErrorMessage) {
        uiState.loadErrorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            viewModel.clearUpdateMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.booking_screen_title_default)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.login_navigate_to_signup_text)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoadingProfile) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { viewModel.onBusinessNameChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.screen_title_business_profile)) },
                        leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null) },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { viewModel.onContactEmailChanged(it) },
                        label = { Text(stringResource(R.string.label_contact_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { viewModel.onContactPhoneChanged(it) },
                        label = { Text(stringResource(R.string.label_contact_phone)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { viewModel.onAddressChanged(it) },
                        label = { Text(stringResource(R.string.label_address)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { viewModel.onLogoUrlChanged(it) },
                        label = { Text(stringResource(R.string.label_logo_url_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Uri
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.saveBusinessProfile() },
                        enabled = !uiState.isUpdatingProfile,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isUpdatingProfile) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.button_save_profile))
                        }
                    }
                }
            }
        }
    }
}