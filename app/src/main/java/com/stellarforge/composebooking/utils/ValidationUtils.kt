package com.stellarforge.composebooking.utils

/**
 * Girdi validasyonu için yardımcı fonksiyonları içerir.
 */
object ValidationUtils { // Fonksiyonları gruplamak için bir object kullanabiliriz
    // veya direkt top-level fonksiyonlar da olabilir.
    // object kullanmak, ValidationUtils.isEmailValid gibi çağırmayı sağlar.

    /**
     * Verilen karakter dizisinin geçerli bir e-posta formatında olup olmadığını kontrol eder.
     * @param email Kontrol edilecek e-posta.
     * @return E-posta formatı geçerliyse true, değilse false.
     */
    fun isEmailValid(email: CharSequence): Boolean {
        if (email.isBlank()) {
            return false
        }
        // Genel kabul görmüş bir e-posta regex'i.
        // İhtiyaçlarınıza göre bu regex'i daha da geliştirebilir veya basitleştirebilirsiniz.
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,6}$")
        return emailRegex.matches(email)
    }

    /**
     * Verilen şifrenin minimum uzunluk kuralını karşılayıp karşılamadığını kontrol eder.
     * @param password Kontrol edilecek şifre.
     * @param minLength Gerekli minimum uzunluk (varsayılan 6).
     * @return Şifre minimum uzunluktaysa veya daha uzunsa true, değilse false.
     */
    fun isPasswordLengthValid(password: CharSequence, minLength: Int = 6): Boolean {
        if (password.isBlank()) {
            return false // Boş şifre genellikle geçersizdir, ama bu kontrol ViewModel'da ayrıca yapılabilir.
            // Bu fonksiyon sadece uzunluğa odaklanabilir.
        }
        return password.length >= minLength
    }

    // Gelecekte başka validasyon fonksiyonları eklenebilir:
    // fun isPhoneNumberValid(phone: CharSequence): Boolean { ... }
    // fun doPasswordsMatch(password: CharSequence, confirmPassword: CharSequence): Boolean { ... }
    // (Şifre eşleşme kontrolü zaten SignUpViewModel'da yapılıyor, burada da olabilirdi)
}