package com.stellarforge.composebooking.utils

/**
 * Utility object for centralized input validation logic.
 *
 * **Purpose:**
 * Ensures data integrity on the UI layer before it reaches the Domain layer.
 * Using a singleton object allows these functions to be easily accessed from any ViewModel
 * or Composable without dependency injection overhead.
 */
object ValidationUtils {

    // A standard, robust Regex for Email validation.
    // Allows alphanumeric characters, dots, underscores, and hyphens.
    // Supports modern top-level domains (e.g., .tech, .design) by not limiting extension length.
    private const val EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"

    /**
     * Checks if the provided char sequence matches a valid email format.
     *
     * @param email The input string to validate.
     * @return `true` if the email format is valid, `false` otherwise.
     */
    fun isEmailValid(email: CharSequence): Boolean {
        if (email.isBlank()) {
            return false
        }
        return Regex(EMAIL_PATTERN).matches(email)
    }

    /**
     * Checks if the provided password meets the minimum security length requirement.
     *
     * @param password The input string to validate.
     * @param minLength The required minimum length (defaults to 6 characters).
     * @return `true` if the password length is sufficient, `false` otherwise.
     */
    fun isPasswordLengthValid(password: CharSequence, minLength: Int = 6): Boolean {
        if (password.isBlank()) {
            return false
        }
        return password.length >= minLength
    }
}