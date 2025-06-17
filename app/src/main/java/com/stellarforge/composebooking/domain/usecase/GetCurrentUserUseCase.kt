package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * Mevcut giriş yapmış kullanıcıyı almak için kullanılan Use Case.
 * SRP: Sadece mevcut kullanıcıyı alma iş mantığını içerir (bu durumda repository'ye delegasyon).
 */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Use case'i fonksiyon gibi çağırmak için.
     * @return Giriş yapmış kullanıcı varsa AuthUser, yoksa null içeren Result.
     */
    suspend operator fun invoke(): Result<AuthUser?> {
        // İleride burada ek iş mantığı (örn. kullanıcı rolü kontrolü) eklenebilir.
        return authRepository.getCurrentUser()
    }
}