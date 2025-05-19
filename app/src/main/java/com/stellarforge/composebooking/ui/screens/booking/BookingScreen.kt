package com.stellarforge.composebooking.ui.screens.booking

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Gerekli tüm ikonlar için
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.utils.PhoneNumberVisualTransformation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// BookingUiState'i ViewModel dosyasından import ettiğini varsayıyorum
// BookingViewEvent'i ViewModel dosyasından import ettiğini varsayıyorum

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingScreen(
    navController: NavController,
    serviceId: String, // Bu parametre ViewModel tarafından alınacak
    viewModel: BookingViewModel = hiltViewModel(),
    onBookingConfirmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Tek seferlik event'leri dinle
    LaunchedEffect(key1 = Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is BookingViewModel.BookingViewEvent.NavigateToConfirmation -> {
                    onBookingConfirmed()
                }
                is BookingViewModel.BookingViewEvent.ShowSnackbar -> {
                    val message = if (event.formatArgs.isEmpty()) {
                        context.getString(event.messageId)
                    } else {
                        context.getString(event.messageId, *event.formatArgs.toTypedArray())
                    }
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.serviceName.isNotBlank()) {
                            uiState.serviceName
                        } else {
                            stringResource(id = R.string.booking_screen_title_default)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.booking_screen_back_button_desc)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // --- Servis Detayları Bölümü ---
            if (uiState.isLoadingService) {
                LoadingSection(text = stringResource(id = R.string.booking_screen_loading_service))
            } else if (uiState.serviceName.isNotBlank()) {
                SectionCard {
                    SectionTitle(
                        text = stringResource(R.string.booking_screen_section_service_details_title),
                        icon = Icons.Filled.Info
                    )
                    Text(
                        text = uiState.serviceName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.booking_screen_duration_value_detailed, uiState.serviceDuration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Tarih ve Saat Seçimi Bölümü ---
            SectionCard {
                SectionTitle(
                    text = stringResource(R.string.booking_screen_section_datetime_title),
                    icon = Icons.Filled.DateRange
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.booking_screen_selected_date_label) + " ${
                            uiState.selectedDate.format(
                                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                            )
                        }",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(stringResource(R.string.booking_screen_change_date_button))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(id = R.string.booking_screen_select_time_label),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (uiState.isLoadingSlots) {
                    LoadingSection(text = stringResource(id = R.string.booking_screen_loading_slots))
                } else if (uiState.slotsErrorMessage != null) {
                    Text(
                        text = stringResource(id = uiState.slotsErrorMessage!!),
                        color = if (uiState.slotsErrorMessage == R.string.booking_screen_no_slots) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                } else if (uiState.availableSlots.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availableSlots.forEach { slot ->
                            val isSelected = uiState.selectedSlot == slot
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onSlotSelected(slot) },
                                label = { Text(slot.format(DateTimeFormatter.ofPattern("HH:mm"))) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                uiState.selectedSlot?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.booking_screen_selected_slot_display, it.format(DateTimeFormatter.ofPattern("HH:mm"))),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Müşteri Bilgileri Formu Bölümü ---
            SectionCard {
                SectionTitle(
                    text = stringResource(R.string.booking_screen_section_customer_info_title),
                    icon = Icons.Filled.Person // Veya Person
                )
                OutlinedTextField(
                    value = uiState.customerName,
                    onValueChange = viewModel::onCustomerNameChanged,
                    label = { Text(stringResource(id = R.string.booking_screen_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.nameErrorRes != null,
                    supportingText = {
                        uiState.nameErrorRes?.let { Text(stringResource(id = it), color = MaterialTheme.colorScheme.error) }
                    },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.customerPhone,
                    onValueChange = { newPhone ->
                        // Sadece rakamları ve en fazla 11 (veya belirlediğin maks.) haneyi al
                        val filteredPhone = newPhone.filter { it.isDigit() }.take(11) // VEYA maxPhoneLength
                        viewModel.onCustomerPhoneChanged(filteredPhone)
                    },
                    label = { Text(stringResource(id = R.string.booking_screen_phone_label)) },
                    placeholder = { Text("0(XXX)-XXX-XX-XX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = uiState.phoneErrorRes != null,
                    supportingText = {
                        uiState.phoneErrorRes?.let { Text(stringResource(id = it), color = MaterialTheme.colorScheme.error) }
                    },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    visualTransformation = PhoneNumberVisualTransformation()
                )
                // TODO: E-posta alanı eklenecekse buraya
                // OutlinedTextField(...)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Onay Butonu
            Button(
                onClick = viewModel::confirmBooking,
                enabled = !uiState.isLoadingService && !uiState.isBooking &&
                        uiState.selectedSlot != null &&
                        uiState.customerName.isNotBlank() && uiState.customerPhone.isNotBlank() &&
                        uiState.nameErrorRes == null && uiState.phoneErrorRes == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isBooking) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.booking_screen_booking_in_progress))
                    }
                } else {
                    Text(stringResource(id = R.string.booking_screen_confirm_button))
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // En altta boşluk
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            // Tarih seçicide bugünden önceki tarihlerin seçilmesini engelle
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate().atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli() - (24 * 60 * 60 * 1000) // Dünü de kapsasın diye -1 gün
                }
                override fun isSelectableYear(year: Int): Boolean {
                    return year >= LocalDate.now().year && year <= LocalDate.now().year + 2 // Örn: Mevcut yıl ve sonraki 2 yıl
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            val selectedLocalDate = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.onDateSelected(selectedLocalDate)
                        }
                    }
                ) { Text(stringResource(id = R.string.date_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.date_picker_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// Yardımcı Composable'lar (Bu dosyanın altına veya ui/components'e taşınabilir)
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(bottom = 12.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LoadingSection(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}