package com.stellarforge.composebooking.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Telefon numarasını "0(XXX) XXX XX XX" formatında gösteren VisualTransformation.
 * Sadece rakam girişine izin verildiği varsayılır.
 */
class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Sadece rakamları al
        val digits = text.text.filter { it.isDigit() }
        var out = ""
        val firstGroupEnd = 1 // 0
        val secondGroupEnd = firstGroupEnd + 3 // 555
        val thirdGroupEnd = secondGroupEnd + 3 // 555
        val fourthGroupEnd = thirdGroupEnd + 2 // 55
        // Son grup için özel bir bitişe gerek yok, kalanlar olacak.

        if (digits.isNotEmpty()) {
            out += digits.substring(0, minOf(digits.length, firstGroupEnd)) // "0"
        }
        if (digits.length > firstGroupEnd) {
            out += "(" + digits.substring(firstGroupEnd, minOf(digits.length, secondGroupEnd))
        }
        if (digits.length > secondGroupEnd) {
            if (out.last() != '(') out += ") " // Parantezi kapat ve boşluk ekle
            else if (digits.length == secondGroupEnd +1 && out.length == 5) out += ") " // (XXX) durumu
            out += digits.substring(secondGroupEnd, minOf(digits.length, thirdGroupEnd))
        }
        if (digits.length > thirdGroupEnd) {
            if (out.last() != ' ') out += " "
            out += digits.substring(thirdGroupEnd, minOf(digits.length, fourthGroupEnd))
        }
        if (digits.length > fourthGroupEnd) {
            if (out.last() != ' ') out += " "
            out += digits.substring(fourthGroupEnd, digits.length)
        }
        // Parantezlerin kapanıp kapanmadığını kontrol et ve gerekirse ekle
        if (digits.length > firstGroupEnd && digits.length <= secondGroupEnd && !out.contains(")")) {
            out += ")"
        }


        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                if (offset <= firstGroupEnd) return offset
                if (offset <= secondGroupEnd) return offset + 1 // ( için
                if (offset == secondGroupEnd + 1 && out.getOrNull(offset+1) == ')') return offset +2 // ) için
                if (offset <= thirdGroupEnd) return offset + 3   // ()  için
                if (offset <= fourthGroupEnd) return offset + 4  // ()   için
                return offset + 5 // ()    için
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return offset
                if (offset <= firstGroupEnd + 1) return offset // 0(
                if (offset <= secondGroupEnd + 2) return offset - 1 // 0(XXX)
                if (offset <= thirdGroupEnd + 4) return offset - 3 // 0(XXX) XXX
                if (offset <= fourthGroupEnd + 5) return offset - 4 // 0(XXX) XXX XX
                return offset - 5 // 0(XXX) XXX XX XX
            }
        }

        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}