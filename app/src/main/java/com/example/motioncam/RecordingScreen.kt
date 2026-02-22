package com.example.motioncam

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motioncam.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Recording Screen - Professional Automotive DashCam Interface
 *
 * Features:
 * - Full-screen camera preview using CameraX
 * - Minimal clean overlay with recording info
 * - Live GPS speed display
 * - Blinking REC indicator
 * - Recording timer
 * - Bottom controls with glassmorphism design
 * - Auto-handles permissions
 * - Proper lifecycle management
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    onStopRecording: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect states from ViewModel
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val recordingDuration by viewModel.recordingDuration.collectAsStateWithLifecycle()
    val isGpsActive by viewModel.isGpsActive.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val isCameraReady by viewModel.isCameraReady.collectAsStateWithLifecycle()
    val recordingError by viewModel.recordingError.collectAsStateWithLifecycle()
    val isMoving by viewModel.isMoving.collectAsStateWithLifecycle()

    // Format recording duration
    val durationFormatted = remember(recordingDuration) {
        val hours = recordingDuration / 3600
        val minutes = (recordingDuration % 3600) / 60
        val seconds = recordingDuration % 60
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Camera preview view
    val previewView = remember { PreviewView(context) }

    // Set up camera preview view in ViewModel
    LaunchedEffect(previewView) {
        viewModel.previewView = previewView
    }

    // Handle lifecycle for camera initialization
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Initialize camera when screen is visible
                    viewModel.initializeCamera(lifecycleOwner)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Camera will be automatically managed by lifecycle
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Lock screen orientation to landscape for dashcam mode
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            // Restore original orientation
            originalOrientation?.let { activity?.requestedOrientation = it }
        }
    }

    // Required permissions for dashcam
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Handle permission denied
    if (!permissionsState.allPermissionsGranted) {
        PermissionDeniedScreen(
            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
            onNavigateBack = onNavigateToHome
        )
        return
    }

    // Show error if any
    recordingError?.let { error ->
        ErrorBanner(error = error, onDismiss = { /* Clear error */ })
    }

    var currentRoute by remember { mutableStateOf("recording") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        bottomBar = {
            // Only show bottom nav when not recording or minimal when recording
            if (!isRecording) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Material3BottomNavigationContent(
                        currentRoute = currentRoute,
                        onNavigateToHome = {
                            currentRoute = "home"
                            onNavigateToHome()
                        },
                        onNavigateToGallery = {
                            currentRoute = "gallery"
                            onNavigateToGallery()
                        },
                        onNavigateToStats = {
                            currentRoute = "stats"
                            onNavigateToStats()
                        },
                        onNavigateToConfig = {
                            currentRoute = "config"
                            onNavigateToConfig()
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera Preview (Full Screen)
            CameraPreview(
                previewView = previewView,
                isReady = isCameraReady,
                modifier = Modifier.fillMaxSize()
            )

            // Top Status Overlay
            TopStatusOverlay(
                isRecording = isRecording,
                recordingDuration = durationFormatted,
                currentSpeed = currentSpeed,
                isGpsActive = isGpsActive,
                isMoving = isMoving,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Bottom Recording Controls
            if (isRecording) {
                BottomRecordingControlsActive(
                    isLocked = isLocked,
                    onLockClip = { viewModel.toggleLock() },
                    onToggleCamera = { viewModel.toggleCamera(lifecycleOwner) },
                    onStopRecording = {
                        viewModel.stopRecording()
                        onStopRecording()
                    },
                    onOpenGallery = onNavigateToGallery,
                    onOpenSettings = onNavigateToConfig,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                // Standby controls
                BottomStandbyControls(
                    onStartRecording = { viewModel.startRecording() },
                    onNavigateHome = onNavigateToHome,
                    onNavigateGallery = onNavigateToGallery,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Camera not ready indicator
            if (!isCameraReady) {
                CameraInitializingIndicator()
            }
        }
    }
}

@Composable
private fun CameraPreview(
    previewView: PreviewView,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { previewView },
        modifier = modifier,
        update = { view ->
            // Ensure proper scaling
            view.scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    )

    // Gradient overlay for better visibility of UI
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.3f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.4f)
                    )
                )
            )
    )
}

