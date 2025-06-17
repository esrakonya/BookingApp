package com.stellarforge.composebooking.ui.screens.businessprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateBusinessProfileUseCase
import com.stellarforge.composebooking.utils.Result // Kendi Result sarmalayıcın
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class) // Sınıf seviyesinde OptIn
@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val getBusinessProfileUseCase: GetBusinessProfileUseCase,
    private val updateBusinessProfileUseCase: UpdateBusinessProfileUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState.asStateFlow()

    // Form alanları için ayrı MutableStateFlow'lar
    // Bunlar Composable ekranındaki TextField'larla iki yönlü veri bağlaması için kullanılacak.
    val businessName = MutableStateFlow("")
    val contactEmail = MutableStateFlow("")
    val contactPhone = MutableStateFlow("")
    val address = MutableStateFlow("")
    val logoUrl = MutableStateFlow("")

    init {
        Timber.d("BusinessProfileViewModel initialized.")
        // ViewModel başlatıldığında hemen profili yüklemeye başla
        // ve başlangıçta yükleme durumunu true yap.
        _uiState.update { it.copy(isLoadingProfile = true) }
        loadBusinessProfile()
    }

    /**
     * Mevcut kullanıcının işletme profilini yükler ve form alanlarını doldurur.
     */
    fun loadBusinessProfile() {
        Timber.d("BusinessProfileViewModel: loadBusinessProfile called.")
        // Yükleme başlamadan önce state'i güncelle (eğer init'te yapılmadıysa veya tekrar yükleme ise)
        _uiState.update { it.copy(isLoadingProfile = true, loadErrorMessage = null) }

        viewModelScope.launch {
            // 1. Mevcut kullanıcıyı al
            when (val userResult = getCurrentUserUseCase()) { // Bu Result<AuthUser?> döndürür
                is Result.Success -> {
                    val authUser = userResult.data
                    if (authUser != null && authUser.uid.isNotBlank()) {
                        Timber.d("BusinessProfileViewModel: Current user loaded: ${authUser.uid}. Now fetching profile.")
                        // 2. GetBusinessProfileUseCase'i ownerUserId ile çağır
                        getBusinessProfileUseCase(authUser.uid) // ownerUserId parametresi veriliyor
                            .collect { profileResult ->
                                Timber.d("BusinessProfileViewModel: Collected from GetBusinessProfileUseCase: $profileResult")
                                when (profileResult) {
                                    is Result.Loading -> {
                                        _uiState.update { it.copy(isLoadingProfile = true) }
                                    }
                                    is Result.Success -> {
                                        val profile = profileResult.data
                                        _uiState.update { currentState ->
                                            currentState.copy(
                                                isLoadingProfile = false,
                                                profileData = profile,
                                                loadErrorMessage = null
                                            )
                                        }
                                        updateFormFieldsFromProfile(profile) // Form alanlarını doldur/güncelle
                                        Timber.d("BusinessProfileViewModel: Profile loaded: $profile. Form fields updated.")
                                    }
                                    is Result.Error -> {
                                        Timber.e(profileResult.exception, "BusinessProfileViewModel: Error loading profile: ${profileResult.message}")
                                        _uiState.update {
                                            it.copy(
                                                isLoadingProfile = false,
                                                loadErrorMessage = profileResult.message ?: "İşletme profili yüklenemedi." // TODO: String kaynağı
                                            )
                                        }
                                        clearFormFields() // Hata durumunda form alanlarını temizle
                                    }
                                }
                            }
                    } else {
                        handleAuthErrorWhileLoading("Kullanıcı doğrulanmamış veya UID boş. Profil yüklenemiyor.")
                    }
                }
                is Result.Error -> {
                    handleAuthErrorWhileLoading("Mevcut kullanıcı bilgisi alınamadı. Profil yüklenemiyor.", userResult.exception)
                }
                is Result.Loading -> {
                    Timber.d("BusinessProfileViewModel: GetCurrentUserUseCase is loading.")
                    // _uiState.isLoadingProfile zaten true olmalı. Formu temizleyebiliriz.
                    clearFormFields()
                }
            }
        }
    }

    /**
     * Yüklenen profil verisine göre form alanlarını günceller.
     */
    private fun updateFormFieldsFromProfile(profile: BusinessProfile?) {
        profile?.let { loadedProfile ->
            businessName.value = loadedProfile.businessName
            contactEmail.value = loadedProfile.contactEmail ?: ""
            contactPhone.value = loadedProfile.contactPhone ?: ""
            address.value = loadedProfile.address ?: ""
            logoUrl.value = loadedProfile.logoUrl ?: ""
        } ?: run {
            // Profil null ise (yeni profil veya yükleme hatası sonrası), form alanlarını boşalt
            clearFormFields()
        }
    }

    /**
     * Form alanlarını temizler.
     */
    private fun clearFormFields() {
        Timber.d("BusinessProfileViewModel: Clearing form fields.")
        businessName.value = ""
        contactEmail.value = ""
        contactPhone.value = ""
        address.value = ""
        logoUrl.value = ""
    }

    /**
     * Profil yükleme sırasında oluşan kimlik doğrulama hatalarını yönetir.
     */
    private fun handleAuthErrorWhileLoading(logMessage: String, exception: Exception? = null) {
        Timber.w(exception, "BusinessProfileViewModel: $logMessage")
        _uiState.update { it.copy(isLoadingProfile = false, loadErrorMessage = logMessage, profileData = null) }
        clearFormFields()
    }

    // Form alanları için onValueChanged event handler'ları
    fun onBusinessNameChanged(name: String) { businessName.value = name }
    fun onContactEmailChanged(email: String) { contactEmail.value = email }
    fun onContactPhoneChanged(phone: String) { contactPhone.value = phone }
    fun onAddressChanged(addr: String) { address.value = addr }
    fun onLogoUrlChanged(url: String) { logoUrl.value = url }

    /**
     * Mevcut form verileriyle işletme profilini kaydeder veya günceller.
     */
    fun saveBusinessProfile() {
        Timber.d("BusinessProfileViewModel: saveBusinessProfile called.")
        val originalProfile = _uiState.value.profileData // Kayıtlı olan (veya olmayan) profil

        // Formdan gelen güncel değerlerle yeni bir BusinessProfile nesnesi oluştur
        // createdAt ve updatedAt alanları UpdateBusinessProfileUseCase içinde yönetilecek.
        val profileToSave = BusinessProfile(
            businessName = businessName.value.trim(),
            contactEmail = contactEmail.value.trim().takeIf { it.isNotBlank() },
            contactPhone = contactPhone.value.trim().takeIf { it.isNotBlank() },
            address = address.value.trim().takeIf { it.isNotBlank() },
            logoUrl = logoUrl.value.trim().takeIf { it.isNotBlank() },
            // createdAt'ı mevcut profilden al, eğer yoksa UseCase yeni oluştururken atayacak.
            createdAt = originalProfile?.createdAt,
            // updatedAt UseCase tarafından ayarlanacak.
            updatedAt = null
        )

        if (profileToSave.businessName.isBlank()) {
            _uiState.update {
                it.copy(
                    isUpdatingProfile = false,
                    updateErrorMessage = "İşletme adı boş bırakılamaz." // TODO: String kaynağı R.string.error_business_name_required
                )
            }
            Timber.w("BusinessProfileViewModel: Business name is blank, update cancelled.")
            return
        }

        _uiState.update {
            it.copy(
                isUpdatingProfile = true,
                updateSuccessMessage = null,
                updateErrorMessage = null
            )
        }

        viewModelScope.launch {
            Timber.d("BusinessProfileViewModel: Calling UpdateBusinessProfileUseCase with: $profileToSave")
            // UpdateBusinessProfileUseCase, authUser'ı kendi içinde alıyor ve timestamp'leri yönetiyor.
            when (val result = updateBusinessProfileUseCase(profileToSave)) {
                is Result.Success -> {
                    Timber.i("BusinessProfileViewModel: Business profile update successful.")
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            updateSuccessMessage = "İşletme profili başarıyla güncellendi.", // TODO: String kaynağı R.string.profile_update_success
                            updateErrorMessage = null
                        )
                    }
                    // Başarılı güncelleme sonrası Firestore'dan en son veriyi çekerek
                    // hem uiState.profileData'yı hem de form alanlarını tazeleyelim.
                    // Bu, özellikle `createdAt` gibi sunucu tarafında atanan değerlerin
                    // doğru yansımasını sağlar.
                    loadBusinessProfile()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "BusinessProfileViewModel: Error updating business profile: ${result.message}")
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            updateErrorMessage = result.message ?: "Profil güncellenemedi." // TODO: String kaynağı R.string.profile_update_failed
                        )
                    }
                }
                is Result.Loading -> {
                    // UpdateBusinessProfileUseCase suspend bir fonksiyon olduğu için
                    // bu case'e genellikle girilmez. isUpdatingProfile zaten true.
                    Timber.d("BusinessProfileViewModel: UpdateBusinessProfileUseCase returned Loading (unexpected).")
                }
            }
        }
    }

    /**
     * Güncelleme sonrası UI'da gösterilen başarı veya hata mesajlarını temizler.
     * Genellikle Snackbar mesajı gösterildikten sonra çağrılır.
     */
    fun clearUpdateMessages() {
        Timber.d("BusinessProfileViewModel: Clearing update messages.")
        _uiState.update { it.copy(updateSuccessMessage = null, updateErrorMessage = null) }
    }
}