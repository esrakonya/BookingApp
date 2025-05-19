package com.stellarforge.composebooking.utils

object BusinessConstants {
    // --- İŞLETME ÇALIŞMA SAATLERİ ---
    // Sabah açılış saati (Saat, Dakika)
    val OPENING_TIME: java.time.LocalTime = java.time.LocalTime.of(9, 0)
    // Akşam kapanış saati (Saat, Dakika)
    // Not: Bu saat, son randevunun BİTEBİLECEĞİ en geç saattir,
    // son randevunun BAŞLAYABİLECEĞİ saat değildir.
    // Örneğin, kapanış 18:00 ve servis süresi 60dk ise, son randevu 17:00'de başlayabilir.
    val CLOSING_TIME: java.time.LocalTime = java.time.LocalTime.of(18, 0)

    // --- RANDEVU SLOT AYARLARI ---
    // Müsait saatlerin gösterileceği zaman aralığı (dakika cinsinden)
    // Örneğin, 15 ise saatler 09:00, 09:15, 09:30 şeklinde gösterilir.
    const val SLOT_INTERVAL_MINUTES = 15

    // --- GELECEKTE EKLENEBİLECEK ÖZELLEŞTİRME SEÇENEKLERİ (ÖRNEK) ---
    // const val MIN_BOOKING_NOTICE_HOURS = 2 // Randevunun en az kaç saat öncesinden alınabileceği
    // const val MAX_BOOKING_DAYS_AHEAD = 30 // En fazla kaç gün sonrasına randevu alınabileceği
    // val LUNCH_BREAK_START: java.time.LocalTime? = java.time.LocalTime.of(12, 30) // Öğle arası başlangıcı (null ise yok)
    // val LUNCH_BREAK_END: java.time.LocalTime? = java.time.LocalTime.of(13, 30)   // Öğle arası bitişi (null ise yok)
    // val NON_WORKING_DAYS: List<java.time.DayOfWeek> = listOf(java.time.DayOfWeek.SUNDAY) // Çalışılmayan günler
}