package com.stellarforge.composebooking.ui.screens.servicelist

import app.cash.turbine.test
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase // Yeni import
import com.stellarforge.composebooking.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock Bağımlılıklar
    private lateinit var mockRepository: AppointmentRepository
    private lateinit var mockSignOutUseCase: SignOutUseCase // SignOutUseCase için mock
    // Test Edilecek Sınıf
    private lateinit var viewModel: ServiceListViewModel

    @Before
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        mockSignOutUseCase = mockk(relaxed = true) // Mock oluşturuldu
        // Başarılı sign out varsayalım (relaxed=true zaten yapar)
        coEvery { mockSignOutUseCase() } returns Result.success(Unit)
    }

    // --- Testler ---

    @Test
    fun `init loads services and updates state to Success when repository returns success`() = runTest {
        // Arrange
        val fakeServices = listOf(Service(id = "1", name = "Service 1"))
        coEvery { mockRepository.getServices() } returns flowOf(Result.success(fakeServices))

        // Act - ViewModel oluşturulurken mockSignOutUseCase'i de verin
        viewModel = ServiceListViewModel(mockRepository, mockSignOutUseCase)

        // Assert
        viewModel.uiState.test(timeout = 3.seconds) {
            assertEquals(ServiceListUiState.Loading, awaitItem()) // Initial Loading
            val successState = awaitItem() // Collect'ten Success
            assertTrue("Expected Success, but was $successState", successState is ServiceListUiState.Success)
            assertEquals(fakeServices, (successState as ServiceListUiState.Success).services)
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 1) { mockRepository.getServices() }
    }

    @Test
    fun `init loads services and updates state to Error when repository returns failure`() = runTest {
        // Arrange
        val exception = Exception("Database connection error from mock")
        coEvery { mockRepository.getServices() } returns flowOf(Result.failure(exception))

        // Act - ViewModel oluşturulurken mockSignOutUseCase'i de verin
        viewModel = ServiceListViewModel(mockRepository, mockSignOutUseCase)

        // Assert
        viewModel.uiState.test(timeout = 3.seconds) {
            assertEquals(ServiceListUiState.Loading, awaitItem()) // Initial Loading
            val errorState = awaitItem() // Collect'ten Error
            assertTrue("Expected Error, but was $errorState", errorState is ServiceListUiState.Error)
            assertEquals(R.string.error_loading_data, (errorState as ServiceListUiState.Error).messageResId)
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 1) { mockRepository.getServices() }
    }

    @Test
    fun `init loads services and updates state to Error when repository flow throws exception`() = runTest {
        // Arrange
        val exception = IOException("Network error from mock flow")
        coEvery { mockRepository.getServices() } returns flow { throw exception }

        // Act - ViewModel oluşturulurken mockSignOutUseCase'i de verin
        viewModel = ServiceListViewModel(mockRepository, mockSignOutUseCase)

        // Assert
        viewModel.uiState.test(timeout = 3.seconds) {
            assertEquals(ServiceListUiState.Loading, awaitItem()) // Initial Loading
            val errorState = awaitItem() // Catch'ten Error
            assertTrue("Expected Error, but was $errorState", errorState is ServiceListUiState.Error)
            assertEquals(R.string.error_loading_data, (errorState as ServiceListUiState.Error).messageResId)
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 1) { mockRepository.getServices() }
    }

    @Test
    fun `onRetryClicked reloads services successfully after initial failure`() = runTest {
        // Arrange
        val initialException = Exception("Initial load failed")
        val successServices = listOf(Service(id = "s1", name = "Retry Success Service"))
        coEvery { mockRepository.getServices() } returnsMany listOf(
            flowOf(Result.failure(initialException)),
            flowOf(Result.success(successServices))
        )

        // Act - ViewModel oluştur
        viewModel = ServiceListViewModel(mockRepository, mockSignOutUseCase)

        // Assert - init sonrası durumu kontrol et
        viewModel.uiState.test(timeout = 5.seconds) {
            assertEquals(ServiceListUiState.Loading, awaitItem()) // Initial Loading
            val firstErrorState = awaitItem() // Collect'ten Error
            assertTrue(firstErrorState is ServiceListUiState.Error)

            // Act - Retry'ı tetikle
            viewModel.onRetryClicked()

            // Assert - onRetryClicked sonrası durumu doğrula
            assertEquals(ServiceListUiState.Loading, awaitItem()) // onRetry -> loadServices -> Loading set
            val successState = awaitItem() // onRetry -> loadServices -> collect -> Success
            assertTrue("Expected Success after retry, but was $successState", successState is ServiceListUiState.Success)
            assertEquals(successServices, (successState as ServiceListUiState.Success).services)

            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 2) { mockRepository.getServices() }
    }

    // --- YENİ TESTLER (Sign Out İçin) ---
    @Test
    fun `signOut when use case succeeds emits NavigateToLogin event`() = runTest {
        // Arrange
        coEvery { mockSignOutUseCase() } returns Result.success(Unit) // Başarılı signOut
        viewModel = ServiceListViewModel(mockRepository, mockSignOutUseCase) // ViewModel'ı oluştur

        // Act & Assert
        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.signOut() // Oturumu kapat
            assertEquals(ServiceListViewEvent.NavigateToLogin, awaitItem()) // Event'i bekle
            expectNoEvents()
        }
        coVerify(exactly = 1) { mockSignOutUseCase() } // UseCase çağrıldı mı?
    }

    @Test
    fun `signOut when use case fails does not emit NavigateToLogin event`() = runTest {
        // Arrange
        val exception = Exception("Sign out failed")
        coEvery { mockSignOutUseCase() } returns Result.failure(exception) // Başarısız signOut ayarlandı
        viewModel = ServiceListViewModel(mockRepository, mockSignOutUseCase) // ViewModel oluşturuldu

        // Act & Assert - EventFlow kısmı doğru görünüyor
        viewModel.eventFlow.test(timeout = 3.seconds) {
            // ---- ACT KISMI BURADA MI? ----
            viewModel.signOut() // Bu satırın burada olduğundan emin olun!
            // ------------------------------
            advanceUntilIdle() // signOut içindeki coroutine'in bitmesini bekle (emin olmak için)
            expectNoEvents() // Hiçbir event emit edilmemeli
        }

        // ---- VERIFY KISMI ----
        // coVerify'den önce advanceUntilIdle() gerekebilir mi? Genellikle runTest içinde gerekmez.
        coVerify(exactly = 1) { mockSignOutUseCase() } // UseCase'in çağrıldığını doğrula
        // ---------------------
    }
}