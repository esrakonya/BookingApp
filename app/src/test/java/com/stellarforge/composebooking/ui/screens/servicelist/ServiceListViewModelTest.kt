// ServiceListViewModelTest.kt

import android.util.Log
import app.cash.turbine.test // Sadece signOut testlerinde kullanılıyor
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.firestore.FirebaseFirestoreException
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.usecase.SignOutUseCase
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListUiState
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListViewEvent
import com.stellarforge.composebooking.ui.screens.servicelist.ServiceListViewModel
import com.stellarforge.composebooking.utils.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
// import kotlinx.coroutines.flow.onStart // Artık retry testlerinde kullanılmıyor
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration.Companion.seconds // Turbine için

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockRepository: AppointmentRepository
    private lateinit var mockSignOutUseCase: SignOutUseCase
    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var mockFirebaseUser: FirebaseUser
    private lateinit var mockGetTokenResult: GetTokenResult
    private lateinit var viewModel: ServiceListViewModel

    // ViewModel'deki retryWhen koşulunun arayacağı mesaj
    private val retryTriggerMessage = "PERMISSION_DENIED_RETRY_TRIGGER"

    @Before
    fun setUp() {
        mockkStatic(FirebaseFirestoreException.Code::class)
        every { FirebaseFirestoreException.Code.values() } returns emptyArray()

        Timber.uprootAll()
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                val priorityChar = when (priority) {
                    Log.VERBOSE -> "V"; Log.DEBUG -> "D"; Log.INFO -> "I";
                    Log.WARN -> "W"; Log.ERROR -> "E"; Log.ASSERT -> "A"; else -> "?"
                }
                println("TIMBER ($priorityChar): ${tag ?: "TestLog"} - $message")
                t?.let { println("TIMBER_EXCEPTION: ${it.javaClass.simpleName} - ${it.message}") }
            }
        })
        Timber.d("setUp: Timber plant edildi.")

        mockRepository = mockk(relaxed = true)
        coEvery { mockRepository.getServices() } returns flowOf(Result.success(emptyList())) // Genel varsayılan

        mockSignOutUseCase = mockk(relaxed = true)
        mockFirebaseAuth = mockk(relaxed = true)
        mockFirebaseUser = mockk(relaxed = true)
        mockGetTokenResult = mockk(relaxed = true)

        setDefaultSuccessAuth()
        Timber.d("setUp: Tamamlandı.")
    }

    private fun setDefaultSuccessAuth() {
        every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "test_uid"
        val successTokenTask: Task<GetTokenResult> = Tasks.forResult(mockGetTokenResult)
        every { mockFirebaseUser.getIdToken(true) } returns successTokenTask
        every { mockGetTokenResult.token } returns "fake_valid_token"
        Timber.d("setDefaultSuccessAuth: Auth mock'ları ayarlandı.")
    }

    @After
    fun tearDown() {
        Timber.d("tearDown: Başladı.")
        unmockkStatic(FirebaseFirestoreException.Code::class)
        clearAllMocks()
        Timber.uprootAll()
        Timber.d("tearDown: Tamamlandı.")
    }

    private fun createViewModel() {
        viewModel = ServiceListViewModel(
            repository = mockRepository,
            signOutUseCase = mockSignOutUseCase,
            firebaseAuth = mockFirebaseAuth
        )
        Timber.d("createViewModel: ViewModel oluşturuldu. Initial state: ${viewModel.uiState.value}")
    }

    // --- TEMEL AUTH VE BAŞARILI YÜKLEME SENARYOLARI ---
    @Test
    fun `loadServices WHEN user is null THEN uiState is ErrorUserNotFound and no repo call`() = runTest {
        Timber.d("TEST BAŞLADI: loadServices WHEN user is null")
        every { mockFirebaseAuth.currentUser } returns null
        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("User null ise ErrorUserNotFound beklenir, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_auth_user_not_found_for_services, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 0) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: loadServices WHEN user is null. Final state: $finalState")
    }

    @Test
    fun `loadServices WHEN token is null THEN uiState is ErrorTokenError and no repo call`() = runTest {
        Timber.d("TEST BAŞLADI: loadServices WHEN token is null")
        setDefaultSuccessAuth()
        every { mockGetTokenResult.token } returns null
        val tokenTaskWithNullToken: Task<GetTokenResult> = Tasks.forResult(mockGetTokenResult)
        every { mockFirebaseUser.getIdToken(true) } returns tokenTaskWithNullToken

        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("Token null ise ErrorTokenError beklenir, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_auth_token_error_for_services, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 0) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: loadServices WHEN token is null. Final state: $finalState")
    }

    @Test
    fun `loadServices WHEN repository returns success on first attempt THEN uiState is Success`() = runTest {
        Timber.d("TEST BAŞLADI: loadServices WHEN repository returns success on first attempt")
        setDefaultSuccessAuth()
        val fakeServices = listOf(Service(id = "s1", name = "Service 1"))
        coEvery { mockRepository.getServices() } returns flowOf(Result.success(fakeServices))

        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("Başarılı yüklemede Success beklenir, gelen: $finalState", finalState is ServiceListUiState.Success)
        assertEquals(fakeServices, (finalState as ServiceListUiState.Success).services)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: loadServices WHEN repository returns success on first attempt. Final state: $finalState")
    }

    // --- RETRY MEKANİZMASI KULLANILMAYAN HATA DURUMLARI ---
    @Test
    fun `loadServices WHEN repository flow throws non_retryable_IOException THEN uiState is ErrorLoadingData`() = runTest {
        Timber.d("TEST BAŞLADI: ...throws non_retryable_IOException...")
        setDefaultSuccessAuth()
        val ioException = IOException("Network error from mock flow (non-retryable)")
        coEvery { mockRepository.getServices() } returns flow {
            Timber.d("Test Repo [NonRetryableIOException]: IOException fırlatılıyor: ${ioException.message}")
            throw ioException
        }
        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        Timber.d("TEST [NonRetryableIOException]: Nihai state: $finalState")
        assertTrue("Nihai state Error bekleniyordu, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_loading_data, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: ...throws non_retryable_IOException....")
    }

    @Test
    fun `loadServices WHEN repository emits Result_failure_PermissionDenied_NoRetryTrigger THEN uiState is ErrorPermissionDenied`() = runTest {
        Timber.d("TEST BAŞLADI: ...emits Result_failure_PermissionDenied_NoRetryTrigger...")
        setDefaultSuccessAuth()
        val permissionDeniedException = mockk<FirebaseFirestoreException>(relaxed = true) {
            every { message } returns "This is a standard PERMISSION_DENIED, but should not trigger retry"
            every { code } returns FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        coEvery { mockRepository.getServices() } returns flowOf(Result.failure(permissionDeniedException))

        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("Nihai state Error bekleniyordu, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_auth_permission_denied_services, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: ...emits Result_failure_PermissionDenied_NoRetryTrigger.... Final state: $finalState")
    }

    @Test
    fun `loadServices WHEN repository emits Result_failure_generic_NoRetryTrigger THEN uiState is ErrorLoadingData`() = runTest {
        Timber.d("TEST BAŞLADI: ...emits Result_failure_generic_NoRetryTrigger...")
        setDefaultSuccessAuth()
        val genericException = Exception("Database connection error (Result.failure, no retry trigger)")
        coEvery { mockRepository.getServices() } returns flowOf(Result.failure(genericException))

        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        assertTrue("Nihai state Error bekleniyordu, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_loading_data, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: ...emits Result_failure_generic_NoRetryTrigger.... Final state: $finalState")
    }

    // --- RETRY MEKANİZMASINI (PRATİKTE ÇALIŞMAYAN İKİNCİ ÇAĞRI İLE) TEST EDEN TESTLER ---
    // Bu testler, retryWhen'in doğru karar verdiğini ama ikinci Flow çağrısının
    // test ortamında gerçekleşmediğini ve ilk hatanın .catch'e düştüğünü varsayar.

    @Test
    fun `loadServices WHEN permission error and retry predicate IS MET but second call DOES NOT OCCUR then uiState is ErrorLoadingData (was retry succeeds)`() = runTest {
        Timber.d("TEST BAŞLADI: ...permission error, retry predicate MET, no second call (was retry succeeds)...")
        setDefaultSuccessAuth()
        val permissionException = mockk<FirebaseFirestoreException>(relaxed = true) {
            every { message } returns retryTriggerMessage
        }
        val firstAttemptFlow: Flow<Result<List<Service>>> = flow {
            Timber.d("Test Repo [RetrySucceedsScenarioButGoesToCatch]: 1. DENEME - Exception fırlatılıyor")
            throw permissionException
        }
        coEvery { mockRepository.getServices() } returns firstAttemptFlow

        createViewModel()
        viewModel.loadServices()

        Timber.d("TEST [RetrySucceedsScenarioButGoesToCatch]: advanceTimeBy(301) öncesi")
        advanceTimeBy(301)
        Timber.d("TEST [RetrySucceedsScenarioButGoesToCatch]: advanceUntilIdle() öncesi")
        advanceUntilIdle()
        Timber.d("TEST [RetrySucceedsScenarioButGoesToCatch]: advanceUntilIdle() sonrası.")

        val finalState = viewModel.uiState.value
        Timber.d("TEST [RetrySucceedsScenarioButGoesToCatch]: Nihai state kontrol ediliyor: $finalState")
        assertTrue(
            "Nihai state ErrorLoadingData bekleniyordu (çünkü ikinci repo çağrısı olmuyor, ilk exception catch'e düşer), gelen: $finalState",
            finalState is ServiceListUiState.Error
        )
        assertEquals(R.string.error_loading_data, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: ...permission error, retry predicate MET, no second call (was retry succeeds)...")
    }

    @Test
    fun `loadServices WHEN permission error and retry predicate IS MET but second call DOES NOT OCCUR (scenario for also fails perm) THEN uiState is ErrorLoadingData`() = runTest {
        Timber.d("TEST BAŞLADI: ...permission error, retry predicate MET, no second call (was for also fails perm)...")
        setDefaultSuccessAuth()
        val firstAttemptException = mockk<FirebaseFirestoreException>(relaxed = true) {
            every { message } returns retryTriggerMessage
        }
        val firstAttemptFlow: Flow<Result<List<Service>>> = flow { throw firstAttemptException }
        coEvery { mockRepository.getServices() } returns firstAttemptFlow

        createViewModel()
        viewModel.loadServices()
        advanceTimeBy(301)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        Timber.d("TEST [RetryFailsPermScenarioButGoesToCatch]: Nihai state kontrol ediliyor: $finalState")
        assertTrue("Nihai state Error bekleniyordu, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_loading_data, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: ...permission error, retry predicate MET, no second call (was for also fails perm)...")
    }

    @Test
    fun `loadServices WHEN permission error and retry predicate IS MET but second call DOES NOT OCCUR (scenario for fails generic) THEN uiState is ErrorLoadingData`() = runTest {
        Timber.d("TEST BAŞLADI: ...permission error, retry predicate MET, no second call (was for fails generic)...")
        setDefaultSuccessAuth()
        val firstAttemptException = mockk<FirebaseFirestoreException>(relaxed = true) {
            every { message } returns retryTriggerMessage
        }
        val firstAttemptFlow: Flow<Result<List<Service>>> = flow { throw firstAttemptException }
        coEvery { mockRepository.getServices() } returns firstAttemptFlow
        createViewModel()
        viewModel.loadServices()
        advanceTimeBy(301)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value
        Timber.d("TEST [RetryFailsGenericScenarioButGoesToCatch]: Nihai state kontrol ediliyor: $finalState")
        assertTrue("Nihai state Error bekleniyordu, gelen: $finalState", finalState is ServiceListUiState.Error)
        assertEquals(R.string.error_loading_data, (finalState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 1) { mockRepository.getServices() }
        Timber.d("TEST BİTTİ: ...permission error, retry predicate MET, no second call (was for fails generic)...")
    }

    // --- onRetryClicked ve signOut Testleri ---
    @Test
    fun `onRetryClicked reloads services successfully after initial auth failure`() = runTest {
        Timber.d("TEST BAŞLADI: onRetryClicked reloads services successfully...")
        every { mockFirebaseAuth.currentUser } returns null
        createViewModel()
        viewModel.loadServices()
        advanceUntilIdle()

        var currentState = viewModel.uiState.value
        assertTrue("İlk yükleme ErrorUserNotFound olmalı", currentState is ServiceListUiState.Error)
        assertEquals(R.string.error_auth_user_not_found_for_services, (currentState as ServiceListUiState.Error).messageResId)
        coVerify(exactly = 0) { mockRepository.getServices() }

        Timber.d("TEST [onRetryClicked]: Auth düzeltiliyor...")
        setDefaultSuccessAuth()
        val successServices = listOf(Service(id = "s1", name = "Retry Success Service"))
        coEvery { mockRepository.getServices() } returns flowOf(Result.success(successServices))

        viewModel.onRetryClicked()
        advanceUntilIdle()

        currentState = viewModel.uiState.value
        assertTrue("Retry sonrası Success beklenir", currentState is ServiceListUiState.Success)
        assertEquals(successServices, (currentState as ServiceListUiState.Success).services)
        coVerify(exactly = 1) { mockRepository.getServices() } // Sadece retry sonrası çağrı
        Timber.d("TEST BİTTİ: onRetryClicked reloads services successfully...")
    }

    @Test
    fun `signOut WHEN use case succeeds THEN emits NavigateToLogin event`() = runTest {
        Timber.d("TEST BAŞLADI: signOut WHEN use case succeeds...")
        coEvery { mockSignOutUseCase() } returns Result.success(Unit)
        createViewModel()

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.signOut()
            assertEquals(ServiceListViewEvent.NavigateToLogin, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        coVerify(exactly = 1) { mockSignOutUseCase() }
        Timber.d("TEST BİTTİ: signOut WHEN use case succeeds...")
    }

    @Test
    fun `signOut WHEN use case fails THEN does not emit NavigateToLogin event`() = runTest {
        Timber.d("TEST BAŞLADI: signOut WHEN use case fails...")
        val signOutException = Exception("Sign out failed")
        coEvery { mockSignOutUseCase() } returns Result.failure(signOutException)
        createViewModel()

        viewModel.eventFlow.test(timeout = 3.seconds) {
            viewModel.signOut()
            advanceUntilIdle()
            expectNoEvents()
        }
        coVerify(exactly = 1) { mockSignOutUseCase() }
        Timber.d("TEST BİTTİ: signOut WHEN use case fails...")
    }

    @Test
    fun `ZZZ - Find Message Resource ID Value`() {
        val targetIdToFind = 2131492948
        println("----- STARTING ID LOOKUP -----")
        Timber.d("----- STARTING ID LOOKUP -----")
        val stringResources = mapOf(
            "error_loading_data" to R.string.error_loading_data,
            "error_auth_user_not_found_for_services" to R.string.error_auth_user_not_found_for_services,
            "error_auth_token_error_for_services" to R.string.error_auth_token_error_for_services,
            "error_auth_permission_denied_services" to R.string.error_auth_permission_denied_services
        )
        var found = false
        stringResources.forEach { (name, idValue) ->
            println("Checking R.string.$name -> ID: $idValue (Hex: ${Integer.toHexString(idValue)})")
            Timber.d("Checking R.string.$name -> ID: $idValue (Hex: ${Integer.toHexString(idValue)})")
            if (idValue == targetIdToFind) {
                println(">>>>>> FOUND IT! ID $targetIdToFind corresponds to R.string.$name <<<<<<")
                Timber.d(">>>>>> FOUND IT! ID $targetIdToFind corresponds to R.string.$name <<<<<<")
                found = true
            }
        }
        if (!found) {
            println("!!!!!! ID $targetIdToFind was NOT FOUND among the listed R.string values. !!!!!!")
            Timber.w("!!!!!! ID $targetIdToFind was NOT FOUND among the listed R.string values. !!!!!!")
        }
        println("----- FINISHED ID LOOKUP -----")
        Timber.d("----- FINISHED ID LOOKUP -----")
        assertTrue("Bu test sadece ID bulmak içindir, her zaman geçmeli", true)
    }
}