package com.example.motioncam

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motioncam.ui.theme.*

/**
 * Video Library / Gallery Screen - Displays recorded dashcam videos
 *
 * Features:
 * - Real video files from storage
 * - Filter tabs (All, Today, Favorites/Locked, Incidents)
 * - Video cards with metadata
 * - Quick actions (lock, share, delete)
 * - Storage usage indicator
 * - FAB for quick recording
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    viewModel: RecordingViewModel,
    onClipClick: (DashCamVideo) -> Unit = {},
    onStartRecording: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToConfig: () -> Unit = {}
) {
    var currentRoute by remember { mutableStateOf("gallery") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(VideoFilter.ALL) }

    // Collect video list from ViewModel
    val videos by viewModel.videoList.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()

    // Calculate storage percentage
    val storagePercentage = remember(storageInfo) {
        (storageInfo.usedPercentage).coerceIn(0, 100)
    }

    // Filter videos based on search and filter tab
    val filteredVideos = remember(videos, searchQuery, selectedFilter) {
        videos.filter { video ->
            val matchesSearch = searchQuery.isEmpty() ||
                    video.fileName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                VideoFilter.ALL -> true
                VideoFilter.TODAY -> isToday(video.timestamp)
                VideoFilter.LOCKED -> video.isLocked
                VideoFilter.RECENT -> isRecent(video.timestamp)
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Title and Storage indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Video Library",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${videos.size} videos • ${formatStorageSize(storageInfo.usedBytes)}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Storage indicator
                    StorageIndicatorCompact(
                        percentage = storagePercentage,
                        onClick = { /* Show storage details */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Storage progress bar
                LinearProgressIndicator(
                    progress = { storagePercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (storagePercentage > 80) Color.Red else Primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        },
        bottomBar = {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartRecording,
                modifier = Modifier.size(64.dp),
                containerColor = Color.Red,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Record",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
        },
        floatingActionButtonPosition = FabPosition.EndOverlay
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                videoCounts = getVideoCounts(videos)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Video List
            if (filteredVideos.isEmpty()) {
                EmptyVideoListMessage()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = filteredVideos,
                        key = { it.id }
                    ) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onClipClick(video) },
                            onLockClick = { viewModel.lockVideo(video) },
                            onDeleteClick = { viewModel.deleteVideo(video) },
                            onMoreClick = { /* Show options */ }
                        )
                    }

                    // Bottom padding for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageIndicatorCompact(
    percentage: Int,
    onClick: () -> Unit
) {
    val color = when {
        percentage > 80 -> Color.Red
        percentage > 60 -> Color.Yellow
        else -> AccentTeal
    }

    Box(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = "${100 - percentage}% Free",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )

            if (query.isEmpty()) {
                Text(
                    text = "Search videos...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            // Basic text field for search
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        // Placeholder already shown above
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: VideoFilter,
    onFilterSelected: (VideoFilter) -> Unit,
    videoCounts: Map<VideoFilter, Int>
) {
    val filters = VideoFilter.entries.toList()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            val count = videoCounts[filter] ?: 0

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) Primary else Color.Transparent,
                animationSpec = tween(200),
                label = "tab_bg"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                animationSpec = tween(200),
                label = "tab_text"
            )

            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Primary else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color.White.copy(alpha = 0.2f)
                                    else Primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(
    video: DashCamVideo,
    onClick: () -> Unit,
    onLockClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
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
                .height(180.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = if (video.isLocked) {
                            listOf(Color(0xFF4A6741), Color(0xFF2D3A25)) // Green tint for locked
                        } else {
                            listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))
                        }
                    )
                )
        ) {
            // Locked badge
            if (video.isLocked) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(
                            color = AccentTeal.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "LOCKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Play button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Primary.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // Duration badge
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text(
                    text = video.formattedDuration.ifEmpty { "--:--" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Info section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.03f))
                .padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.fileName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = "${video.formattedDate} • ${video.formattedSize}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lock button
                    IconButton(
                        onClick = onLockClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (video.isLocked) Icons.Default.Lock
                            else Icons.Default.LockOpen,
                            contentDescription = if (video.isLocked) "Unlock" else "Lock",
                            modifier = Modifier.size(20.dp),
                            tint = if (video.isLocked) AccentTeal
                            else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // More options
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVideoListMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No videos yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start recording to capture your drives",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * Video filter options
 */
enum class VideoFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    LOCKED("Locked"),
    RECENT("Recent")
}

/**
 * Helper function to get video counts for each filter
 */
private fun getVideoCounts(videos: List<DashCamVideo>): Map<VideoFilter, Int> {
    return mapOf(
        VideoFilter.ALL to videos.size,
        VideoFilter.TODAY to videos.count { isToday(it.timestamp) },
        VideoFilter.LOCKED to videos.count { it.isLocked },
        VideoFilter.RECENT to videos.count { isRecent(it.timestamp) }
    )
}

/**
 * Check if timestamp is today
 */
private fun isToday(timestamp: Long): Boolean {
    val now = java.util.Calendar.getInstance()
    val then = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
}

/**
 * Check if timestamp is within last 7 days
 */
private fun isRecent(timestamp: Long): Boolean {
    val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
    return timestamp > weekAgo
}

/**
 * Format storage size
 */
private fun formatStorageSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f KB", bytes / 1024.0)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun VideoLibraryScreenPreview() {
    MotionCamTheme(darkTheme = true) {
        // Preview would need a mock viewmodel
    }
}
