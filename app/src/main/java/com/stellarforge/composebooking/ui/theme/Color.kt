package com.stellarforge.composebooking.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Application Color Palette.
 *
 * **Design Philosophy:**
 * A professional, clean, and corporate palette based on "Navy Blue" tones.
 * Designed to instill trust and calm (ideal for Service/Booking apps).
 *
 * **Customization:**
 * To re-brand the app, simply change the [PrimaryBlue] and [SecondaryGrey] values here.
 * The Theme system will automatically propagate these changes throughout the app.
 */

// --- BRAND COLORS (Primary Identity) ---
val PrimaryBlue = Color(0xFF2C3E50)// Deep Navy: Used for Main Buttons, Headers, Active States.
val OnPrimaryBlue = Color(0xFFFFFFFF)     // Text color on Primary components (White).
val PrimaryContainerBlue = Color(0xFFD3DCE5) // Soft Blue-Grey: Used for selected items, light backgrounds.
val OnPrimaryContainerBlue = Color(0xFF001E2F)// High-contrast text for Primary Container.

// --- SECONDARY COLORS (Supporting Accents) ---
val SecondaryGrey = Color(0xFF535F70)      // Slate Grey: Used for Icons, Secondary Actions.
val OnSecondaryGrey = Color(0xFFFFFFFF)    // Text color on Secondary components.
val SecondaryContainerGrey = Color(0xFFD9E3F0)// Very Light Grey: Card backgrounds, Chips.
val OnSecondaryContainerGrey = Color(0xFF101C2B)// Dark text for Secondary Container.

// --- SEMANTIC COLORS (Feedback) ---
val ErrorRed = Color(0xFFB00020)           // Standard Material Error Red.
val OnErrorRed = Color.White

// --- NEUTRAL COLORS (Light Theme) ---
val BackgroundLight = Color(0xFFF7F9FC)    // Cool White/Light Grey: Main screen background (Better for eyes than #FFFFFF).
val OnBackgroundLight = Color(0xFF1A1C1E)  // Primary text color (Almost Black).
val SurfaceLight = Color(0xFFFFFFFF)       // Pure White: Cards, Sheets, Menus.
val OnSurfaceLight = Color(0xFF1A1C1E)     // Text on surfaces.
val SurfaceVariantLight = Color(0xFFDEE3EB) // Dividers, Borders.
val OnSurfaceVariantLight = Color(0xFF42474E) // Secondary text, Hints.
val OutlineLight = Color(0xFF72777F)       // Input field borders.

// --- NEUTRAL COLORS (Dark Theme) ---
val BackgroundDark = Color(0xFF1A1C1E)     // Deep Grey/Black: OLED friendly background.
val OnBackgroundDark = Color(0xFFE2E2E6)   // Primary text color (Off-White).
val SurfaceDark = Color(0xFF1A1C1E)        // Dark Surface.
val OnSurfaceDark = Color(0xFFE2E2E6)      // Text on dark surfaces.
val SurfaceVariantDark = Color(0xFF42474E) // Darker Dividers.
val OnSurfaceVariantDark = Color(0xFFC2C7CE) // Secondary text on dark.
val OutlineDark = Color(0xFF8C9199)        // Input borders on dark.