package com.stellarforge.composebooking.ui.screens.addeditservice

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.usecase.AddServiceUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.GetServiceDetailsUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateServiceUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AddEditServiceViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK private lateinit var getServiceDetailsUseCase: GetServiceDetailsUseCase
    @RelaxedMockK private lateinit var addServiceUseCase: AddServiceUseCase
    @RelaxedMockK private lateinit var updateServiceUseCase: UpdateServiceUseCase
    @RelaxedMockK private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: AddEditServiceViewModel

    private val testUser = AuthUser(uid = "test-owner-id")
    private val existingServiceId = "service-123"
    private val existingService = Service(
        id = existingServiceId,
        ownerId = testUser.uid,
        name = "Existing Service",
        description = "Old Desc",
        durationMinutes = 30,
        priceInCents = 2500L
    )

    @Before
    fun setUp() {
        coEvery { getCurrentUserUseCase() } returns Result.Success(testUser)
        coEvery { getServiceDetailsUseCase(any()) } returns Result.Success(existingService)
        coEvery { addServiceUseCase(any()) } returns Result.Success(Unit)
        coEvery { updateServiceUseCase(any()) } returns Result.Success(Unit)
    }

    private fun createViewModel(serviceId: String?) {
        val savedStateHandle = SavedStateHandle(mapOf("serviceId" to serviceId))
        viewModel = AddEditServiceViewModel(
            savedStateHandle,
            getServiceDetailsUseCase,
            addServiceUseCase,
            updateServiceUseCase,
            getCurrentUserUseCase
        )
    }

    @Test
    fun `init - in add mode - state is initialized with empty form`() = runTest {
        createViewModel(serviceId = null)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.screenTitle).isEqualTo("Yeni Servis Ekle")
        assertThat(state.name).isEmpty()
    }

    @Test
    fun `saveService - in add mode with valid data - calls addServiceUseCase and sets serviceSaved to true`() = runTest {
        createViewModel(serviceId = null)
        val serviceSlot = slot<Service>()

        viewModel.onNameChange("New Service")
        viewModel.onDurationChange("60")
        viewModel.onPriceChange("100.50")

        viewModel.saveService()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertThat(finalState.isSaving).isFalse()
        assertThat(finalState.serviceSaved).isTrue()

        coVerify(exactly = 1) { addServiceUseCase(capture(serviceSlot)) }
        coVerify(exactly = 0) { updateServiceUseCase(any()) }

        val capturedService = serviceSlot.captured
        assertThat(capturedService.name).isEqualTo("New Service")
        assertThat(capturedService.durationMinutes).isEqualTo(60)
        assertThat(capturedService.priceInCents).isEqualTo(10050L)
        assertThat(capturedService.ownerId).isEqualTo(testUser.uid)
    }

    // Düzenleme Modu Testleri
    @Test
    fun `init - in edit mode - loads service and fills form`() = runTest {
        createViewModel(serviceId = existingServiceId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.screenTitle).isEqualTo("Servisi Düzenle")
        assertThat(state.name).isEqualTo(existingService.name)
        assertThat(state.duration).isEqualTo(existingService.durationMinutes.toString())
        assertThat(state.price).isEqualTo("25.0")

        coVerify(exactly = 1) { getServiceDetailsUseCase(existingServiceId) }
    }

    @Test
    fun `saveService - in edit mode with valid data - calls updateServiceUseCase`() = runTest {
        createViewModel(serviceId = existingServiceId)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val serviceSlot = slot<Service>()

        viewModel.onNameChange("Updated Name")

        viewModel.saveService()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { updateServiceUseCase(capture(serviceSlot)) }
        coVerify(exactly = 0) { addServiceUseCase(any()) }

        val capturedService = serviceSlot.captured
        assertThat(capturedService.id).isEqualTo(existingServiceId)
        assertThat(capturedService.name).isEqualTo("Updated Name")

        assertThat(capturedService.priceInCents).isEqualTo(existingService.priceInCents)
    }

    //Hata Senaryoları
    @Test
    fun `saveService - with invalid price - sets error and does not call any use case`() = runTest {
        createViewModel(serviceId = null)
        viewModel.onNameChange("Valid Name")
        viewModel.onDurationChange("30")
        viewModel.onPriceChange("invalid_price")

        viewModel.saveService()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.serviceSaved).isFalse()
        assertThat(state.error).isEqualTo("Lütfen tüm zorunlu alanları doğru doldurun.")

        coVerify(exactly = 0) { addServiceUseCase(any()) }
        coVerify(exactly = 0) { updateServiceUseCase(any()) }
    }

    @Test
    fun `saveService - when use case fails - sets error state`() = runTest {
        val exception = Exception("DB write failed")
        coEvery { addServiceUseCase(any()) } returns Result.Error(exception, "Servis kaydedilemedi.")

        createViewModel(serviceId = null)
        viewModel.onNameChange("Valid Name")
        viewModel.onDurationChange("30")
        viewModel.onPriceChange("50")

        viewModel.saveService()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.serviceSaved).isFalse()
        assertThat(state.error).isEqualTo("Servis kaydedilemedi.")
    }
}