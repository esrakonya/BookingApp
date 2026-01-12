package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.CustomerProfileRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

class UpdateCustomerProfileUseCase @Inject constructor(
    private val customerProfileRepository: CustomerProfileRepository
) {
    suspend operator fun invoke(userId: String, name: String, phone: String): Result<Unit> {
        if (name.isBlank()) {
            return Result.Error(IllegalArgumentException("Name cannot be empty."))
        }
        if (phone.isBlank()) {
            return Result.Error(IllegalArgumentException("Phone cannot be empty."))
        }

        return customerProfileRepository.updateCustomerProfile(userId, name.trim(), phone.trim())
    }
}