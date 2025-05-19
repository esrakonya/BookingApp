package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.repository.AuthRepository
import javax.inject.Inject

/**
 * E-posta ve şifre ile giriş yapma işlemini gerçekleştiren Use Case.
 * SRP: Sadece giriş yapma iş mantığını içerir.
 */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Use case'i fonksiyon gibi çağırmak için.
     * @param email Kullanıcı e-postası.
     * @param password Kullanıcı şifresi.
     * @return Başarılı giriş sonrası AuthUser içeren Result, veya hata.
     */
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> {
        // Temel format validasyonu burada yapılabilir (opsiyonel, ViewModel'da da yapılabilir)
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password cannot be blank."))
            // Veya daha spesifik bir exception türü / hata kodu döndürülebilir.
        }
        // E-posta format kontrolü de eklenebilir.

        return authRepository.signInWithEmailPassword(email, password)
    }
}