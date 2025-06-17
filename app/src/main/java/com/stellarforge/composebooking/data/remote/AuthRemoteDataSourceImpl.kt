package com.stellarforge.composebooking.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AuthRemoteDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRemoteDataSource {
    override suspend fun getCurrentUser(): Result<AuthUser?> {
        return try {
            val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
            val authUser: AuthUser? = firebaseUser?.let { convertFirebaseUserToAuthUser(it) }
            Result.Success(authUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.Success(convertFirebaseUserToAuthUser(firebaseUser))
            } else {
                Result.Error(Exception("Firebase Authentication returned null user after successful sign in."))
            }
        } catch (e: CancellationException) {
            throw  e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String
    ): Result<AuthUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                Result.Success(convertFirebaseUserToAuthUser(firebaseUser))
            } else {
                Result.Error(Exception("Firebase Authentication returned null user after successful sign up."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun convertFirebaseUserToAuthUser(firebaseUser: FirebaseUser): AuthUser {
        return AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            isAnonymous = firebaseUser.isAnonymous
        )
    }

}