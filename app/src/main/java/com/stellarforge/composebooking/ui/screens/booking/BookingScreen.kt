package com.stellarforge.composebooking.ui.screens.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.utils.PhoneNumberVisualTransformation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch


// ViewModel'dan gelen state ve event'lerin import edildiğini varsayıyoruz.
// import com.stellarforge.composebooking.ui.screens.booking.BookingViewModel.BookingViewEvent
// import com.stellarforge.composebooking.ui.screens.booking.BookingUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) // DÜZELTİLDİ: ExperimentalLayoutApi eklendi
@Composable
fun BookingScreen(
    navController: NavController,
    viewModel: BookingViewModel = hiltViewModel(),
    onBookingConfirmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val eventFlow = viewModel.eventFlow

    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        eventFlow.collect { event ->
            // DÜZELTME: Event'lere tam yoluyla erişelim (ViewModel adıyla)
            when (event) {
                is BookingViewEvent.NavigateToConfirmation -> {
                    onBookingConfirmed()
                }

                is BookingViewEvent.ShowSnackbar -> {
                    val message = if (event.formatArgs.isEmpty()) {
                        context.getString(event.messageId)
                    } else {
                        // Spread operatörü (*) ile format argümanlarını doğru şekilde geçir
                        context.getString(event.messageId, *event.formatArgs.toTypedArray())
                    }
                    // Snackbar'ı göstermek için yeni bir coroutine başlat
                    // LaunchedEffect scope'u zaten bir coroutine scope'udur, ama
                    // showSnackbar suspend olduğu için launch kullanmak daha güvenlidir.
                    launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.onScreenReady()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.serviceName.ifBlank { stringResource(id = R.string.booking_screen_title_default) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Button(
                    onClick = viewModel::confirmBooking,
                    enabled = !uiState.isLoadingService && !uiState.isBooking &&
                            uiState.selectedSlot != null &&
                            uiState.customerName.isNotBlank() &&
                            uiState.nameErrorRes == null && uiState.phoneErrorRes == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp)
                ) {
                    if (uiState.isBooking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(id = R.string.booking_screen_booking_in_progress))
                        }
                    } else {
                        Text(
                            stringResource(id = R.string.booking_screen_confirm_button),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoadingService && uiState.serviceName.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceDetailsSection(uiState = uiState)
                DateTimeSelectionSection(
                    uiState = uiState,
                    onDateSelected = viewModel::onDateSelected,
                    onSlotSelected = viewModel::onSlotSelected
                )
                CustomerInfoSection(
                    uiState = uiState,
                    onNameChanged = viewModel::onCustomerNameChanged,
                    onPhoneChanged = { phone ->
                        val filteredPhone = phone.filter { it.isDigit() }.take(11)
                        viewModel.onCustomerPhoneChanged(filteredPhone)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
                override fun isSelectableYear(year: Int): Boolean {
                    return year >= LocalDate.now().year && year <= LocalDate.now().year + 1
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

//--- YARDIMCI COMPOSABLE BİLEŞENLER ---

@Composable
private fun ServiceDetailsSection(uiState: BookingUiState) {
    if (uiState.serviceName.isNotBlank()) {
        SectionCard(
            title = stringResource(R.string.booking_screen_section_service_details_title),
            icon = Icons.Filled.Info // DÜZELTİLDİ: InfoOutline yerine Info
        ) {
            Text(
                text = uiState.serviceName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.booking_screen_duration_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.booking_screen_duration_value, uiState.serviceDuration),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DateTimeSelectionSection(
    uiState: BookingUiState,
    onDateSelected: (LocalDate) -> Unit,
    onSlotSelected: (LocalTime) -> Unit
) {
    SectionCard(
        title = stringResource(R.string.booking_screen_section_datetime_title),
        icon = Icons.Default.CalendarToday
    ) {
        // --- TAKVİM KISMI (TAMAMEN DÜZELTİLDİ) ---
        val currentMonth = remember { YearMonth.now() }
        val startMonth = remember { currentMonth }
        val endMonth = remember { currentMonth.plusMonths(3) }
        val daysOfWeek = remember { daysOfWeek() }

        val calendarState = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = daysOfWeek.first()
        )

        val visibleMonth = remember { derivedStateOf { calendarState.firstVisibleMonth } }.value
        Text(
            text = "${visibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }} ${visibleMonth.yearMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Haftanın günleri başlığı (PZT, SAL, ...)
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Yatay Takvim
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                CalendarDay( // Düzeltilmiş CalendarDay'i kullan
                    day = day,
                    isSelected = uiState.selectedDate == day.date,
                    onDayClick = { clickedDate ->
                        if (day.position == DayPosition.MonthDate) { // Sadece ayın içindeki günlere tıkla
                            onDateSelected(clickedDate)
                        }
                    }
                )
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- SAAT SLOTLARI KISMI (TAMAMEN DÜZELTİLDİ) ---
        if (uiState.isLoadingSlots) {
            LoadingSection(text = stringResource(R.string.booking_screen_loading_slots))
        } else if (uiState.availableSlots.isEmpty()) {
            Text(
                stringResource(id = R.string.booking_screen_no_slots),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow( // Resmi FlowRow kullanımı
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableSlots.forEach { slot ->
                    val isSelected = uiState.selectedSlot == slot
                    val chipEnabled = !uiState.isLoadingSlots

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSlotSelected(slot) },
                        label = { Text(slot.format(DateTimeFormatter.ofPattern("HH:mm"))) },
                        enabled = chipEnabled,
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                !chipEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerInfoSection(
    uiState: BookingUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit
) {
    SectionCard(
        title = stringResource(R.string.booking_screen_section_customer_info_title),
        icon = Icons.Default.Person // DÜZELTİLDİ: PersonOutline yerine Person
    ) {
        OutlinedTextField(
            value = uiState.customerName,
            onValueChange = onNameChanged,
            label = { Text(stringResource(id = R.string.booking_screen_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.nameErrorRes != null,
            supportingText = { uiState.nameErrorRes?.let { Text(stringResource(id = it)) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.customerPhone,
            onValueChange = onPhoneChanged,
            label = { Text(stringResource(id = R.string.booking_screen_phone_label)) },
            placeholder = { Text("05xx xxx xx xx") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            isError = uiState.phoneErrorRes != null,
            supportingText = { uiState.phoneErrorRes?.let { Text(stringResource(id = it)) } },
            visualTransformation = PhoneNumberVisualTransformation()
        )
    }
}

@Composable
private fun SectionCard(
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
                modifier = Modifier.padding(bottom = 12.dp)
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
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
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

@Composable
private fun CalendarDay(
    day: CalendarDay, // DÜZELTİLDİ: WeekDay yerine CalendarDay
    isSelected: Boolean,
    onDayClick: (LocalDate) -> Unit
) {
    val isSelectable = day.position == DayPosition.MonthDate && day.date >= LocalDate.now()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(
                enabled = isSelectable,
                onClick = { onDayClick(day.date) }
            ),
        contentAlignment = Alignment.Center
    ) {
        val textColor = when {
            !isSelectable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}