package com.stellarforge.composebooking.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [GetBusinessProfileUseCase].
 * Verifies the data flow from Repository to ViewModel.
 */
class GetBusinessProfileUseCaseTest {
    private val mockRepo = mockk<BusinessProfileRepository>()
    private val useCase = GetBusinessProfileUseCase(mockRepo)

    @Test
    fun `invoke - calls repository and emits correct flow states`() = runTest {
        // ARRANGE
        val profile = BusinessProfile(businessName = "Test Owner")

        // Mock: Return a successful flow from the repository
        every { mockRepo.getBusinessProfile("owner1") } returns flowOf(Result.Success(profile))

        // ACT & ASSERT
        useCase("owner1").test {
            // 1. First emission must be Loading (Ensures UI shows a spinner)
            val loadingItem = awaitItem()
            assertThat(loadingItem).isInstanceOf(Result.Loading::class.java)

            // 2. Second emission must be Success with data
            val result = awaitItem()
            assertThat(result).isInstanceOf(Result.Success::class.java)
            assertThat((result as Result.Success).data).isEqualTo(profile)

            awaitComplete()
        }
    }
}