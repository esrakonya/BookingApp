package com.stellarforge.composebooking.domain.usecase

import android.net.Uri
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.utils.Result
import javax.inject.Inject

/**
 * UseCase responsible for handling the business logo upload process.
 *
 * **Architectural Role:**
 * It acts as a clean bridge between the ViewModel (which provides the local file URI)
 * and the Repository (which handles the actual upload to Firebase Storage).
 *
 * This UseCase is currently a direct pass-through, but it is the designated place
 * for future business logic, such as:
 * - Validating image size or dimensions before upload.
 * - Applying watermarks.
 * - Triggering analytics events for uploads.
 */
class UploadBusinessLogoUseCase @Inject constructor(
    private val repository: BusinessProfileRepository
) {
    /**
     * Executes the logo upload operation.
     *
     * @param uri The local [Uri] of the image selected by the user from their device's gallery.
     * @param ownerId The unique identifier of the business owner, used for file path creation in Storage.
     * @return [Result.Success] containing the publicly accessible **Download URL** (String) if the upload is successful.
     */
    suspend operator fun invoke(uri: Uri, ownerId: String): Result<String> {
        return repository.uploadLogo(uri, ownerId)
    }
}