package com.stellarforge.composebooking.ui.screens.businessprofile

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.UploadBusinessLogoUseCase
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI State for the Business Profile Screen.
 * Uses Resource IDs for localized messages.
 */
data class BusinessProfileUiState(
    val isLoadingProfile: Boolean = true,
    val profileData: BusinessProfile? = null,
    @StringRes val loadErrorResId: Int? = null,

    val isUpdatingProfile: Boolean = false,
    @StringRes val updateSuccessResId: Int? = null,
    @StringRes val updateErrorResId: Int? = null
)

sealed interface BusinessProfileEvent {
    object NavigateToLogin: BusinessProfileEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val getBusinessProfileUseCase: GetBusinessProfileUseCase,
    private val updateBusinessProfileUseCase: UpdateBusinessProfileUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val uploadBusinessLogoUseCase: UploadBusinessLogoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessProfileUiState())
    val uiState: StateFlow<BusinessProfileUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BusinessProfileEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _isUploadingLogo = MutableStateFlow(false)
    val isUploadingLogo = _isUploadingLogo.asStateFlow()

    // Form Fields (Two-way binding)
    val businessName = MutableStateFlow("")
    val contactEmail = MutableStateFlow("")
    val contactPhone = MutableStateFlow("")
    val address = MutableStateFlow("")
    val logoUrl = MutableStateFlow("")

    init {
        Timber.d("BusinessProfileViewModel initialized.")
        loadBusinessProfile()
    }

    /**
     * Loads the current user's business profile.
     */
    fun loadBusinessProfile() {
        Timber.d("BusinessProfileViewModel: loadBusinessProfile called.")
        _uiState.update { it.copy(isLoadingProfile = true, loadErrorResId = null) }

        viewModelScope.launch {
            // 1. Get Current User
            val userResult = getCurrentUserUseCase()
            if (userResult is Result.Error) {
                Timber.w(userResult.exception, "Failed to fetch current user.")
                _uiState.update {
                    it.copy(
                        isLoadingProfile = false,
                        loadErrorResId = R.string.error_user_not_found_generic
                    )
                }
                clearFormFields()
                return@launch
            }

            val authUser = (userResult as Result.Success).data
            if (authUser == null || authUser.uid.isBlank()) {
                Timber.w("User is null or UID blank.")
                _uiState.update {
                    it.copy(
                        isLoadingProfile = false,
                        loadErrorResId = R.string.error_auth_user_not_found
                    )
                }
                clearFormFields()
                return@launch
            }

            // 2. Fetch Profile
            Timber.d("Current user loaded: ${authUser.uid}. Fetching profile.")
            getBusinessProfileUseCase(authUser.uid).collect { profileResult ->
                when(profileResult) {
                    is Result.Success -> {
                        val profile = profileResult.data
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoadingProfile = false,
                                profileData = profile,
                                loadErrorResId = null
                            )
                        }
                        updateFormFieldsFromProfile(profile)
                        Timber.d("Profile loaded. Form fields updated.")
                    }
                    is Result.Error -> {
                        Timber.e(profileResult.exception, "Error loading profile.")
                        _uiState.update {
                            it.copy(
                                isLoadingProfile = false,
                                loadErrorResId = R.string.error_loading_data_firestore
                            )
                        }
                        clearFormFields()
                    }
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoadingProfile = true) }
                    }
                }
            }
        }
    }

    fun onLogoSelected(uri: Uri) {
        viewModelScope.launch {
            val userResult = getCurrentUserUseCase()
            val userId = (userResult as? Result.Success)?.data?.uid ?: return@launch

            _isUploadingLogo.value = true

            when (val result = uploadBusinessLogoUseCase(uri, userId)) {
                is Result.Success -> {

                    logoUrl.value = result.data
                    _isUploadingLogo.value = false
                    saveBusinessProfile()
                }

                is Result.Error -> {
                    _isUploadingLogo.value = false
                    _uiState.update { it.copy(updateErrorResId = R.string.error_generic_unknown) }
                }

                is Result.Loading -> {}
            }
        }
    }

    private fun updateFormFieldsFromProfile(profile: BusinessProfile?) {
        profile?.let { loadedProfile ->
            businessName.value = loadedProfile.businessName
            contactEmail.value = loadedProfile.contactEmail ?: ""
            contactPhone.value = loadedProfile.contactPhone ?: ""
            address.value = loadedProfile.address ?: ""
            logoUrl.value = loadedProfile.logoUrl ?: ""
        } ?: run {
            clearFormFields()
        }
    }

    private fun clearFormFields() {
        businessName.value = ""
        contactEmail.value = ""
        contactPhone.value = ""
        address.value = ""
        logoUrl.value = ""
    }

    // Form Handlers
    fun onBusinessNameChanged(name: String) { businessName.value = name }
    fun onContactEmailChanged(email: String) { contactEmail.value = email }
    fun onContactPhoneChanged(phone: String) { contactPhone.value = phone }
    fun onAddressChanged(addr: String) { address.value = addr }
    fun onLogoUrlChanged(url: String) { logoUrl.value = url }

    /**
     * Saves or Updates the profile.
     */
    fun saveBusinessProfile() {
        Timber.d("BusinessProfileViewModel: saveBusinessProfile called.")
        val originalProfile = _uiState.value.profileData

        val profileToSave = BusinessProfile(
            businessName = businessName.value.trim(),
            contactEmail = contactEmail.value.trim().takeIf { it.isNotBlank() },
            contactPhone = contactPhone.value.trim().takeIf { it.isNotBlank() },
            address = address.value.trim().takeIf { it.isNotBlank() },
            logoUrl = logoUrl.value.trim().takeIf { it.isNotBlank() },
            createdAt = originalProfile?.createdAt,
            updatedAt = null // Will be set by UseCase
        )

        // Validation
        if (profileToSave.businessName.isBlank()) {
            _uiState.update {
                it.copy(
                    isUpdatingProfile = false,
                    updateErrorResId = R.string.label_business_name_required
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isUpdatingProfile = true,
                updateSuccessResId = null,
                updateErrorResId = null
            )
        }

        viewModelScope.launch {
            when (val result = updateBusinessProfileUseCase(profileToSave)) {
                is Result.Success -> {
                    Timber.i("Profile update successful.")
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            updateSuccessResId = R.string.profile_update_success,
                            updateErrorResId = null
                        )
                    }
                    // Refresh data to ensure consistency (especially timestamps)
                    loadBusinessProfile()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error updating profile.")
                    _uiState.update {
                        it.copy(
                            isUpdatingProfile = false,
                            updateErrorResId = R.string.profile_update_failed
                        )
                    }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun clearUpdateMessages() {
        _uiState.update { it.copy(updateSuccessResId = null, updateErrorResId = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            _eventFlow.emit(BusinessProfileEvent.NavigateToLogin)
        }
    }
}