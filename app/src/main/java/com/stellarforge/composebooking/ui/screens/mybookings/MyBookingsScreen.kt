package com.stellarforge.composebooking.ui.screens.mybookings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.ui.components.AppBottomNavigationBar
import com.stellarforge.composebooking.ui.components.LoadingIndicator
import com.stellarforge.composebooking.utils.toFormattedPrice
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    navController: NavController,
    viewModel: MyBookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.my_bookings_tab_upcoming),
        stringResource(id = R.string.my_bookings_tab_past)
    )

    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is MyBookingsEvent.ShowSnackbar -> {
                    launch {
                        snackbarHostState.showSnackbar(context.getString(event.messageId))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.my_bookings_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_navigate_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AppBottomNavigationBar(navController = navController, userRole = "customer")
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }
                uiState.bookings != null -> {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(text = title) }
                            )
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> BookingList(
                            bookings = uiState.bookings!!.upcomingBookings,
                            isUpcoming = true,
                            onCancelBooking = { viewModel.cancelBooking(it) },
                            cancellingBookingId = uiState.isCancellingBookingId
                        )
                        1 -> BookingList(
                            bookings = uiState.bookings!!.pastBookings,
                            isUpcoming = false,
                            onCancelBooking = {}
                        )
                    }
                }
                uiState.error != null -> {
                    ErrorState(message = uiState.error!!, onRetry = { viewModel.loadMyBookings() })
                }
            }
        }
    }
}

@Composable
private fun BookingList(
    bookings: List<Appointment>,
    isUpcoming: Boolean,
    onCancelBooking: (String) -> Unit,
    cancellingBookingId: String? = null
) {
    if (bookings.isEmpty()) {
        val message = if (isUpcoming) R.string.my_bookings_no_upcoming else R.string.my_bookings_no_past
        EmptyState(messageResId = message)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(bookings, key = { it.id }) { booking ->
            BookingListItem(
                booking = booking,
                isUpcoming = isUpcoming,
                onCancelClick = { onCancelBooking(booking.id) },
                isBeingCancelled = booking.id == cancellingBookingId
            )
        }
    }
}

@Composable
private fun BookingListItem(
    booking: Appointment,
    isUpcoming: Boolean,
    onCancelClick: () -> Unit,
    isBeingCancelled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(booking.serviceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = stringResource(R.string.my_bookings_item_date), value = booking.appointmentDateTime.toFormattedDate())
            DetailRow(label = stringResource(R.string.my_bookings_item_time), value = booking.appointmentDateTime.toFormattedTime())
            DetailRow(label = stringResource(R.string.my_bookings_item_price), value = booking.servicePriceInCents.toFormattedPrice())

            if (isUpcoming) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.align(Alignment.End)) {
                    if (isBeingCancelled) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        OutlinedButton(
                            onClick = onCancelClick,
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Text(stringResource(id = R.string.my_bookings_cancel_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(@androidx.annotation.StringRes messageResId: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(id = messageResId),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
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

// --- YARDIMCI FORMATLAMA FONKSİYONLARI ---
private fun Timestamp.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr", "TR"))
    return sdf.format(this.toDate())
}

private fun Timestamp.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
    return sdf.format(this.toDate())
}