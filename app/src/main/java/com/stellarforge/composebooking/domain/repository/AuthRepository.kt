package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.utils.Result

/**
 * Authentication işlemleri için Repository arayüzü.
 * DataSource detaylarını soyutlar ve UseCase'lere temiz bir API sunar.
 */
interface AuthRepository {

    /**
     * Mevcut giriş yapmış kullanıcıyı getirir.
     * @return Giriş yapmış kullanıcı varsa AuthUser, yoksa null içeren Result.
     */
    suspend fun getCurrentUser(): Result<AuthUser?>

    /**
     * E-posta ve şifre ile kullanıcı girişi yapar.
     * @param email Kullanıcı e-postası.
     * @param password Kullanıcı şifresi.
     * @return Başarılı giriş sonrası AuthUser içeren Result, veya hata.
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser>

    /**
     * E-posta ve şifre ile yeni kullanıcı kaydı yapar.
     * @param email Kullanıcı e-postası.
     * @param password Kullanıcı şifresi.
     * @return Başarılı kayıt sonrası AuthUser içeren Result, veya hata.
     */
    suspend fun signUpWithEmailPassword(email: String, password: String, role: String): Result<AuthUser>

    /**
     * Mevcut kullanıcının oturumunu kapatır.
     * @return Başarılı olursa Result.success(Unit), veya hata.
     */
    suspend fun signOut(): Result<Unit>
}