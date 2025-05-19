package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.annotation.StringRes
import androidx.compose.material3.TimeInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository // Repository arayüzünü import et
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface ServiceListViewEvent {
    object NavigateToLogin : ServiceListViewEvent
}

sealed interface ServiceListUiState {
    object Loading : ServiceListUiState // Yükleniyor durumu
    data class Success(val services: List<Service>) : ServiceListUiState // Başarılı veri durumu
    data class Error(@StringRes val messageResId: Int) : ServiceListUiState // Hata durumu
}

@HiltViewModel
class ServiceListViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    private val signOutUseCase: SignOutUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // UI durumunu tutacak özel (private) MutableStateFlow
    private val _uiState = MutableStateFlow<ServiceListUiState>(ServiceListUiState.Loading)
    // UI'ın gözlemleyeceği public StateFlow (read-only)
    val uiState: StateFlow<ServiceListUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ServiceListViewEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // ViewModel oluşturulduğunda servisleri yüklemeye başla
    init {
        Timber.d("ServiceListViewModel initialized.")
        loadServices()
    }

    private fun loadServices() {
        viewModelScope.launch {
            _uiState.value = ServiceListUiState.Loading
            Timber.d("ServiceListViewModel: Attempting to load services...")

            val user = firebaseAuth.currentUser
            if (user == null) {
                Timber.e("ServiceListViewModel: User is null before loading services.")
                _uiState.value = ServiceListUiState.Error(R.string.error_auth_user_not_found_for_services)
                return@launch
            }

            Timber.d("ServiceListViewModel: User found (UID: ${user.uid}). Verifying ID token for service loading.")

            val token = user.getIdToken(true)
            if (token == null) {
                Timber.e("ServiceListViewModel: Failed to get token before loading services.")
                _uiState.value = ServiceListUiState.Error(R.string.error_auth_token_error_for_services)
                return@launch
            }

            Timber.d("ServiceListViewModel: Token confirmed. Proceeding with getServices.")

            repository.getServices()
                .retry(retries = 1) { cause ->
                    if (cause is FirebaseFirestoreException && cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.w(cause, "Permission denied on attempt. Retrying in 300ms...")
                        delay(300)
                        return@retry true
                    }
                    return@retry false
                }
                .catch { exception ->
                    Timber.e(exception, "Exception in getServices Flow stream (after token check and retries).")
                    _uiState.value = ServiceListUiState.Error(R.string.error_loading_data)
                }
                .collect { result ->
                    if (result.isSuccess) {
                        val services = result.getOrDefault(emptyList())
                        Timber.d("ServiceListViewModel: Successfully loaded ${services.size} services.")
                        _uiState.value = ServiceListUiState.Success(services)
                    } else {
                        Timber.e(result.exceptionOrNull(), "Failed to load services (Result.failure).")
                        if (result.exceptionOrNull() is FirebaseFirestoreException &&
                            (result.exceptionOrNull() as FirebaseFirestoreException).code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            _uiState.value = ServiceListUiState.Error(R.string.error_auth_permission_denied_services)
                        } else {
                            _uiState.value = ServiceListUiState.Error(R.string.error_loading_data)
                        }
                    }
                }
        }
    }


    fun onRetryClicked() {
        Timber.d("Retry button clicked, reloading services.")
        loadServices() // private olanı çağırır
    }

    fun signOut() {
        viewModelScope.launch {
            Timber.d("Signing out user...")
            val result = signOutUseCase()
            if (result.isSuccess) {
                Timber.i("User signed out successfully.")
                _eventFlow.emit(ServiceListViewEvent.NavigateToLogin)
            } else {
                Timber.e(result.exceptionOrNull(), "Sign out failed.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("ServiceListViewModel: onCleared CALLED. viewModelScope is being cancelled.")
        // viewModelScope.cancel() // Bu satır genellikle Hilt tarafından otomatik yapılır.
        // Ekstra çağırmak genellikle gereksiz ve bazen sorunlu olabilir.
        // ViewModel yok edildiğinde viewModelScope otomatik olarak iptal edilir.
    }
}