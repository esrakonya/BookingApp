package com.stellarforge.composebooking.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A robust [VisualTransformation] for formatting phone numbers.
 *
 * **Format:** `(XXX) XXX XX XX`
 *
 * **Logic:**
 * - It strictly limits the input to 10 digits (Standard mobile length without country code).
 * - It calculates the correct cursor position (OffsetMapping) bi-directionally to prevent
 *   crashes when the user deletes characters or moves the cursor.
 * - If the user types more than 10 digits, the excess is ignored in the visual output.
 */
class PhoneNumberVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // 1. Extract only digits from the input
        val trimmed = if (text.text.length >= 10) {
            text.text.substring(0..9)
        } else {
            text.text
        }

        var out = ""

        // 2. Build the formatted string based on the current length
        for (i in trimmed.indices) {
            out += when (i) {
                0 -> "("
                3 -> ") "
                6 -> " "
                8 -> " "
                else -> ""
            }
            out += trimmed[i]
        }

        // 3. Define the Offset Mapping (Crucial for Cursor Position)
        val phoneNumberOffsetTranslator = object : OffsetMapping {

            // Converts "Raw" index (Data) to "Visual" index (Screen)
            // Example: Index 3 in "5551..." -> Index 5 in "(555) 1..."
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 3) return offset + 1 // Add 1 for '('
                if (offset <= 6) return offset + 3 // Add 1 for '(' and 2 for ') '
                if (offset <= 8) return offset + 4 // Add 3 previous + 1 for ' '
                if (offset <= 10) return offset + 5 // Add 4 previous + 1 for ' '
                return 15 // Max length
            }

            // Converts "Visual" index (Screen) back to "Raw" index (Data)
            // Used when the user deletes a character or clicks somewhere.
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 4) return offset - 1
                if (offset <= 9) return offset - 3
                if (offset <= 12) return offset - 4
                if (offset <= 15) return offset - 5
                return 10 // Max length
            }
        }

        return TransformedText(AnnotatedString(out), phoneNumberOffsetTranslator)
    }
}