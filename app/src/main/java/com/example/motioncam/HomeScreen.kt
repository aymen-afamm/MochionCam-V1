package com.example.motioncam

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.motioncam.ui.theme.AccentTeal
import com.example.motioncam.ui.theme.BackgroundDark
import com.example.motioncam.ui.theme.MotionCamTheme
import com.example.motioncam.ui.theme.Primary
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Data class representing trip statistics with movement detection
 */
data class TripStats(
    val durationMinutes: Int = 0,
    val distanceMiles: Double = 0.0,
    val maxSpeedMph: Int = 0,
    val isMoving: Boolean = false,
    val lastTripDate: String = ""
) {
    val hasData: Boolean
        get() = durationMinutes > 0 || distanceMiles > 0.0 || maxSpeedMph > 0
}

/**
 * Data class representing a video clip
 */
data class VideoClip(
    val id: String,
    val title: String,
    val timestamp: String,
    val duration: String,
    val thumbnailUrl: String? = null
)

/**
 * Navigation item data
 */
data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * Wrapper composable to break Column/Row scope for AnimatedVisibility
 * This ensures AnimatedVisibility from animation package is used, not ColumnScope.AnimatedVisibility
 */
@Composable
private fun AnimatedVisibilityWrapper(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: androidx.compose.animation.EnterTransition = fadeIn(),
    exit: androidx.compose.animation.ExitTransition = fadeOut(),
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        content = { content() }
    )
}

