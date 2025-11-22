package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.utils.Result
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
    suspend operator fun invoke(email: String, password: String, role: String): Result<AuthUser> {
        // Temel format validasyonu ve şifre gücü kontrolü burada yapılabilir.
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(IllegalArgumentException("Email or password cannot be blank."))
        }

        if (role != "customer" && role != "owner") {
            return Result.Error(IllegalArgumentException("Invalid user role specified."))
        }

        return authRepository.signUpWithEmailPassword(email, password, role)
    }
}