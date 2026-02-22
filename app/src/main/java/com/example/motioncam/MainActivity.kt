package com.example.motioncam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motioncam.data.OnboardingPreferences
import com.example.motioncam.ui.theme.MotionCamTheme
import kotlinx.coroutines.flow.first

/**
 * MainActivity - Entry point for the DashCam application
 *
 * Navigation Structure:
 * - Splash -> Onboarding (if first launch) -> Login -> Home
 * - Home is the main dashboard with recording button
 * - Recording screen is full-screen dashcam interface
 * - Gallery shows recorded videos
 * - Settings for configuration
 */
class MainActivity : ComponentActivity() {

    // Global Recording ViewModel - shared across all screens
    private val recordingViewModel: RecordingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MotionCamTheme(darkTheme = true) {
                MotionCamApp(recordingViewModel = recordingViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up ViewModel resources
        recordingViewModel.cleanup()
    }
}

@Composable
fun MotionCamApp(
    recordingViewModel: RecordingViewModel
) {
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

    // Handle lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Refresh any necessary state when app comes to foreground
                    recordingViewModel.refreshVideos()
                }
                else -> {}
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
            BackHandler {
                currentScreen = Screen.Login
            }

            RegistrationScreen(
                onBackClick = {
                    currentScreen = Screen.Login
                },
                onRegistrationSuccess = {
                    currentScreen = Screen.Login
                },
                onLoginClick = {
                    currentScreen = Screen.Login
                }
            )
        }

        Screen.Home -> {
            BackHandler {
                (context as? ComponentActivity)?.finish()
            }

            HomeScreen(
                userName = "Alex",
                isRecording = recordingViewModel.isRecording.collectAsStateWithLifecycle().value,
                onStartRecording = {
                    recordingViewModel.startRecording()
                    currentScreen = Screen.Recording
                },
                onViewAllClips = {
                    currentScreen = Screen.Gallery
                },
                onClipClick = { clip ->
                    // TODO: Open video player
                },
                onNavigateToGallery = {
                    currentScreen = Screen.Gallery
                },
                onNavigateToStats = {
                    currentScreen = Screen.Stats
                },
                onNavigateToConfig = {
                    currentScreen = Screen.Settings
                }
            )
        }

        Screen.Recording -> {
            BackHandler {
                // Return to Home but keep recording running
                currentScreen = Screen.Home
            }

            RecordingScreen(
                viewModel = recordingViewModel,
                onStopRecording = {
                    currentScreen = Screen.Home
                },
                onNavigateToHome = {
                    currentScreen = Screen.Home
                },
                onNavigateToGallery = {
                    currentScreen = Screen.Gallery
                },
                onNavigateToStats = {
                    currentScreen = Screen.Stats
                },
                onNavigateToConfig = {
                    currentScreen = Screen.Settings
                }
            )
        }

        Screen.Gallery -> {
            BackHandler {
                currentScreen = Screen.Home
            }

            VideoLibraryScreen(
                viewModel = recordingViewModel,
                onStartRecording = {
                    recordingViewModel.startRecording()
                    currentScreen = Screen.Recording
                },
                onNavigateToHome = {
                    currentScreen = Screen.Home
                },
                onNavigateToGallery = {
                    // Already on gallery
                },
                onNavigateToStats = {
                    currentScreen = Screen.Stats
                },
                onNavigateToConfig = {
                    currentScreen = Screen.Settings
                }
            )
        }

        Screen.Stats -> {
            BackHandler {
                currentScreen = Screen.Home
            }

            StatsPlaceholderScreen(
                onBackToHome = { currentScreen = Screen.Home }
            )
        }

        Screen.Settings -> {
            BackHandler {
                currentScreen = Screen.Home
            }

            SettingsPlaceholderScreen(
                onBackToHome = { currentScreen = Screen.Home }
            )
        }

        Screen.Camera -> {
            // Legacy - redirect to Recording
            BackHandler {
                currentScreen = Screen.Home
            }

            CameraPlaceholderScreen(
                onBackToHome = { currentScreen = Screen.Home }
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
    object Recording : Screen()
    object Gallery : Screen()
    object Stats : Screen()
    object Settings : Screen()
    object Camera : Screen() // Legacy - can be removed
}

/**
 * Placeholder Stats Screen
 */
@Composable
fun StatsPlaceholderScreen(
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
                text = "Stats Screen - Coming Soon",
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

/**
 * Placeholder Settings Screen
 */
@Composable
fun SettingsPlaceholderScreen(
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
                text = "Settings Screen - Coming Soon",
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
