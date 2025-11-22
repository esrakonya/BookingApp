package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.BusinessProfile
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow

interface BusinessProfileRepository {
    /**
     * Belirtilen işletme sahibinin işletme profilini Firestore'dan alır ve
     * değişiklikleri anlık olarak yansıtan bir [Flow] olarak sunar.
     *
     * Flow içindeki her bir yayım, işlemin sonucunu [Result] ile sarmalar.
     *
     * @param ownerUserId İşletme sahibinin kimliği (AuthUser.uid).
     * @return İşletme profili varsa [Result.Success] içinde [BusinessProfile] içeren bir Flow,
     *         profil dokümanı yoksa [Result.Success(null)] içeren bir Flow,
     *         veya hata oluşursa [Result.Error] içeren bir Flow döner.
     */
    fun getBusinessProfile(ownerUserId: String): Flow<Result<BusinessProfile?>>

    /**
     * İşletme profilini Firestore'a yazar veya günceller.
     * Bu işlem, ilgili DataSource üzerinden gerçekleştirilir.
     *
     * @param ownerUserId İşletme sahibinin kimliği.
     * @param profile Kaydedilecek veya güncellenecek [BusinessProfile] nesnesi.
     * @return İşlem başarılı olursa [Result.Success] içinde Unit, hata oluşursa [Result.Error] döner.
     */
    suspend fun updateBusinessProfile(ownerUserId: String, profile: BusinessProfile): Result<Unit>
}