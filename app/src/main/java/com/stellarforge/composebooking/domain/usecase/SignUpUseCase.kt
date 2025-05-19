package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.repository.AuthRepository
import javax.inject.Inject

/**
 * E-posta ve şifre ile yeni kullanıcı kaydı yapan Use Case.
 * SRP: Sadece kayıt olma iş mantığını içerir.
 */
class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Use case'i fonksiyon gibi çağırmak için.
     * @param email Kullanıcı e-postası.
     * @param password Kullanıcı şifresi.
     * @return Başarılı kayıt sonrası AuthUser içeren Result, veya hata.
     */
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> {
        // Temel format validasyonu ve şifre gücü kontrolü burada yapılabilir.
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email or password cannot be blank."))
        }

        return authRepository.signUpWithEmailPassword(email, password)
    }
}