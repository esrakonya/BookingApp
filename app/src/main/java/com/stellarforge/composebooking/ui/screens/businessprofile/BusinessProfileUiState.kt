package com.stellarforge.composebooking.ui.screens.businessprofile

import com.stellarforge.composebooking.data.model.BusinessProfile

/**
 * BusinessProfileScreen'in kullanıcı arayüzü durumunu temsil eder.
 */
data class BusinessProfileUiState(
    val isLoadingProfile: Boolean = true,
    val profileData: BusinessProfile? = null,
    val loadErrorMessage: String? = null,

    val isUpdatingProfile: Boolean = false,
    val updateSuccessMessage: String? = null,
    val updateErrorMessage: String? = null
)
