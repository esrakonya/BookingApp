package com.stellarforge.composebooking.ui.screens.businessprofile

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.domain.usecase.GetBusinessProfileUseCase
import com.stellarforge.composebooking.domain.usecase.GetCurrentUserUseCase
import com.stellarforge.composebooking.domain.usecase.UpdateBusinessProfileUseCase
import com.stellarforge.composebooking.utils.MainDispatcherRule // Kendi MainDispatcherRule'un yolu
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlinx.coroutines.test.TestDispatcher as TestDispatcher1

@ExperimentalCoroutinesApi
class BusinessProfileViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK
    private lateinit var getBusinessProfileUseCase: GetBusinessProfileUseCase

    @RelaxedMockK
    private lateinit var updateBusinessProfileUseCase: UpdateBusinessProfileUseCase

    @RelaxedMockK
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    private lateinit var viewModel: BusinessProfileViewModel

    // Testlerde kullanılacak sahte veriler
    private val fakeAuthUser = AuthUser(uid = "test_user_id", email = "test@test.com")
    private val fakeBusinessProfile = BusinessProfile(businessName = "Test Salon", contactEmail = "contact@test.com")

    /**
     * Her testte ViewModel'ı oluşturmak için kullanılan yardımcı fonksiyon.
     */
    private fun createViewModel() {
        viewModel =  BusinessProfileViewModel(
            getBusinessProfileUseCase = getBusinessProfileUseCase,
            updateBusinessProfileUseCase = updateBusinessProfileUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase
        )
    }

    // --- Profil Yükleme Testleri (loadBusinessProfile) ---

    @Test
    fun `init - when use cases succeed - loads profile and updates form fields`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        // DİKKAT: Flow döndüren fonksiyonlar 'every' ile mock edilmelidir.
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(fakeBusinessProfile))

        // ACT
        createViewModel()
        // ViewModel'ın init bloğundaki coroutine'lerin tamamlanması için zamanı ilerlet.
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        // Zaman ilerletildiği için, tüm ara state'ler geçti ve en son state'e ulaştık.
        // Bu yüzden doğrudan `uiState.value` ile son durumu kontrol ediyoruz.
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isEqualTo(fakeBusinessProfile)
        assertThat(finalState.loadErrorMessage).isNull()

        // Form alanlarının da dolduğunu kontrol et
        assertThat(viewModel.businessName.value).isEqualTo(fakeBusinessProfile.businessName)
        assertThat(viewModel.contactEmail.value).isEqualTo(fakeBusinessProfile.contactEmail)

        // UseCase'lerin doğru çağrıldığını doğrula
        coVerify(exactly = 1) { getCurrentUserUseCase() }
        coVerify(exactly = 1) { getBusinessProfileUseCase(fakeAuthUser.uid) }
    }

    @Test
    fun `init - when no existing profile - uiState is success with null data`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(null))

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isNull()
        assertThat(finalState.loadErrorMessage).isNull()

        assertThat(viewModel.businessName.value).isEmpty()
    }

    @Test
    fun `init - when getCurrentUser fails - uiState shows error`() = runTest {
        // ARRANGE
        val authException = Exception("Authentication failed")
        coEvery { getCurrentUserUseCase() } returns Result.Error(authException)

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isNull()
        assertThat(finalState.loadErrorMessage).isEqualTo("Kullanıcı bilgisi alınamadı.")

        // Kullanıcı alınamadığı için profil use case'i hiç çağrılmamalı.
        coVerify(exactly = 0) { getBusinessProfileUseCase(any()) }
    }

    @Test
    fun `saveBusinessProfile - when successful - shows success and reloads profile`() = runTest {
        // ARRANGE
        // 1. Başlangıçta yüklü bir profil olsun
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        // mockK'in 'andThen' özelliği ile, aynı fonksiyonun sonraki çağrılarda
        // farklı değerler döndürmesini sağlayabiliriz.
        val updatedProfile = fakeBusinessProfile.copy(businessName = "Updated Test Salon")
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns
                flowOf(Result.Success(fakeBusinessProfile)) andThen // İlk yükleme
                flowOf(Result.Success(updatedProfile))      // Yeniden yükleme

        // 2. ViewModel'ı oluştur ve ilk yüklemenin tamamlanmasını sağla
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // 3. Form alanını değiştir ve UpdateUseCase'i mock'la
        viewModel.onBusinessNameChanged("Updated Test Salon")
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Success(Unit)

        // ACT
        viewModel.saveBusinessProfile()
        // Kaydetme ve yeniden yükleme işlemlerinin tamamlanması için zamanı ilerlet
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isUpdatingProfile).isFalse()
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.updateSuccessMessage).isEqualTo("İşletme profili başarıyla güncellendi.")
        assertThat(finalState.profileData).isEqualTo(updatedProfile)

        // Form alanlarının da güncellendiğini doğrula
        assertThat(viewModel.businessName.value).isEqualTo("Updated Test Salon")

        // UpdateUseCase'in doğru parametrelerle çağrıldığını doğrula
        coVerify(exactly = 1) { updateBusinessProfileUseCase(any()) }
    }

    @Test
    fun `saveBusinessProfile - with blank business name - shows validation error and does not call use case`() = runTest {
        // ARRANGE
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(Result.Success(null))
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ACT
        viewModel.onBusinessNameChanged("  ") // Boş veya sadece boşluk içeren isim
        viewModel.saveBusinessProfile()
        // Bu işlem anında (bir coroutine içinde değil) gerçekleştiği için zamanı ilerletmeye gerek yok.

        // ASSERT
        val currentState = viewModel.uiState.value
        assertThat(currentState.isUpdatingProfile).isFalse()
        assertThat(currentState.updateErrorMessage).isEqualTo("İşletme adı boş bırakılamaz.")

        // UpdateUseCase'in HİÇ çağrılmadığını doğrula
        coVerify(exactly = 0) { updateBusinessProfileUseCase(any()) }
    }

    @Test
    fun `init - when getUser succeeds but getProfile fails - uiState shows load error`() = runTest {
        // SENARYO 1: Kullanıcıyı aldık ama profili Firestore'dan çekerken hata oldu.

        // ARRANGE
        val profileException = Exception("Bu mesajın bir önemi yok çünkü aşağıda ezeceğiz.")
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)

        // DÜZELTME: Result.Error'ı iki parametre ile oluşturuyoruz.
        // İkinci parametre, testimizin assert edeceği mesajdır.
        every { getBusinessProfileUseCase(fakeAuthUser.uid) } returns flowOf(
            Result.Error(profileException, "Profil yüklenemedi")
        )

        // ACT
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isLoadingProfile).isFalse()
        assertThat(finalState.profileData).isNull()
        // Artık bu doğrulama başarılı olacak çünkü ViewModel'a tam olarak bu mesajı verdik.
        assertThat(finalState.loadErrorMessage).isEqualTo("İşletme profili yüklenemedi.")
        assertThat(viewModel.businessName.value).isEmpty()
    }

    @Test
    fun `saveBusinessProfile - when updateUseCase fails - uiState shows update error`() = runTest {
        // SENARYO 2: Profili kaydetmeye çalışırken UseCase bir hata fırlattı.

        // ARRANGE
        val updateException = Exception("Bu mesajın da bir önemi yok.")
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))

        // DÜZELTME: Result.Error'ı yine iki parametre ile oluşturuyoruz.
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Error(
            updateException, "Profil güncellenemedi."
        )

        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ACT
        viewModel.onBusinessNameChanged("A Valid Name")
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // ASSERT
        val finalState = viewModel.uiState.value
        assertThat(finalState.isUpdatingProfile).isFalse()
        assertThat(finalState.updateSuccessMessage).isNull()
        // Bu doğrulama da artık başarılı olacak.
        assertThat(finalState.updateErrorMessage).isEqualTo("Profil güncellenemedi.")
    }

    @Test
    fun `clearUpdateMessages - after a success - clears success message`() = runTest {
        // SENARYO 3.1: Başarılı bir işlem sonrası mesajı temizleme.

        // ARRANGE: Başarılı bir kaydetme durumu oluştur
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null)) andThen flowOf(Result.Success(fakeBusinessProfile))
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Success(Unit)
        createViewModel()
        viewModel.onBusinessNameChanged("Some Name")
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Başlangıçta bir başarı mesajı olduğunu doğrula
        assertThat(viewModel.uiState.value.updateSuccessMessage).isNotNull()

        // ACT
        viewModel.clearUpdateMessages()
        // Bu senkron bir işlem olduğu için zamanı ilerletmeye gerek yok.

        // ASSERT
        // Mesajın temizlendiğini doğrula
        assertThat(viewModel.uiState.value.updateSuccessMessage).isNull()
        assertThat(viewModel.uiState.value.updateErrorMessage).isNull()
    }

    @Test
    fun `clearUpdateMessages - after an error - clears error message`() = runTest {
        // SENARYO 3.2: Hatalı bir işlem sonrası mesajı temizleme.

        // ARRANGE: Hatalı bir kaydetme durumu oluştur
        coEvery { getCurrentUserUseCase() } returns Result.Success(fakeAuthUser)
        every { getBusinessProfileUseCase(any()) } returns flowOf(Result.Success(null))
        coEvery { updateBusinessProfileUseCase(any()) } returns Result.Error(Exception("Test Error"))
        createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onBusinessNameChanged("Some Name")
        viewModel.saveBusinessProfile()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Başlangıçta bir hata mesajı olduğunu doğrula
        assertThat(viewModel.uiState.value.updateErrorMessage).isNotNull()

        // ACT
        viewModel.clearUpdateMessages()

        // ASSERT
        // Mesajın temizlendiğini doğrula
        assertThat(viewModel.uiState.value.updateSuccessMessage).isNull()
        assertThat(viewModel.uiState.value.updateErrorMessage).isNull()
    }
}