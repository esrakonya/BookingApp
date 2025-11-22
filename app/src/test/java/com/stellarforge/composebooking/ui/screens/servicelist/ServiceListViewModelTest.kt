// ServiceListViewModelTest.kt

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListUiState
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListViewEvent
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListViewModel
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.utils.MainDispatcherRule
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds // Turbine için

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var mockServiceRepository: AppointmentRepository

    @RelaxedMockK
    private lateinit var mockSignOutUseCase: SignOutUseCase

    @RelaxedMockK
    private lateinit var mockGetCurrentUserUseCase: GetCurrentUserUseCase

    @RelaxedMockK
    private lateinit var mockGetBusinessProfileUseCase: GetBusinessProfileUseCase

    private lateinit var viewModel: ServiceListViewModel

    // Test verileri
    private val fakeAuthUser = AuthUser("test_uid", "test@test.com")
    private val fakeServices = listOf(Service(id = "s1", name = "Service 1"))

    @Before
    fun setUp() {
        // FirebaseFirestoreException mock'u (statik olduğu için)
        mockkStatic(FirebaseFirestoreException.Code::class)
        every { FirebaseFirestoreException.Code.values() } returns emptyArray()

        // Timber'ı test logları için ayarla
        Timber.uprootAll()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                println("TIMBER: $message") // Test loglarını basitleştirelim
                t?.printStackTrace()
            }
        })
        Timber.d("setUp: Başladı.")
    }

    @After
    fun tearDown() {
        Timber.d("tearDown: Başladı.")
        unmockkStatic(FirebaseFirestoreException.Code::class)
        Timber.uprootAll()
    }

    // ViewModel'ı oluşturan yardımcı fonksiyon (YENİ VIEWMODEL'A GÖRE)
    private fun createViewModel() {
        viewModel = ServiceListViewModel(
            serviceRepository = mockServiceRepository,
            signOutUseCase = mockSignOutUseCase,
            getCurrentUserUseCase = mockGetCurrentUserUseCase, // YENİ
            getBusinessProfileUseCase = mockGetBusinessProfileUseCase  // YENİ
        )
        Timber.d("createViewModel: ViewModel oluşturuldu.")
    }

    // --- TEMEL AUTH VE BAŞARILI YÜKLEME SENARYOLARI ---
    @Test
    fun `init - when user exists - loads initial data successfully`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        coEvery { mockGetBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(null)) // İşletme adı şimdilik önemli değil
        coEvery { mockServiceRepository.getServices() } returns flowOf(Result.Success(fakeServices))

        // ACT
        createViewModel() // init bloğu loadInitialData'yı tetikler

        // ASSERT
        viewModel.uiState.test {
            // Başlangıç Loading state'i
            assertEquals(ServiceListUiState.Loading, awaitItem())

            // Başarılı yükleme sonrası Success state'i
            val successState = awaitItem()
            assertTrue("State should be Success", successState is ServiceListUiState.Success)
            assertEquals(fakeServices, (successState as ServiceListUiState.Success).services)

            cancelAndIgnoreRemainingEvents()
        }

        // UseCase ve Repository çağrılarını doğrula
        coVerify(exactly = 1) { mockGetCurrentUserUseCase() }
        coVerify(exactly = 1) { mockGetBusinessProfileUseCase(fakeAuthUser.uid) }
        coVerify(exactly = 1) { mockServiceRepository.getServices() }
    }

    @Test
    fun `init - when getCurrentUser fails - uiState is Error`() = mainDispatcherRule.testScope.runTest { // Kendi scope'umuzu kullan
        // ARRANGE
        val authException = Exception("Auth failed")
        coEvery { mockGetCurrentUserUseCase() } returns Result.Error(authException)

        // ACT
        createViewModel()
        // ViewModel'ın init'i tetiklendi ama içindeki launch henüz çalışmadı.

        // ASSERT - Başlangıç durumunu kontrol et
        var currentState = viewModel.uiState.value
        assertTrue("Initial state should be Loading", currentState is ServiceListUiState.Loading)

        // Coroutine scheduler'ını ilerleterek viewModelScope.launch'ın çalışmasını sağla
        mainDispatcherRule.scheduler.advanceUntilIdle()

        // ASSERT - Son durumu kontrol et
        currentState = viewModel.uiState.value
        assertTrue("Final state should be Error, but was $currentState", currentState is ServiceListUiState.Error)
        assertEquals(
            R.string.error_auth_user_not_found_for_services,
            (currentState as ServiceListUiState.Error).messageResId
        )

        // Orijinal exception'ın da state'e eklendiğini kontrol edelim
        assertEquals(authException, (currentState as ServiceListUiState.Error).exception)

        coVerify(exactly = 0) { mockGetBusinessProfileUseCase(any()) }
        coVerify(exactly = 0) { mockServiceRepository.getServices() }
    }

    @Test
    fun `onRetryClicked - reloads initial data`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        coEvery { mockGetBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))
        coEvery { mockServiceRepository.getServices() } returns flowOf(Result.Success(fakeServices))
        createViewModel()
        advanceUntilIdle() // İlk yüklemenin bitmesini bekle

        // ACT
        viewModel.onRetryClicked()

        // ASSERT
        viewModel.uiState.test {
            // Tekrar deneme sonrası ilk state Loading olur
            assertEquals(ServiceListUiState.Loading, awaitItem())
            // Sonra Success gelir
            assertTrue(awaitItem() is ServiceListUiState.Success)
        }

        // UseCase'lerin TOPLAMDA 2 kere çağrıldığını doğrula (init + retry)
        coVerify(exactly = 2) { mockGetCurrentUserUseCase() }
        coVerify(exactly = 2) { mockServiceRepository.getServices() }
    }

    @Test
    fun `signOut - when use case succeeds - emits NavigateToLogin event`() = runTest {
        // ARRANGE
        coEvery { mockGetCurrentUserUseCase() } returns Result.Success(fakeAuthUser) // init'te hata vermemesi için
        coEvery { mockSignOutUseCase() } returns Result.Success(Unit)
        createViewModel()
        advanceUntilIdle()

        // ACT & ASSERT
        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.signOut()
            assertEquals(ServiceListViewEvent.NavigateToLogin, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 1) { mockSignOutUseCase() }
    }
}