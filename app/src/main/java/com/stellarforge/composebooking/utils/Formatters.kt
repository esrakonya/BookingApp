package com.stellarforge.composebooking.utils

import com.google.firebase.Timestamp
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Extension functions for formatting Currency, Date, and Time.
 *
 * **Purpose:**
 * Centralizes all formatting logic to ensure consistency across the app
 * and proper localization support.
 */

/**
 * Converts a price in cents (Long) to a localized currency string.
 * Example: 15050 -> "$150.50" (US) or "₺150,50" (TR).
 *
 * @receiver The price in the smallest currency unit (e.g., cents, kuruş).
 * @return Formatted String with currency symbol.
 */
fun Long.toFormattedPrice(): String {
    // 1. Get the currency format.
    // TODO for Developers: If your business operates in a specific currency (e.g., only EUR),
    // replace 'Locale.getDefault()' with 'Locale.GERMANY' or 'Locale("tr", "TR")' etc.
    // Currently, it adapts to the user's device settings.
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    // 2. Convert cents to major unit (100 cents = 1.00)
    return currencyFormat.format(this / 100.0)
}

/**
 * Converts Firestore Timestamp to a readable Time string.
 * Automatically respects the user's 12h/24h preference.
 *
 * Example: "2:30 PM" (US) or "14:30" (EU).
 */
fun Timestamp?.toFormattedTime(): String {
    if (this == null) return ""

    // SHORT style usually gives "HH:mm" or "h:mm a"
    val formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
    return formatter.format(this.toDate())
}

/**
 * Converts Firestore Timestamp to a readable, full Date string.
 * Automatically respects the user's language and date order preferences.
 *
 * Example: "Sunday, December 7, 2025" (US) or "7 Aralık 2025 Pazar" (TR).
 */
fun Timestamp?.toFormattedDate(): String {
    if (this == null) return ""

    // FULL style gives the most verbose and friendly date format
    val formatter = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault())
    return formatter.format(this.toDate())
}