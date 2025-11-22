package com.stellarforge.composebooking.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Long tipindeki kuruş değerini, kullanıcının yerel ayarlarına uygun bir para birimi
 * formatında (örn: "₺150,50") String'e çevirir.
 *
 * Kullanım:
 * val priceInCents: Long = 15050
 * val formattedPrice: String = priceInCents.toFormattedPrice() // Sonuç: "₺150,50"
 */
fun Long.toFormattedPrice(): String {
    // Türkiye için (TL) para birimi formatını alıyoruz.
    // Bu, şablonu alan kişinin kendi ülkesine göre (örn: Locale("en", "US") for $)
    // kolayca değiştirebileceği bir yerdir.
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

    // Kuruş değerini tam para birimine (örn: 15050 -> 150.50) çevirip formatlıyoruz.
    return currencyFormat.format(this / 100.0)
}