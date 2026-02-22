package com.example.motioncam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motioncam.ui.theme.AccentTeal
import com.example.motioncam.ui.theme.BackgroundDark
import com.example.motioncam.ui.theme.MotionCamTheme
import com.example.motioncam.ui.theme.Primary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onTimeout()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        SpeedLinesBackground()
        
        DecorativeGradients()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoSection()
            
            Spacer(modifier = Modifier.height(32.dp))
            
            BrandingSection()
        }
        
        BottomSection(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}

@Composable
fun SpeedLinesBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        SpeedLine(
            modifier = Modifier
                .width(256.dp)
                .offset(x = (-40).dp, y = 0.dp)
                .align(Alignment.TopStart)
                .padding(top = 120.dp)
                .rotate(15f),
            color = Primary
        )
        
        SpeedLine(
            modifier = Modifier
                .width(384.dp)
                .offset(x = (-80).dp, y = 0.dp)
                .align(Alignment.CenterEnd)
                .padding(top = 50.dp)
                .rotate(-10f),
            color = AccentTeal
        )
        
        SpeedLine(
            modifier = Modifier
                .width(320.dp)
                .offset(x = (-80).dp, y = 0.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 150.dp)
                .rotate(5f),
            color = Primary
        )
        
        SpeedLine(
            modifier = Modifier
                .width(192.dp)
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 40.dp)
                .rotate(-20f),
            color = AccentTeal
        )
        
        SpeedLine(
            modifier = Modifier
                .width(500.dp)
                .offset(x = (-20).dp, y = 0.dp)
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp)
                .rotate(-5f),
            color = Primary
        )
    }
}

@Composable
fun SpeedLine(modifier: Modifier = Modifier, color: Color) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
fun DecorativeGradients() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(256.dp)
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        BackgroundDark.copy(alpha = 0.8f),
                        BackgroundDark
                    ),
                    startY = Float.POSITIVE_INFINITY,
                    endY = 0f
                )
            )
    )
}

@Composable
fun LogoSection() {
    Box(
        modifier = Modifier.size(128.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .blur(40.dp)
                .background(Primary.copy(alpha = 0.2f), CircleShape)
        )
        
        Box(
            modifier = Modifier.size(128.dp),
            contentAlignment = Alignment.Center
        ) {
            MotionLines()
            
            CameraLens()
        }
    }
}

@Composable
fun MotionLines() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .offset(x = 16.dp, y = 0.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Primary.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .offset(x = (-16).dp, y = 0.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            AccentTeal.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .offset(x = 8.dp, y = 0.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Primary.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun CameraLens() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(BackgroundDark, CircleShape)
            .padding(2.dp)
            .background(Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .padding(1.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.Transparent, CircleShape)
                    .padding(4.dp)
                    .background(Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.1f)
                        ),
                        center = androidx.compose.ui.geometry.Offset(60f, 20f)
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun BrandingSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val brandName = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic
                )
            ) {
                append("MOTION")
            }
            withStyle(
                style = SpanStyle(
                    color = Primary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic
                )
            ) {
                append("CAM")
            }
        }
        
        Text(
            text = brandName,
            letterSpacing = (-2).sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.2f)
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "PREMIUM DASHCAM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 3.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun BottomSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(192.dp)
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Primary, AccentTeal)
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "CAPTURE EVERY MILE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 4.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Security",
                tint = Primary,
                modifier = Modifier.size(10.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "ENCRYPTED RECORDING ACTIVE",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 0.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    MotionCamTheme(darkTheme = true) {
        SplashScreen()
    }
}