/**
 * HomeScreen - Main dashboard screen with trip stats, clips, and navigation
 *
 * Features:
 * - Dynamic trip summary (visible only when car has moved)
 * - Dynamic recent clips section (visible only when videos exist)
 * - Modern Material 3 bottom navigation with smooth animations
 * - Properly anchored bottom bar that respects system insets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String = "Alex",
    profileImageUrl: String? = null,
    currentLocation: LatLng = LatLng(34.0522, -118.2437),
    storageFree: Float = 0.85f,
    tripStats: TripStats = TripStats(),
    recentClips: List<VideoClip> = emptyList(),
    isCarMoving: Boolean = false,
    onStartRecording: () -> Unit = {},
    onViewAllClips: () -> Unit = {},
    onClipClick: (VideoClip) -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {}
) {
    // Recording state
    var isRecording by remember { mutableStateOf(false) }
    
    // Scroll state
    val scrollState = rememberScrollState()
    
    // Current navigation route
    var currentRoute by remember { mutableStateOf("home") }
    
    // Demo: Simulate trip data with movement updates
    var demoTripStats by remember { mutableStateOf(tripStats) }
    var demoClips by remember { mutableStateOf(recentClips) }
    
    LaunchedEffect(Unit) {
        // Simulate car movement - in real app, this comes from ViewModel/Repository
        // Delay to show initial empty state, then populate data
        delay(500)
        demoTripStats = TripStats(
            durationMinutes = 42,
            distanceMiles = 12.4,
            maxSpeedMph = 65,
            isMoving = isCarMoving,
            lastTripDate = "Aug 24, 14:20"
        )
        
        // Simulate clips being added after recording
        demoClips = listOf(
            VideoClip(
                id = "1",
                title = "Morning Commute",
                timestamp = "Today, 08:15 AM",
                duration = "05:22"
            ),
            VideoClip(
                id = "2",
                title = "Night Drive Loop",
                timestamp = "Aug 23, 11:40 PM",
                duration = "12:10"
            )
        )
    }

    // Using Scaffold for proper layout with bottom bar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            // Storage indicator in top bar area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 24.dp, top = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                StorageIndicator(storageFree = storageFree)
            }
        },
        bottomBar = {
            // Modern Material 3 Bottom Navigation Bar - ALWAYS VISIBLE
            // Properly handles navigation bar insets
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Material3BottomNavigationContent(
                    currentRoute = currentRoute,
                    onNavigateToHome = { currentRoute = "home" },
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
    ) { paddingValues ->
        // Main scrollable content - respects Scaffold padding (includes bottom bar height)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Header Section
            HeaderSection(
                userName = userName,
                profileImageUrl = profileImageUrl,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            // Last Trip Stats Card - ONLY VISIBLE WHEN HAS DATA (car moved)
            AnimatedVisibilityWrapper(
                visible = demoTripStats.hasData,
                enter = fadeIn(animationSpec = tween(300)) + 
                        expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) + 
                       shrinkVertically(animationSpec = tween(200))
            ) {
                TripSummaryCard(
                    stats = demoTripStats,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Add spacer only if trip card is visible
            AnimatedVisibilityWrapper(
                visible = demoTripStats.hasData,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Recording Button
            RecordingButton(
                isRecording = isRecording,
                onClick = {
                    isRecording = !isRecording
                    onStartRecording()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Map Section
            MapPreviewSection(
                currentLocation = currentLocation,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Clips Section - ONLY VISIBLE WHEN HAS CLIPS
            AnimatedVisibilityWrapper(
                visible = demoClips.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(400)) + 
                        expandVertically(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(300)) + 
                       shrinkVertically(animationSpec = tween(300))
            ) {
                RecentClipsSection(
                    clips = demoClips,
                    onViewAll = onViewAllClips,
                    onClipClick = onClipClick,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Bottom padding for better scroll experience
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderSection(
    userName: String,
    profileImageUrl: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Ready to Drive?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Safe travels today, $userName.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Profile Image
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = 2.dp,
                    color = Primary.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .padding(4.dp)
        ) {
            // Placeholder profile - in real app, load from URL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFF8B4513),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TripSummaryCard(
    stats: TripStats,
    modifier: Modifier = Modifier
) {
    val cardAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "card_alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha }
            .background(
                color = Primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            // Header row with moving indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "LAST TRIP SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        letterSpacing = 2.sp
                    )
                    
                    // Moving indicator - shows when car is currently moving
                    AnimatedVisibilityWrapper(
                        visible = stats.isMoving,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        MovingIndicator()
                    }
                }
                
                Text(
                    text = stats.lastTripDate,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row - DYNAMIC VALUES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Duration
                StatItem(
                    value = "${stats.durationMinutes}",
                    unit = "m",
                    label = "DURATION"
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Primary.copy(alpha = 0.15f))
                )

                // Distance
                StatItem(
                    value = String.format("%.1f", stats.distanceMiles),
                    unit = "mi",
                    label = "DISTANCE"
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Primary.copy(alpha = 0.15f))
                )

                // Max Speed
                StatItem(
                    value = "${stats.maxSpeedMph}",
                    unit = "mph",
                    label = "MAX SPEED"
                )
            }
        }
    }
}

@Composable
private fun MovingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "moving_indicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    // Pulsing dot with "LIVE" text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = AccentTeal.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
        Text(
            text = "LIVE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = AccentTeal.copy(alpha = alpha),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    unit: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun RecordingButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isRecording) 0.6f else 0.3f,
        animationSpec = tween(300),
        label = "glow_alpha"
    )
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect behind button
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    alpha = glowAlpha
                }
                .background(
                    color = if (isRecording) Color.Red else Primary,
                    shape = CircleShape
                )
                .blur(60)
        )

        // Main button
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color.Red else Primary,
                disabledContainerColor = Primary.copy(alpha = 0.5f)
            ),
            shape = CircleShape,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 12.dp,
                pressedElevation = 8.dp,
                disabledElevation = 0.dp
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isRecording) "STOP" else "START",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "RECORDING",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun MapPreviewSection(
    currentLocation: LatLng,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onResume()
        mapView.getMapAsync { googleMap ->
            // Dark map style
            try {
                val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Add marker at current location
            googleMap.addMarker(
                MarkerOptions()
                    .position(currentLocation)
                    .title("Current Location")
            )
            
            // Move camera to location
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(currentLocation, 15f)
            )
            
            // Disable UI controls for preview mode
            googleMap.uiSettings.isZoomControlsEnabled = false
            googleMap.uiSettings.isMyLocationButtonEnabled = false
        }
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // Map
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            BackgroundDark.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Location info overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${String.format("%.4f", currentLocation.latitude)}° N, ${String.format("%.4f", currentLocation.longitude)}° W",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun RecentClipsSection(
    clips: List<VideoClip>,
    onViewAll: () -> Unit,
    onClipClick: (VideoClip) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Recent Clips",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                // Clip count badge
                if (clips.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${clips.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }
            }
            
            TextButton(
                onClick = onViewAll,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Primary
                )
            ) {
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clips Grid - shows up to 2 clips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            clips.take(2).forEach { clip ->
                ClipCard(
                    clip = clip,
                    onClick = { onClipClick(clip) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ClipCard(
    clip: VideoClip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "clip_scale"
    )
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Thumbnail area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .background(Color.Black)
        ) {
            // Placeholder gradient (in real app, load actual thumbnail)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A),
                                Color(0xFF1A1A1A)
                            )
                        )
                    )
            )

            // Play icon
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play",
                modifier = Modifier
                    .size(44.dp)
                .align(Alignment.Center),
                tint = Color.White.copy(alpha = 0.9f)
            )

            // Duration badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = clip.duration,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f))
                .padding(12.dp)
        ) {
            Text(
                text = clip.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = clip.timestamp,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StorageIndicator(
    storageFree: Float
) {
    val percentage = (storageFree * 100).toInt()
    
    Row(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$percentage% Free",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(2.dp)
                )
        ) {
            val usedPercentage = 1f - storageFree
            Box(
                modifier = Modifier
                    .fillMaxWidth(usedPercentage)
                    .fillMaxHeight()
                    .background(
                        color = if (storageFree > 0.2f) Primary else Color.Red,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/**
 * Material 3 Bottom Navigation Content
 * This is the actual navigation content that sits inside the bottomBar slot
 */
