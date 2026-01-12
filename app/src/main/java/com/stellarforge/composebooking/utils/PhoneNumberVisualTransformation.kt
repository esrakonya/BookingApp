package com.stellarforge.composebooking.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A VisualTransformation to format raw phone input into a readable styling.
 *
 * **CURRENT FORMAT (US/International):**
 * Input:  5550105555  (10 digits)
 * Output: +1 (555) 010-5555
 *
 * ==============================================================================
 * 🛠️ CUSTOMIZATION GUIDE FOR BUYERS
 * ==============================================================================
 * To change this to your country's format (e.g., UK, India, Turkey):
 *
 * 1. Modify the `filter` function:
 *    - Change "+1 (" to your country code (e.g., "+44 ", "+90 ").
 *    - Adjust the positions of parentheses `()` and hyphens `-`.
 *
 * 2. Modify the `offsetTranslator` object:
 *    - Update the index calculations to match your new special characters.
 *    - This ensures the cursor stays in the correct position while typing.
 * ==============================================================================
 */
class PhoneNumberVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // 1. Filter: Only allow digits
        val trimmed = if (text.text.length >= 10) {
            text.text.substring(0..9)
        } else {
            text.text
        }

        var out = ""

        // 2. Format Logic
        // Loop through characters and insert formatting symbols
        for (i in trimmed.indices) {
            // [CUSTOMIZE HERE] - Prefix / Country Code
            if (i == 0) out += "+1 ("

            out += trimmed[i]

            // [CUSTOMIZE HERE] - Closing Parenthesis after 3 digits
            if (i == 2) out += ") "

            // [CUSTOMIZE HERE] - Hyphen after the next 3 digits
            if (i == 5) out += "-"
        }

        // 3. Cursor Mapping Logic
        // Maps the cursor position from the raw string to the formatted string.
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                // We added "+1 (" (4 chars) at the start
                if (offset <= 3) return offset + 4
                // We added ") " (2 chars) after index 3
                if (offset <= 6) return offset + 6
                // We added "-" (1 char) after index 6
                if (offset <= 10) return offset + 7
                return 17 // Total length of the formatted string
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 4) return 0
                if (offset <= 7) return offset - 4
                if (offset <= 12) return offset - 6
                if (offset <= 17) return offset - 7
                return 10 // Length of raw string
            }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

/**
 * Extension function to format a raw string into the UI format for display purposes (Text views).
 * Usage: "5550199999".formatToUsPhone() -> "+1 (555) 019-9999"
 */
fun String.formatToUsPhone(): String {
    // 1. Clean the input: Keep only digits, take max 10
    val trimmed = this.filter { it.isDigit() }.take(10)

    // 2. Apply the same logic as the VisualTransformation
    var out = ""
    for (i in trimmed.indices) {
        if (i == 0) out += "+1 ("
        out += trimmed[i]
        if (i == 2) out += ") "
        if (i == 5) out += "-"
    }
    return out
}