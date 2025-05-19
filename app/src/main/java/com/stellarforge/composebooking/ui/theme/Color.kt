package com.stellarforge.composebooking.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 Tema Renkleri (Android Studio varsayılanları)
// Açık Tema (Light Scheme) için ana renkler:
val Purple40 = Color(0xFF6650a4) // Genellikle Primary olarak kullanılır
val PurpleGrey40 = Color(0xFF625b71) // Genellikle Secondary veya Surface Variant olarak kullanılır
val Pink40 = Color(0xFF7D5260) // Genellikle Tertiary olarak kullanılır

// Koyu Tema (Dark Scheme) için ana renkler:
val Purple80 = Color(0xFFD0BCFF) // Genellikle Primary olarak kullanılır
val PurpleGrey80 = Color(0xFFCCC2DC) // Genellikle Secondary veya Surface Variant olarak kullanılır
val Pink80 = Color(0xFFEFB8C8) // Genellikle Tertiary olarak kullanılır

// --- EK ÖZELLEŞTİRİLEBİLİR RENKLER (ÖRNEK) ---
// Şablon kullanıcılarının kolayca değiştirebileceği ek renkler tanımlanabilir.
// Bu renkler Theme.kt içinde veya doğrudan Composable'larda kullanılabilir.
// Örnek:
// val SuccessColor = Color(0xFF4CAF50)
// val WarningColor = Color(0xFFFFC107)
// val ErrorColorMaterial = Color(0xFFF44336) // Zaten MaterialTheme.colorScheme.error var ama override edilebilir.

// val PositiveActionBackground = Color(0xFF4CAF50)
// val NegativeActionBackground = Color(0xFFD32F2F)
// val NeutralActionBackground = Color(0xFF757575)