@Composable
private fun TopStatusOverlay(
    isRecording: Boolean,
    recordingDuration: String,
    currentSpeed: Int,
    isGpsActive: Boolean,
    isMoving: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: REC indicator + Timer
            StatusBadge(
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRecording) {
                            // Blinking REC dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color.Red.copy(alpha = recAlpha),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = "REC",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Gray, CircleShape)
                            )
                            Text(
                                text = "STBY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(Color.White.copy(alpha = 0.3f))
                            )
                            Text(
                                text = recordingDuration,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            )

            // Center: Speed Display
            SpeedDisplay(speed = currentSpeed)

            // Right: GPS Status
            StatusBadge(
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isGpsActive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(AccentTeal, CircleShape)
                            )
                            Text(
                                text = "GPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = "GPS Off",
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                        }

                        if (isMoving) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .background(
                                        color = AccentTeal.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "MOVING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SpeedDisplay(speed: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$speed",
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            letterSpacing = (-2).sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text(
            text = "MPH",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun StatusBadge(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        content()
    }
}

@Composable
private fun BottomRecordingControlsActive(
    isLocked: Boolean,
    onLockClip: () -> Unit,
    onToggleCamera: () -> Unit,
    onStopRecording: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stopButtonScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "stop_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lock Button
        ControlButton(
            icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
            onClick = onLockClip,
            isActive = isLocked,
            activeColor = AccentTeal,
            size = 48.dp
        )

        // Camera Toggle
        ControlButton(
            icon = Icons.Default.Cameraswitch,
            onClick = onToggleCamera,
            isActive = false,
            activeColor = Primary,
            size = 48.dp
        )

        // Stop Recording Button (Large, Center)
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing glow effect
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulse_scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulse_alpha"
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(Color.Red.copy(alpha = 0.5f), CircleShape)
            )

            // Main stop button
            FilledIconButton(
                onClick = onStopRecording,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = stopButtonScale
                        scaleY = stopButtonScale
                    },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop Recording",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }

        // Gallery Button
        ControlButton(
            icon = Icons.Default.PhotoLibrary,
            onClick = onOpenGallery,
            isActive = false,
            activeColor = Primary,
            size = 48.dp
        )

        // Settings Button
        ControlButton(
            icon = Icons.Default.Settings,
            onClick = onOpenSettings,
            isActive = false,
            activeColor = Primary,
            size = 48.dp
        )
    }
}

@Composable
private fun BottomStandbyControls(
    onStartRecording: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home Button
        ControlButton(
            icon = Icons.Default.Home,
            onClick = onNavigateHome,
            isActive = false,
            activeColor = Primary,
            size = 48.dp
        )

        // Start Recording Button (Large, Center)
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "start_scale"
        )

        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Red.copy(alpha = 0.3f), CircleShape)
                    .blur(20)
            )

            FilledIconButton(
                onClick = onStartRecording,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Start Recording",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
        }

        // Gallery Button
        ControlButton(
            icon = Icons.Default.PhotoLibrary,
            onClick = onNavigateGallery,
            isActive = false,
            activeColor = Primary,
            size = 48.dp
        )
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isActive: Boolean,
    activeColor: Color,
    size: androidx.compose.ui.unit.Dp = 56.dp
) {
    val backgroundColor = if (isActive) activeColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.5f)
    val iconColor = if (isActive) activeColor else Color.White
    val borderColor = if (isActive) activeColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (isActive) 26.dp else 24.dp),
            tint = iconColor
        )
    }
}

@Composable
private fun CameraInitializingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Initializing Camera...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Red.copy(alpha = 0.9f))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color.White
                    )
                    Text(
                        text = error,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.clickable { onDismiss() },
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NoPhotography,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Red
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Camera Permission Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DashCam needs camera and location permissions to record video and track your speed.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Grant Permissions")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateBack) {
                Text("Go Back", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

// Extension for blur modifier
private fun Modifier.blur(radius: Int): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
    }
)
