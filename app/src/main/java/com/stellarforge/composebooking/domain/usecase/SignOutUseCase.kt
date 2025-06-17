package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * Oturumu kapatma işlemini gerçekleştiren Use Case.
 * SRP: Sadece oturum kapatma iş mantığını içerir.
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Use case'i fonksiyon gibi çağırmak için.
     * @return Başarılı olursa Result.success(Unit), veya hata.
     */
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}