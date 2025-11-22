package com.stellarforge.composebooking.ui.screens.manageservices

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.DeleteServiceUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetOwnerServicesUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ManageServicesViewModelTest {

    @get: Rule
    val mockkRule = MockKRule(this)

    @get: Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK private lateinit var getOwnerServicesUseCase: GetOwnerServicesUseCase
    @RelaxedMockK private lateinit var deleteServiceUseCase: DeleteServiceUseCase
    @RelaxedMockK private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: ManageServicesViewModel

    private val testUser = AuthUser(uid = "test-owner-id")
    private val testServices = listOf(
        Service(id = "s1", name = "Service A", ownerId = testUser.uid),
        Service(id = "s2", name = "Service B", ownerId = testUser.uid)
    )

    @Before
    fun setUp() {
        coEvery { getCurrentUserUseCase() } returns Result.Success(testUser)
        every { getOwnerServicesUseCase(testUser.uid) } returns flowOf(Result.Success(testServices))
        coEvery { deleteServiceUseCase(any()) } returns Result.Success(Unit)
    }

    private fun createViewModel() {
        viewModel = ManageServicesViewModel(
            getOwnerServicesUseCase,
            deleteServiceUseCase,
            getCurrentUserUseCase
        )
    }

    @Test
    fun `init - when user is authenticated - loads services successfully`() = runTest {
        createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val successState = awaitItem()

            assertThat(successState.isLoading).isFalse()
            assertThat(successState.services).isEqualTo(testServices)
            assertThat(successState.error).isNull()
        }

        coVerify(exactly = 1) { getCurrentUserUseCase() }
        verify(exactly = 1) { getOwnerServicesUseCase(testUser.uid) }
    }

    @Test
    fun `init - when user is not authenticated - sets error state`() = runTest {
        coEvery { getCurrentUserUseCase() } returns Result.Success(null)

        createViewModel()

        viewModel.uiState.test {
            skipItems(1) //Başlangıçtaki isLoading=true durumunu atla
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.services).isEmpty()
            assertThat(errorState.error).isEqualTo("Kullanıcı bulunamadı. Lütfen tekrar giriş yapın.")
        }

        verify(exactly = 0) { getOwnerServicesUseCase(any()) }
    }

    @Test
    fun `init - when getOwnerServicesUseCase fails - sets error state`() = runTest {
        val exception = Exception("DB error")
        every { getOwnerServicesUseCase(testUser.uid) } returns flowOf(Result.Error(exception, "Servisler yüklenemedi."))

        createViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.services).isEmpty()
            assertThat(errorState.error).isEqualTo("Servisler yüklenemedi.")
        }
    }

    @Test
    fun `deleteService - when successful - calls use case and updates deleting state`() = runTest {
        createViewModel()

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.services).isEqualTo(testServices)

        viewModel.uiState.test {
            //val initialState = awaitItem()
            //assertThat(initialState.services).isEqualTo(testServices)
            skipItems(1)

            viewModel.deleteService("s1")

            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            val deletingState = awaitItem()
            assertThat(deletingState.isDeletingServiceId).isEqualTo("s1")


            val finalState = awaitItem()
            assertThat(finalState.isDeletingServiceId).isNull()
        }

        coVerify(exactly = 1) { deleteServiceUseCase("s1") }
    }

    @Test
    fun `deleteService - when use case fails - sets error state and resets deleting state`() = runTest {
        val exception = Exception("Deletion failed")
        coEvery { deleteServiceUseCase("s1") } returns Result.Error(exception, "Servis silinemedi.")
        createViewModel()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.deleteService("s1")

            assertThat(awaitItem().isDeletingServiceId).isEqualTo("s1")

            val errorState = awaitItem()
            assertThat(errorState.error).isEqualTo("Servis silinemedi.")

            val finalState = awaitItem()
            assertThat(finalState.isDeletingServiceId).isNull()
        }
    }
}