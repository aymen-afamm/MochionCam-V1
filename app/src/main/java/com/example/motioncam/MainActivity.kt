package com.example.motioncam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.motioncam.data.OnboardingPreferences
import com.example.motioncam.ui.theme.MotionCamTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MotionCamTheme(darkTheme = true) {
                MotionCamApp()
            }
        }
    }
}

@Composable
fun MotionCamApp() {
    val context = LocalContext.current
    val onboardingPrefs = remember { OnboardingPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var showOnboarding by remember { mutableStateOf<Boolean?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    
    // Check onboarding status on first launch
    LaunchedEffect(Unit) {
        val isCompleted = onboardingPrefs.isOnboardingCompleted.first()
        showOnboarding = !isCompleted
    }
    
    // Handle lifecycle to refresh onboarding status when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh onboarding status when app comes to foreground
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Show splash while loading onboarding status
    if (showOnboarding == null) {
        SplashScreen(onTimeout = { })
        return
    }
    
    // Navigation based on current screen state
    when (currentScreen) {
        Screen.Splash -> {
            SplashScreen(
                onTimeout = {
                    currentScreen = if (showOnboarding == true) {
                        Screen.Onboarding
                    } else {
                        Screen.Login
                    }
                }
            )
        }
        
        Screen.Onboarding -> {
            OnboardingScreen(
                onFinish = {
                    currentScreen = Screen.Login
                }
            )
        }
        
        Screen.Login -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = Screen.Camera
                },
                onSignUpClick = {
                    // TODO: Navigate to Sign Up
                },
                onForgotPasswordClick = {
                    // TODO: Navigate to Forgot Password
                }
            )
        }
        
        Screen.Camera -> {
            // TODO: Camera screen placeholder
            CameraPlaceholderScreen()
        }
    }
}

/**
 * Sealed class representing app screens
 */
sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    object Login : Screen()
    object Camera : Screen()
}

/**
 * Placeholder camera screen - implement with actual camera functionality
 */
@Composable
fun CameraPlaceholderScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "Camera Screen - Coming Soon",
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}
