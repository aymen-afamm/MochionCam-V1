package com.example.motioncam

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motioncam.R
import com.example.motioncam.data.OnboardingPreferences
import com.example.motioncam.ui.theme.AccentTeal
import com.example.motioncam.ui.theme.BackgroundDark
import com.example.motioncam.ui.theme.MotionCamTheme
import com.example.motioncam.ui.theme.Primary
import kotlinx.coroutines.launch

/**
 * Data class representing an onboarding page
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val content: @Composable () -> Unit
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onboardingPrefs = remember { OnboardingPreferences(context) }
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Main content column
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar with Skip button only
            SkipButtonBar(
                onSkip = {
                    scope.launch {
                        onboardingPrefs.setOnboardingCompleted()
                        onFinish()
                    }
                }
            )

            // Horizontal Pager for swipeable content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> OnboardingPageContent(
                        title = "Your Road Eye",
                        description = "Record drives automatically with AI-powered incident detection.",
                        content = { FirstPageImage() }
                    )
                    1 -> OnboardingPageContent(
                        title = "Smart Insights",
                        description = "Automatically tag and organize your trips with GPS and speed data.",
                        content = { SecondPageContent() }
                    )
                    2 -> OnboardingPageContent(
                        title = "Safe & Connected",
                        description = "Lock critical footage and share trips with one tap. Your data stays secure.",
                        content = { ThirdPageContent() }
                    )
                }
            }
        }

        // Fixed bottom section - always visible
        FixedBottomSection(
            pagerState = pagerState,
            currentPage = currentPage,
            onNext = {
                if (currentPage < 2) {
                    scope.launch {
                        pagerState.animateScrollToPage(currentPage + 1)
                    }
                } else {
                    scope.launch {
                        onboardingPrefs.setOnboardingCompleted()
                        onFinish()
                    }
                }
            }
        )
    }
}

@Composable
private fun SkipButtonBar(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onSkip,
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
}

@Composable
private fun FirstPageImage() {
    // Large rectangle container for the image
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        // Center the drawable image
        Image(
            painter = painterResource(id = R.drawable.road_preview),
            contentDescription = "Road preview illustration",
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SecondPageContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Primary
            )
        }

        // Feature list
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FeatureItem("GPS Tracking")
            FeatureItem("Speed Analysis")
            FeatureItem("Trip Organization")
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = AccentTeal
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color(0xFFAEAEB2)
        )
    }
}

@Composable
private fun ThirdPageContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        // Two feature icons side by side
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lock feature
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        modifier = Modifier.size(36.dp),
                        tint = Primary
                    )
                }
                Text(
                    text = "Lock",
                    fontSize = 14.sp,
                    color = Color(0xFFAEAEB2)
                )
            }

            // Share feature
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AccentTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        modifier = Modifier.size(36.dp),
                        tint = AccentTeal
                    )
                }
                Text(
                    text = "Share",
                    fontSize = 14.sp,
                    color = Color(0xFFAEAEB2)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        // Content (image or icons) - centered
        content()

        // Title
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Description
        Text(
            text = description,
            fontSize = 17.sp,
            color = Color(0xFFAEAEB2),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun FixedBottomSection(
    pagerState: PagerState,
    currentPage: Int,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Page indicators
        PageIndicators(
            pageCount = 3,
            currentPage = currentPage
        )

        // Next / Get Started button - fixed at bottom
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPage < 2) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
                Text(
                    text = if (currentPage < 2) "Next" else "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (index == currentPage) {
                            Primary
                        } else {
                            Color(0xFF48484A)
                        }
                    )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    MotionCamTheme(darkTheme = true) {
        OnboardingScreen(onFinish = {})
    }
}
