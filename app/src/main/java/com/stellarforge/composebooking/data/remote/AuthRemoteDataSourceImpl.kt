package com.stellarforge.composebooking.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Concrete implementation of [AuthRemoteDataSource] using Firebase SDKs.
 *
 * **Key Architectural Pattern: Hybrid Authentication**
 *
 * Firebase Authentication handles the secure credential management (Email/Password),
 * but it doesn't natively store custom application data like "User Role" (Owner vs Customer)
 * in a way that is easily queryable or editable.
 *
 * Therefore, this class orchestrates a synchronized flow:
 * 1. **Identity:** Managed by [FirebaseAuth] (UID, Email).
 * 2. **Role & Profile:** Stored in [FirebaseFirestore] under the 'users' collection.
 *
 * Every time a user logs in or is checked, we fetch their ID from Auth and their Role from Firestore.
 */
class AuthRemoteDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRemoteDataSource {

    override suspend fun getCurrentUser(): Result<AuthUser?> {
        return try {
            val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
            // If user is logged in, verify role from Firestore
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
                // Fetch the role immediately after sign in
                Result.Success(convertFirebaseUserToAuthUser(firebaseUser))
            } else {
                Result.Error(Exception("Firebase Authentication returned null user after successful sign in."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String,
        role: String
    ): Result<AuthUser> {
        return try {
            // 1. Create User in Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // 2. Create User Document in Firestore with the Role
                val newUserForFirestore = hashMapOf(
                    "email" to firebaseUser.email,
                    "role" to role
                )
                firestore.collection(FirebaseConstants.USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .set(newUserForFirestore)
                    .await()

                // 3. Return Combined Result
                val authUser = AuthUser(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    isAnonymous = firebaseUser.isAnonymous,
                    role = role
                )
                Result.Success(authUser)
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

    /**
     * Bridges the gap between [FirebaseUser] and our domain [AuthUser].
     * Performs a network call to Firestore to fetch the user's role.
     */
    private suspend fun convertFirebaseUserToAuthUser(firebaseUser: FirebaseUser): AuthUser {
        val userDocument = firestore.collection(FirebaseConstants.USERS_COLLECTION)
            .document(firebaseUser.uid)
            .get()
            .await()

        // Default to "customer" if role is missing (Safety fallback)
        val role = userDocument.getString("role") ?: "customer"

        return AuthUser(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            isAnonymous = firebaseUser.isAnonymous,
            role = role
        )
    }
}