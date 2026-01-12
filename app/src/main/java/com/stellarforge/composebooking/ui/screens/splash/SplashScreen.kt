package com.stellarforge.composebooking.ui.screens.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.stellarforge.composebooking.R
import com.stellarforge.composebooking.ui.navigation.ScreenRoutes
import com.stellarforge.composebooking.ui.theme.PrimaryBlue

/**
 * The initial Splash Screen displayed upon app launch.
 *
 * **Purpose:**
 * - **Branding:** Displays the app logo with a smooth "Fade-In & Scale-Up" animation.
 * - **Seamless Transition:** Matches the system splash screen background to prevent flickering.
 * - **Routing State:** Waits for [SplashViewModel] to determine the user's role (Owner vs. Customer)
 *   and navigates to the appropriate screen.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Observe the navigation decision from ViewModel
    val startDestination by viewModel.startDestination.collectAsState()

    // Animation State: false -> Invisible/Small, true -> Visible/Normal
    var startAnimation by remember { mutableStateOf(false) }

    // Alpha Animation (Opacity): 0f -> 1f
    // Duration set to 1000ms to finish comfortably before the ViewModel's 2000ms delay.
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "AlphaAnim"
    )

    // Scale Animation (Zoom): 0.5f -> 1f
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ScaleAnim"
    )

    // Trigger animation on first composition
    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    // Handle Navigation Event
    LaunchedEffect(startDestination) {
        startDestination?.let { route ->
            navController.navigate(route) {
                // Remove Splash from the back stack so user cannot return to it
                popUpTo(ScreenRoutes.Splash.route) { inclusive = true }
            }
        }
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = PrimaryBlue) // Must match themes.xml windowBackground
            .systemBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            // Apply animations to the entire logo/text block
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // --- LOGO COMPOSITION ---
            // A white container with a shadow gives it a premium "App Icon" look
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- APP NAME ---
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp, // Adds a modern, airy feel to the text
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // --- VERSION INFO ---
        // Static element at the bottom (Not animated)
        Text(
            text = stringResource(id = R.string.version_label), // "Version 1.0.0"
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}