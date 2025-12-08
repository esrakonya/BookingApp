package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [UpdateBusinessProfileUseCase].
 *
 * SECURITY CHECK:
 * Ensures that business profile updates are ALWAYS enforced on the target business ID
 * defined in [FirebaseConstants], regardless of the user ID requesting the change.
 * This prevents accidental overwrites or security exploits in a single-owner template model.
 */
class UpdateBusinessProfileUseCaseTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockRepo: BusinessProfileRepository
    @RelaxedMockK
    private lateinit var mockAuthRepo: AuthRepository

    private lateinit var useCase: UpdateBusinessProfileUseCase

    @Before
    fun setUp() {
        useCase = UpdateBusinessProfileUseCase(mockRepo, mockAuthRepo)
    }

    @Test
    fun `invoke - forces update to TARGET_BUSINESS_OWNER_ID regardless of logged in user uid`() = runTest {
        // ARRANGE
        val loggedInUser = AuthUser("random_user_123", "test@test.com")
        coEvery { mockAuthRepo.getCurrentUser() } returns Result.Success(loggedInUser)
        coEvery { mockRepo.updateBusinessProfile(any(), any()) } returns Result.Success(Unit)

        val profile = BusinessProfile(businessName = "New Name")

        // ACT
        useCase(profile)

        // ASSERT
        // Verification: The repository MUST be called with the constant TARGET_BUSINESS_OWNER_ID.
        coVerify {
            mockRepo.updateBusinessProfile(
                eq(FirebaseConstants.TARGET_BUSINESS_OWNER_ID), // <-- Security Enforcement Check
                any()
            )
        }
    }

    @Test
    fun `invoke - when user not logged in - returns Error`() = runTest {
        // ARRANGE
        coEvery { mockAuthRepo.getCurrentUser() } returns Result.Success(null)

        // ACT
        val result = useCase(BusinessProfile())

        // ASSERT
        assertThat(result).isInstanceOf(Result.Error::class.java)
        coVerify(exactly = 0) { mockRepo.updateBusinessProfile(any(), any()) }
    }
}