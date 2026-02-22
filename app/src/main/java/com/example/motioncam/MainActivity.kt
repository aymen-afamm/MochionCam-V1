package com.example.motioncam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.maps.model.LatLng
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                    currentScreen = Screen.Home
                },
                onSignUpClick = {
                    currentScreen = Screen.Registration
                },
                onForgotPasswordClick = {
                    // TODO: Navigate to Forgot Password
                }
            )
        }
        
        Screen.Registration -> {
            // Handle system back button - go back to Login
            BackHandler {
                currentScreen = Screen.Login
            }

            RegistrationScreen(
                onBackClick = {
                    // Navigate back to Login
                    currentScreen = Screen.Login
                },
                onRegistrationSuccess = {
                    // Navigate back to Login after successful registration
                    // so user can sign in with their new credentials
                    currentScreen = Screen.Login
                },
                onLoginClick = {
                    // Navigate back to Login screen
                    currentScreen = Screen.Login
                }
            )
        }
        
        Screen.Home -> {
            // Handle system back button - exit app from home
            BackHandler {
                // Exit app or show exit confirmation
                (context as? ComponentActivity)?.finish()
            }
            
            HomeScreen(
                userName = "Alex",
                onStartRecording = {
                    currentScreen = Screen.Camera
                },
                onViewAllClips = {
                    // TODO: Navigate to full gallery
                },
                onClipClick = { clip ->
                    // TODO: Open clip player
                },
                onNavigateToGallery = {
                    // TODO: Navigate to gallery screen
                },
                onNavigateToStats = {
                    // TODO: Navigate to stats screen
                },
                onNavigateToConfig = {
                    // TODO: Navigate to settings/config screen
                }
            )
        }
        
        Screen.Camera -> {
            // Handle system back button - return to Home
            BackHandler {
                currentScreen = Screen.Home
            }
            
            CameraPlaceholderScreen(
                onBackToHome = {
                    currentScreen = Screen.Home
                }
            )
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
    object Registration : Screen()
    object Home : Screen()
    object Camera : Screen()
}

/**
 * Placeholder camera screen - implement with actual camera functionality
 */
@Composable
fun CameraPlaceholderScreen(
    onBackToHome: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Camera Screen - Coming Soon",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = onBackToHome,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back to Home")
            }
        }
    }
}
