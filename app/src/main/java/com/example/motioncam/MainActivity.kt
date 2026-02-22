package com.example.motioncam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.motioncam.ui.theme.MotionCamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MotionCamTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("splash") }
    
    when (currentScreen) {
        "splash" -> {
            SplashScreen(
                onTimeout = {
                    currentScreen = "onboarding"
                }
            )
        }
        "onboarding" -> {
            OnboardingScreen(
                onFinish = {
                    currentScreen = "login"
                }
            )
        }
        "login" -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = "camera"
                },
                onSignUpClick = {
                    
                },
                onForgotPasswordClick = {
                    
                }
            )
        }
        "camera" -> {
            
        }
    }
}