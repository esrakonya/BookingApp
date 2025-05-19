package com.stellarforge.composebooking.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.stellarforge.composebooking.data.model.AuthUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.reflect.typeOf

class AuthRemoteDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Mevcut giriş yapmış kullanıcıyı getirir.
     * @return Giriş yapmış kullanıcı varsa AuthUser, yoksa null içeren Result.
     */
    suspend fun getCurrentUser(): Result<AuthUser?> {
        return try {
            val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
            val authUser: AuthUser? = firebaseUser?.let { convertFirebaseUserToAuthUser(it) }
            Result.success(authUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailPassword(email: String, password: String): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(convertFirebaseUserToAuthUser(firebaseUser))
            } else {
                Result.failure(Exception("Firebase Authentication returned null user after successful sign in."))
            }
        } catch (e: CancellationException) {
            throw  e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmailPassword(email: String, password: String): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.success(convertFirebaseUserToAuthUser(firebaseUser))
            } else {
                Result.failure(Exception("Firebase Authentication returned null user after successful sign up."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * FirebaseUser nesnesini kendi AuthUser modelimize dönüştüren yardımcı fonksiyon.
     * (Extension function yerine normal private metot)
     */
    private fun convertFirebaseUserToAuthUser(firebaseUser: FirebaseUser): AuthUser {
        return AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            isAnonymous = firebaseUser.isAnonymous
        )
    }
}