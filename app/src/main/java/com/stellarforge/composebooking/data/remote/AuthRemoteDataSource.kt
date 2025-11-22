package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.utils.Result

interface AuthRemoteDataSource {
    /**
     * Mevcut giriş yapmış kullanıcıyı getirir.
     * @return Giriş yapmış kullanıcı varsa AuthUser, yoksa null içeren Result.
     */
    suspend fun getCurrentUser(): Result<AuthUser?>

    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser>

    suspend fun signUpWithEmailPassword(email: String, password: String, role: String): Result<AuthUser>

    suspend fun signOut(): Result<Unit>
}