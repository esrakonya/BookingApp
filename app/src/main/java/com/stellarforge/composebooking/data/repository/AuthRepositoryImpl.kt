package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.remote.AuthRemoteDataSource
import com.stellarforge.composebooking.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * AuthRepository arayüzünün somut implementasyonu.
 * İşlemleri AuthRemoteDataSource'a delege eder.
 * SRP: Sadece Auth verisi erişimini soyutlar.
 * DIP: AuthRemoteDataSource'a (somut sınıf) bağlı, ancak UseCase'ler bu implementasyona değil,
 *      AuthRepository arayüzüne bağımlı olacak.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    override suspend fun getCurrentUser(): Result<AuthUser?> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.getCurrentUser()
        }
    }

    override suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<AuthUser> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.signInWithEmailPassword(email, password)
        }
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String
    ): Result<AuthUser> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.signUpWithEmailPassword(email, password)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return withContext(ioDispatcher) {
            authRemoteDataSource.signOut()
        }
    }

}