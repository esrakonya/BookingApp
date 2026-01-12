package com.stellarforge.composebooking.data.repository

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.AuthUser
import com.stellarforge.composebooking.data.remote.AuthRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.utils.UserPrefs
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {

    private val remoteDataSource: AuthRemoteDataSource = mockk()
    private val userPrefs: UserPrefs = mockk(relaxed = true)
    private val ioDispatcher = Dispatchers.Unconfined

    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        repository = AuthRepositoryImpl(remoteDataSource, ioDispatcher, userPrefs)
    }

    @Test
    fun `signIn - saves user role to prefs on success`() = runBlocking {
        // ARRANGE
        val user = AuthUser("123", "test@test.com", role = "owner")
        coEvery { remoteDataSource.signInWithEmailPassword(any(), any()) } returns Result.Success(user)

        // ACT
        val result = repository.signInWithEmailPassword("email", "pass")

        // ASSERT
        assertThat(result).isInstanceOf(Result.Success::class.java)

        verify(exactly = 1) { userPrefs.saveUserRole("owner") }
    }

    @Test
    fun `signOut - clears user prefs`() = runBlocking {
        // ARRANGE
        coEvery { remoteDataSource.signOut() } returns Result.Success(Unit)

        // ACT
        repository.signOut()

        // ASSERT
        verify(exactly = 1) { userPrefs.clear() }
    }
}