package com.stellarforge.composebooking.ui.screens.customerprofile.edit

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetCustomerProfileUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateCustomerProfileUseCase
import com.stellarforge.composebooking.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val isSaved: Boolean = false,
    @StringRes val errorResId: Int? = null
)

@HiltViewModel
class EditCustomerProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getCustomerProfileUseCase: GetCustomerProfileUseCase,
    private val updateCustomerProfileUseCase: UpdateCustomerProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        loadAndPreFillData()
    }

    /**
     * Loads the current user's profile and pre-fills the UI fields.
     * This ensures the user sees their existing data and can edit only what they want.
     */
    private fun loadAndPreFillData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Get User ID
            val authResult = getCurrentUserUseCase()
            if (authResult is Result.Success && authResult.data != null) {
                val uid = authResult.data.uid
                currentUserId = uid

                // 2. Fetch existing profile from Firestore
                try {
                    // We take the FIRST item from the flow to populate the form initially.
                    val profileFlow = getCustomerProfileUseCase(uid)

                    profileFlow.collect { result ->
                        if (result is Result.Success) {
                            val profile = result.data
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    // Pre-fill Name (Keep empty if null)
                                    name = profile.name ?: "",
                                    // Pre-fill Phone (Keep empty if null)
                                    phone = profile.phone ?: ""
                                )
                            }
                            // Stop collecting after first load. We don't want real-time updates
                            // to override what the user is currently typing.
                            return@collect
                        } else if (result is Result.Error) {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error pre-filling form")
                    _uiState.update { it.copy(isLoading = false) }
                }

            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorResId = R.string.error_user_not_found_generic)
                }
            }
        }
    }

    fun onNameChange(text: String) { _uiState.update { it.copy(name = text, errorResId = null) } }
    fun onPhoneChange(text: String) { _uiState.update { it.copy(phone = text, errorResId = null) } }

    fun saveProfile() {
        val uid = currentUserId ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = updateCustomerProfileUseCase(uid, state.name, state.phone)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                }
                is Result.Error -> {
                    val errorMsg = if (result.exception is IllegalArgumentException) {
                        R.string.error_booking_generic_problem
                    } else {
                        R.string.error_generic_unknown
                    }
                    _uiState.update { it.copy(isLoading = false, errorResId = errorMsg) }
                }
                is Result.Loading -> {}
            }
        }
    }
}