@Composable
private fun Material3BottomNavigationContent(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    val navItems = listOf(
        NavItem("home", Icons.Default.Home, "Home"),
        NavItem("gallery", Icons.Default.VideoLibrary, "Gallery"),
        NavItem("stats", Icons.Default.BarChart, "Stats"),
        NavItem("config", Icons.Default.Settings, "Settings")
    )
    
    // Material 3 Navigation Bar container with glassmorphism
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1C1C1E).copy(alpha = 0.98f),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val onClick: () -> Unit = when (item.route) {
                    "home" -> onNavigateToHome
                    "gallery" -> onNavigateToGallery
                    "stats" -> onNavigateToStats
                    "config" -> onNavigateToConfig
                    else -> { {} }
                }
                
                Material3NavItem(
                    icon = item.icon,
                    label = item.label,
                    isSelected = isSelected,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun Material3NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animated colors
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color.White.copy(alpha = 0.5f),
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "icon_color"
    )
    
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color.White.copy(alpha = 0.5f),
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "label_color"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_scale"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Icon with animated background
            Box(
                modifier = Modifier
                    .size(if (isSelected) 44.dp else 40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Selected background indicator - use wrapper to break scope
                AnimatedVisibilityWrapper(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = Primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
                
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .size(if (isSelected) 26.dp else 24.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Label
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = labelColor,
                letterSpacing = 0.3.sp
            )
            
            // Animated selection indicator dot - use wrapper
            AnimatedVisibilityWrapper(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(150)) + 
                        expandVertically(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100)) + 
                       shrinkVertically(animationSpec = tween(100))
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .background(Primary, CircleShape)
                )
            }
            
            // Spacer when not selected to maintain consistent height
            if (!isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Extension function to simulate blur effect
 * Note: Use actual BlurModifier on Android 12+ for real blur
 */
private fun Modifier.blur(radius: Int): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()
    }
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    MotionCamTheme(darkTheme = true) {
        HomeScreen()
    }
}
