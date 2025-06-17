package com.stellarforge.composebooking.ui.screens.servicelist

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository // VEYA ServiceRepository (projenize göre doğru olanı seçin)
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase // YENİ
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.utils.Result // Kendi Result.kt dosyanızın importu
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ServiceListViewEvent ve ServiceListUiState sealed interface'leri aynı kalır
sealed interface ServiceListViewEvent {
    object NavigateToLogin : ServiceListViewEvent
    data class ShowSnackbar(@StringRes val messageResId: Int) : ServiceListViewEvent
}

sealed interface ServiceListUiState {
    object Loading : ServiceListUiState
    data class Success(val services: List<Service>) : ServiceListUiState
    data class Error(
        @StringRes val messageResId: Int,
        val exception: Exception? = null
    ) : ServiceListUiState
}

@HiltViewModel
class ServiceListViewModel @OptIn(ExperimentalCoroutinesApi::class)
@Inject constructor(
    private val serviceRepository: AppointmentRepository, // ADINI serviceRepository OLARAK DEĞİŞTİRDİM, DAHA GENEL
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getBusinessProfileUseCase: GetBusinessProfileUseCase // YENİ ENJEKSİYON
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServiceListUiState>(ServiceListUiState.Loading)
    val uiState: StateFlow<ServiceListUiState> = _uiState.asStateFlow()

    // YENİ: İşletme adını tutmak için StateFlow
    private val _businessName = MutableStateFlow<String?>(null)
    val businessName: StateFlow<String?> = _businessName.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ServiceListViewEvent>()
    val eventFlow: SharedFlow<ServiceListViewEvent> = _eventFlow.asSharedFlow()

    init {
        Timber.d("ServiceListViewModel initialized.")
        loadInitialData() // Hem servisleri hem de işletme profilini yükle
    }

    // `loadServices` yerine daha genel bir isim
    fun loadInitialData() {
        _uiState.value = ServiceListUiState.Loading
        _businessName.value = null // Başlangıçta işletme adını sıfırla
        Timber.d("ServiceListViewModel: Attempting to load initial data...")

        viewModelScope.launch {
            // Önce mevcut kullanıcıyı al
            when (val userResult = getCurrentUserUseCase()) { // userResult tipi: com.stellarforge.composebooking.utils.Result<AuthUser?>
                is Result.Success -> {
                    val authUser = userResult.data
                    if (authUser != null && authUser.uid.isNotBlank()) {
                        Timber.d("ServiceListViewModel: User authenticated (UID: ${authUser.uid}).")
                        // İşletme adını yükle (paralel olarak başlatılabilir)
                        launch { loadBusinessNameForCurrentUser(authUser.uid) }
                        // Servisleri yükle
                        loadServicesForCurrentUser(authUser)
                    } else {
                        handleUserAuthError("User is null or UID blank after GetCurrentUserUseCase success.")
                    }
                }
                is Result.Error -> {
                    handleUserAuthError("GetCurrentUserUseCase failed.", userResult.exception)
                }
                is Result.Loading -> {
                    Timber.d("ServiceListViewModel: GetCurrentUserUseCase is Loading. UI state already Loading.")
                    // _uiState.value zaten Loading olduğu için ek bir işlem yapmaya gerek yok.
                }
            }
        }
    }

    private suspend fun loadBusinessNameForCurrentUser(ownerUserId: String) {
        Timber.d("ServiceListViewModel: Loading business name for ownerUserId: $ownerUserId")
        // GetBusinessProfileUseCase'in ownerUserId parametresi aldığını varsayıyoruz
        getBusinessProfileUseCase(ownerUserId) // invoke(ownerUserId)
            .collect { profileResult ->
                when (profileResult) {
                    is Result.Success -> {
                        _businessName.value = profileResult.data?.businessName
                        Timber.i("ServiceListViewModel: Business name loaded: ${_businessName.value}")
                    }
                    is Result.Error -> {
                        Timber.e(profileResult.exception, "ServiceListViewModel: Failed to load business name. Message: ${profileResult.message}")
                        _businessName.value = null // Hata durumunda ismi temizle veya varsayılan bir şey ata
                    }
                    is Result.Loading -> {
                        Timber.d("ServiceListViewModel: GetBusinessProfileUseCase is Loading business name.")
                        // _businessName için ayrı bir loading state tutulabilir veya null kalabilir.
                    }
                }
            }
    }

    private suspend fun loadServicesForCurrentUser(authUser: AuthUser) {
        Timber.d("ServiceListViewModel: Loading services for user: ${authUser.uid}")
        // repository.getServices()'in Flow<com.stellarforge.composebooking.utils.Result<List<Service>>> döndürdüğünü varsayıyoruz
        // VE getServices metodunun artık ownerUserId parametresi aldığını varsayalım (eğer servisler kullanıcıya/işletmeye özgüyse)
        // Eğer getServices() ownerUserId almıyorsa ve tüm servisleri getiriyorsa, o zaman parametresiz çağırın.
        // Şimdilik, getServices()'ın tüm (aktif) servisleri getirdiğini varsayalım, çünkü işletme profili ayrı yükleniyor.
        serviceRepository.getServices() // Eğer ownerUserId gerekiyorsa: serviceRepository.getServices(authUser.uid)
            .retryWhen { cause, attempt ->
                Timber.d("retryWhen (services): attempt=$attempt, Cause type=${cause::class.simpleName}, Msg=${cause.message}")
                if (attempt < 1 && cause is FirebaseFirestoreException) { // Sadece 1 kere tekrar dene
                    if (cause.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                        cause.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) { // PERMISSION_DENIED geçici bir token sorunu olabilir
                        Timber.w(cause, "retryWhen (services): Retriable error. Delaying 300ms for retry.")
                        delay(300)
                        return@retryWhen true
                    }
                }
                Timber.d("retryWhen (services): Not retrying for this cause or max attempts reached.")
                return@retryWhen false
            }
            .catch { throwable: Throwable ->
                Timber.e(throwable, "ServiceListViewModel: Uncaught exception in getServices Flow stream.")
                val ex = if (throwable is Exception) throwable else Exception("Unexpected throwable: ${throwable.localizedMessage}", throwable)
                _uiState.value = ServiceListUiState.Error(R.string.error_loading_data, ex)
            }
            .collect { serviceLoadResult ->
                Timber.d("VM Collect (services): Received serviceLoadResult: $serviceLoadResult")
                when (serviceLoadResult) {
                    is Result.Success -> {
                        val services = serviceLoadResult.data
                        Timber.i("ServiceListViewModel: Successfully loaded ${services.size} services.")
                        _uiState.value = ServiceListUiState.Success(services)
                    }
                    is Result.Error -> {
                        Timber.e(serviceLoadResult.exception, "ServiceListViewModel: Failed to load services (Result.Error). Message: ${serviceLoadResult.message}")
                        val errorRes = determineFirestoreErrorMessage(serviceLoadResult.exception)
                        _uiState.value = ServiceListUiState.Error(errorRes, serviceLoadResult.exception)
                    }
                    is Result.Loading -> {
                        Timber.d("ServiceListViewModel: getServices Flow emitted Loading.")
                        // _uiState.value = ServiceListUiState.Loading // Ana Loading state'i zaten ayarlı olabilir.
                    }
                }
            }
    }

    private fun handleUserAuthError(logMessage: String, exception: Exception? = null) {
        Timber.e(exception, "ServiceListViewModel: $logMessage")
        val errorRes = determineAuthErrorMessage(exception)
        _uiState.value = ServiceListUiState.Error(errorRes, exception)
        _businessName.value = null // Kullanıcı hatası durumunda işletme adını da temizle
    }

    private fun determineFirestoreErrorMessage(exception: Exception?): Int {
        return when (exception) {
            is FirebaseFirestoreException -> when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.error_auth_permission_denied_services
                FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_network_connection_firestore
                else -> R.string.error_loading_data_firestore
            }
            else -> R.string.error_loading_data
        }
    }

    private fun determineAuthErrorMessage(exception: Exception?): Int {
        return when (exception) {
            is FirebaseAuthException -> R.string.error_auth_generic
            is FirebaseNetworkException -> R.string.error_network_connection
            else -> R.string.error_auth_user_not_found_for_services
        }
    }

    fun onRetryClicked() {
        Timber.d("ServiceListViewModel: Retry button clicked, reloading initial data.")
        loadInitialData() // Artık genel yükleme fonksiyonunu çağırıyoruz
    }

    fun signOut() {
        viewModelScope.launch {
            Timber.d("ServiceListViewModel: Signing out user...")
            when (val result = signOutUseCase()) {
                is Result.Success -> {
                    Timber.i("ServiceListViewModel: User signed out successfully.")
                    _eventFlow.emit(ServiceListViewEvent.NavigateToLogin)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "ServiceListViewModel: Sign out failed. Message: ${result.message}")
                    _eventFlow.emit(ServiceListViewEvent.ShowSnackbar(R.string.error_sign_out_failed))
                }
                is Result.Loading -> {
                    Timber.d("ServiceListViewModel: SignOutUseCase returned Loading (unexpected for suspend fun).")
                }
            }
        }
    }
}