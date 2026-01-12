package com.stellarforge.composebooking.domain.usecase

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test

class UploadBusinessLogoUseCaseTest {

    private val repository: BusinessProfileRepository = mockk()
    private val useCase = UploadBusinessLogoUseCase(repository)

    @Test
    fun `invoke - calls repository uploadLogo and returns URL`() = runBlocking {
        // ARRANGE
        val uri = mockk<Uri>() // Mock Android Uri
        val ownerId = "owner_123"
        val expectedUrl = "https://firebasestorage.com/logo.jpg"

        coEvery { repository.uploadLogo(uri, ownerId) } returns Result.Success(expectedUrl)

        // ACT
        val result = useCase(uri, ownerId)

        // ASSERT
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(expectedUrl)

        coVerify(exactly = 1) { repository.uploadLogo(uri, ownerId) }
    }
}