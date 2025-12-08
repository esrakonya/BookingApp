package com.stellarforge.composebooking.ui.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.daysOfWeek
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.AppSnackbarHost
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.ui.screens.booking.CalendarDay
import com.stellarforge.composebooking.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * The main Dashboard/Schedule screen for Business Owners.
 *
 * **Features:**
 * - **Interactive Calendar:** Allows the owner to navigate through days and see their agenda.
 * - **Appointment List:** Displays detailed information (Customer Name, Service, Time) for the selected date.
 * - **Empty State Handling:** Shows a clear message when no bookings exist for a day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Note: Although we don't actively trigger snackbars in this screen yet,
    // we keep the Host ready for future features (e.g., cancelling an appointment as Owner).
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.schedule_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_navigate_back)
                        )
                    }
                }
            )
        },
        // CUSTOM SNACKBAR HOST
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },

        bottomBar = {
            AppBottomNavigationBar(navController = navController, userRole = "owner")
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Calendar Header
            CalendarSection(
                selectedDate = uiState.selectedDate,
                onDateSelected = viewModel::onDateSelected
            )
            HorizontalDivider()

            // Content Area
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.errorResId != null -> {
                        ErrorState(
                            message = stringResource(uiState.errorResId!!),
                            onRetry = viewModel::onRetry
                        )
                    }
                    uiState.appointments.isEmpty() -> {
                        EmptyState(
                            messageResId = R.string.schedule_screen_no_appointments,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        AppointmentList(appointments = uiState.appointments)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSection(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(6) }
    val endMonth = remember { currentMonth.plusMonths(6) }
    val daysOfWeek = remember { daysOfWeek() }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = YearMonth.from(selectedDate),
        firstDayOfWeek = daysOfWeek.first()
    )
    val visibleMonth = remember { derivedStateOf { calendarState.firstVisibleMonth } }.value

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${visibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }} ${visibleMonth.yearMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(12.dp))
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
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                CalendarDay(
                    day = day,
                    isSelected = selectedDate == day.date,
                    onDayClick = onDateSelected
                )
            }
        )
    }
}

@Composable
private fun AppointmentList(appointments: List<Appointment>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(appointments, key = { it.id }) { appointment ->
            AppointmentListItem(appointment = appointment)
        }
    }
}

@Composable
private fun AppointmentListItem(appointment: Appointment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = appointment.appointmentDateTime.toFormattedTime(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(appointment.serviceName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(appointment.customerName, style = MaterialTheme.typography.bodyMedium)
                appointment.customerPhone.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(id = R.string.retry_button_label))
        }
    }
}

// --- HELPER FORMATTING FUNCTIONS ---
private fun Timestamp.toFormattedTime(): String {
    // Use device default locale ensuring international support
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(this.toDate())
}