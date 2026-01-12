package com.stellarforge.composebooking.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.ui.components.AppSnackbarHost
import com.stellarforge.composebooking.ui.components.AppTopBar
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.utils.PhoneNumberVisualTransformation
import com.stellarforge.composebooking.utils.toFormattedPrice
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The primary screen for the Customer Booking Flow.
 *
 * **Features:**
 * - **Service Details:** Displays the summary of the selected service.
 * - **Calendar:** Interactive horizontal calendar for date selection.
 * - **Slot Selection:** Dynamic time slots based on availability logic.
 * - **Customer Info:** Form to capture user details (Name, Phone, Email).
 * - **Confirmation:** Triggers the booking transaction.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingScreen(
    navController: NavController,
    viewModel: BookingViewModel = hiltViewModel(),
    onBookingConfirmed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Handle One-time Events (Navigation & Errors)
    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is BookingViewEvent.NavigateToConfirmation -> {
                    onBookingConfirmed()
                }
                is BookingViewEvent.ShowSnackbar -> {
                    scope.launch {
                        // Use "error" action label to trigger the Red/Error styling in AppSnackbar
                        snackbarHostState.showSnackbar(
                            message = context.getString(event.messageId),
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
            AppTopBar(
                title = uiState.service?.let { stringResource(R.string.booking_screen_title_with_service, it.name) }
                    ?: stringResource(id = R.string.booking_screen_title),
                canNavigateBack = true,
                navigateUp = { navController.popBackStack() }
            )
        },
        // CUSTOM SNACKBAR HOST
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },

        bottomBar = {
            val isFormValid = uiState.customerName.isNotBlank() &&
                    uiState.customerPhone.isNotBlank() &&
                    uiState.nameError == null &&
                    uiState.phoneError == null

            Surface(tonalElevation = 4.dp, shadowElevation = 4.dp) {
                Button(
                    onClick = viewModel::confirmBooking,
                    enabled = !uiState.isLoadingService && !uiState.isBooking && uiState.selectedSlot != null && isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp) // Standard touch target height
                ) {
                    if (uiState.isBooking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            stringResource(id = R.string.booking_screen_confirm_button),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // Main Content Area
        if (uiState.isLoadingService && uiState.service == null) {
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                uiState.service?.let { service ->
                    ServiceDetailsSection(service = service)
                }

                DateTimeSelectionSection(
                    uiState = uiState,
                    onDateSelected = viewModel::onDateSelected,
                    onSlotSelected = viewModel::onSlotSelected
                )

                CustomerInfoSection(
                    uiState = uiState,
                    onNameChanged = viewModel::onCustomerNameChanged,
                    onPhoneChanged = viewModel::onCustomerPhoneChanged,
                    onEmailChanged = viewModel::onCustomerEmailChanged
                )

                Spacer(modifier = Modifier.height(64.dp)) // Extra space for bottom bar
            }
        }
    }
}

//--- HELPER COMPOSABLES ---

@Composable
private fun ServiceDetailsSection(service: Service) {
    SectionCard(
        title = stringResource(R.string.booking_screen_section_service_details),
        icon = Icons.Default.ContentCut
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailItem(
                    label = stringResource(id = R.string.booking_screen_duration_label),
                    value = stringResource(id = R.string.service_list_item_duration, service.durationMinutes)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DetailItem(
                    label = stringResource(id = R.string.booking_screen_price_label),
                    value = service.priceInCents.toFormattedPrice()
                )
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
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
        title = stringResource(R.string.booking_screen_section_datetime),
        icon = Icons.Default.CalendarToday
    ) {
        // Calendar Logic
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

        // Week Headers
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

        // Calendar Grid
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                CalendarDay(
                    day = day,
                    isSelected = uiState.selectedDate == day.date,
                    onDayClick = { clickedDate ->
                        if (day.position == DayPosition.MonthDate) {
                            onDateSelected(clickedDate)
                        }
                    }
                )
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Time Slot Selection Grid
        if (uiState.isLoadingSlots) {
            LoadingSection(text = stringResource(R.string.loading))
        } else if (uiState.error != null && uiState.availableSlots.isEmpty()) {
            Text(
                stringResource(id = uiState.error!!),
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.booking_screen_select_time_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableSlots.forEach { slot ->
                    val isSelected = uiState.selectedSlot == slot
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSlotSelected(slot) },
                        label = { Text(slot.format(DateTimeFormatter.ofPattern("HH:mm"))) },
                        leadingIcon = if (isSelected) { { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } } else null
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
    onPhoneChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit
) {
    SectionCard(
        title = stringResource(R.string.booking_screen_section_customer_info),
        icon = Icons.Default.Person
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = uiState.customerName,
                onValueChange = onNameChanged,
                label = { Text(stringResource(id = R.string.booking_screen_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.nameError != null,
                supportingText = { uiState.nameError?.let { Text(stringResource(id = it)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            OutlinedTextField(
                value = uiState.customerPhone,
                onValueChange = { input ->
                    val cleaned = input.filter { it.isDigit() }.take(10)
                    onPhoneChanged(cleaned)
                },
                label = { Text(stringResource(id = R.string.booking_screen_phone_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                isError = uiState.phoneError != null,
                supportingText = { uiState.phoneError?.let { Text(stringResource(id = it)) } },
                visualTransformation = PhoneNumberVisualTransformation()
            )
            OutlinedTextField(
                value = uiState.customerEmail,
                onValueChange = onEmailChanged,
                label = { Text(stringResource(id = R.string.booking_screen_email_label)) },
                placeholder = { Text(stringResource(id = R.string.booking_screen_email_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            )
        }
    }
}

/**
 * Reusable Card component for grouping related booking information.
 */
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}

@Composable
private fun LoadingSection(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CalendarDay(
    day: CalendarDay,
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
            .clickable(enabled = isSelectable) { onDayClick(day.date) },
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