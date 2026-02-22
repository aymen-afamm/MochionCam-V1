package com.example.motioncam

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motioncam.ui.theme.AccentTeal
import com.example.motioncam.ui.theme.MotionCamTheme
import com.example.motioncam.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onFinish,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> YourRoadEyePage(
                        onNext = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        onSkip = onFinish,
                        currentPage = pagerState.currentPage
                    )
                    1 -> SmartInsightsPage(
                        onNext = {
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        onSkip = onFinish,
                        currentPage = pagerState.currentPage
                    )
                    2 -> SafeConnectedPage(
                        onGetStarted = onFinish,
                        currentPage = pagerState.currentPage
                    )
                }
            }
        }
    }
}

@Composable
fun YourRoadEyePage(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    currentPage: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        PhoneMockup(
            modifier = Modifier.weight(weight = 1f, fill = false)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(weight = 1f, fill = false)
        ) {
            Text(
                text = "Your Road Eye",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Record drives automatically with AI-powered incident detection.",
                fontSize = 17.sp,
                color = Color(0xFFAEAEB2),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            PaginationDots(currentPage = currentPage, totalPages = 3)
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
            
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "SKIP INTRO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF636366),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SmartInsightsPage(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    currentPage: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(weight = 1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT FOOTAGE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.5.sp
                )
                
                Text(
                    text = "Analysis Active",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93)
                )
            }
            
            SmartInsightsVideoGrid()
            
            AITripSummaryCard()
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Smart Insights",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Automatically tag and organize your trips with GPS and speed data.",
                fontSize = 17.sp,
                color = Color(0xFFAEAEB2),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            PaginationDots(currentPage = currentPage, totalPages = 3)
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Next",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
            
            TextButton(
                onClick = onSkip,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "SKIP INTRO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF636366),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun SafeConnectedPage(
    onGetStarted: () -> Unit,
    currentPage: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        SafeConnectedIllustration()
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(weight = 1f, fill = false)
        ) {
            Text(
                text = "Safe & Connected",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Lock critical footage and share trips with one tap.",
                fontSize = 17.sp,
                color = Color(0xFFAEAEB2),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            PaginationDots(currentPage = currentPage, totalPages = 3)
            
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Text(
                        text = "🚀",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VideoThumbnailGrid() {
    val thumbnails = listOf(
        "04:22" to listOf(Color(0xFFD4A574), Color(0xFFC9A77D)),
        "12:15" to listOf(Color(0xFFE5C8A8), Color(0xFFD4B89A)),
        "08:45" to listOf(Color(0xFFD4A574), Color(0xFFC9A77D)),
        "02:30" to listOf(Color(0xFF9FB8B8), Color(0xFFB0C5C5))
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            thumbnails.take(2).forEach { (duration, colors) ->
                VideoThumbnail(
                    duration = duration,
                    colors = colors,
                    modifier = Modifier.weight(weight = 1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            thumbnails.drop(2).forEach { (duration, colors) ->
                VideoThumbnail(
                    duration = duration,
                    colors = colors,
                    modifier = Modifier.weight(weight = 1f)
                )
            }
        }
    }
}

@Composable
fun VideoThumbnail(
    duration: String,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(colors = colors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AccentTeal.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Text(
            text = duration,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LocalEncryptionCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AccentTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = AccentTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = "Local Encryption",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Text(
                    text = "STATUS: PROTECTED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Checkmark",
            tint = Color(0xFF48484A),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SmartInsightsVideoGrid() {
    val thumbnails = listOf(
        Triple("04:22", Icons.Default.Info, "Camera"),
        Triple("12:15", Icons.Default.LocationOn, "Location"),
        Triple("08:45", Icons.Default.Settings, "Upload"),
        Triple("02:30", Icons.Default.Edit, "Edit")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            thumbnails.take(2).forEach { (duration, icon, contentDesc) ->
                SmartInsightVideoCard(
                    duration = duration,
                    icon = icon,
                    contentDescription = contentDesc,
                    modifier = Modifier.weight(weight = 1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            thumbnails.drop(2).forEach { (duration, icon, contentDesc) ->
                SmartInsightVideoCard(
                    duration = duration,
                    icon = icon,
                    contentDescription = contentDesc,
                    modifier = Modifier.weight(weight = 1f)
                )
            }
        }
    }
}

@Composable
fun SmartInsightVideoCard(
    duration: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = Color(0xFF2C2C2E),
                shape = RoundedCornerShape(20.dp)
            )
            .background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AccentTeal,
            modifier = Modifier.size(48.dp)
        )
        
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(AccentTeal.copy(alpha = 0.3f), CircleShape)
                .border(
                    width = 1.5.dp,
                    color = AccentTeal,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = AccentTeal,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Text(
            text = duration,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        )
    }
}

@Composable
fun AITripSummaryCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(AccentTeal.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Chart",
                    tint = AccentTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = "AI Trip Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Text(
                    text = "PROCESSING METADATA...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "AI",
            tint = AccentTeal,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SafeConnectedIllustration() {
    Box(
        modifier = Modifier
            .size(280.dp)
            .drawBehind {
                val radius = size.minDimension / 2f
                drawCircle(
                    color = Color(0xFF4A1C1C),
                    radius = radius * 0.9f,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
                drawCircle(
                    color = Color(0xFF4A1C1C),
                    radius = radius * 0.6f,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                )
                
                drawLine(
                    color = Color(0xFF4A1C1C),
                    start = Offset(size.width * 0.2f, size.height * 0.2f),
                    end = Offset(size.width * 0.8f, size.height * 0.8f),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF4A1C1C),
                    start = Offset(size.width * 0.8f, size.height * 0.2f),
                    end = Offset(size.width * 0.2f, size.height * 0.8f),
                    strokeWidth = 1.5f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = (-30).dp, y = (-20).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.3f),
                            Primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-30).dp, y = (-20).dp)
                .background(Primary, RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Shield",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .size(90.dp)
                .offset(x = 50.dp, y = 30.dp)
                .background(Primary, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun PhoneMockup(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 19.5f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(9f / 19.5f)
                .clip(RoundedCornerShape(48.dp))
                .background(Color(0xFF1C2A3A))
                .border(
                    width = 8.dp,
                    color = Color(0xFF2C2C2E),
                    shape = RoundedCornerShape(48.dp)
                )
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(44.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A3A52),
                                Color(0xFF0D1F2D)
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    var isRecording by remember { mutableStateOf(true) }
                    
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            isRecording = !isRecording
                        }
                    }
                    
                    val alpha by animateFloatAsState(
                        targetValue = if (isRecording) 1f else 0.3f,
                        animationSpec = tween(500), label = ""
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Primary.copy(alpha = alpha), CircleShape)
                    )
                    
                    Text(
                        text = "RECORDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xFF3A3A3C), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .fillMaxHeight()
                            .background(Primary, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun PaginationDots(
    currentPage: Int,
    totalPages: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index == currentPage) Primary else Color(0xFF48484A),
                        shape = CircleShape
                    )
            )
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    MotionCamTheme(darkTheme = true) {
        OnboardingScreen()
    }
}
