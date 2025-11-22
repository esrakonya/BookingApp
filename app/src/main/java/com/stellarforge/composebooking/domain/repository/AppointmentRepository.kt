package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.Service
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.Result

/**
 * Randevu ve Servis verileriyle ilgili işlemleri tanımlayan arayüz.
 * Uygulamanın geri kalanı bu arayüz üzerinden veri katmanıyla etkileşim kurar.
 */
interface AppointmentRepository {
    /**
     * Aktif olan tüm servislerin listesini Flow olarak döndürür.
     * Flow kullanmak, veritabanındaki değişikliklerin (eğer dinleniyorsa)
     * otomatik olarak UI'a yansımasını sağlar.
     * @return Servis listesini içeren bir Flow<List<Service>>.
     */
    fun getServices(): Flow<Result<List<Service>>>

    /**
     * Belirtilen ID'ye sahip tek bir servisin detaylarını getirir.
     * @param serviceId Getirilecek servisin ID'si.
     * @return Servis detayını veya hatayı içeren bir Result<Service>.
     */
    suspend fun getServiceDetails(serviceId: String): Result<Service>

    /**
     * İŞLETME SAHİBİ için: Belirli bir işletme sahibine ait TÜM servisleri (aktif/pasif) dinler.
     * Bu, işletme sahibinin "Servisleri Yönet" ekranı için kullanılacak.
     * @param ownerId İşletme sahibinin UID'si.
     */
    fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>>

    /**
     * Yeni bir servis ekler.
     * @param service Eklenecek servis verisi.
     */
    suspend fun addService(service: Service): Result<Unit>

    /**
     * Mevcut bir servisi günceller.
     * @param service Güncellenmiş servis verisi.
     */
    suspend fun updateService(service: Service): Result<Unit>

    /**
     * Bir servisi ID'sini kullanarak siler.
     * @param serviceId Silinecek servisin ID'si.
     */
    suspend fun deleteService(serviceId: String): Result<Unit>



    /**
     * Belirli bir tarihteki randevuları getirir.
     * @param date Randevuların alınacağı tarih.
     * @return Randevu listesini veya hatayı içeren bir Result<List<Appointment>>.
     *         Bu fonksiyon suspend veya Flow olabilir. Suspend daha basit olabilir.
     */
    suspend fun getAppointmentsForDate(ownerId: String, date: LocalDate): Result<List<Appointment>>

    /**
     * Belirli bir kullanıcıya ait tüm randevuları dinler.
     * @param userId Randevuları listelenecek müşterinin UID'si.
     */
    fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>>

    /**
     * Bir randevuyu ID'sini kullanarak siler.
     * @param appointmentId Silinecek randevunun ID'si.
     */
    suspend fun deleteAppointment(appointmentId: String): Result<Unit>



    suspend fun createAppointmentAndSlot(appointment: Appointment, slot: BookedSlot): Result<Unit>











}