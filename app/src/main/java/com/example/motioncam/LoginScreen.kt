package com.example.motioncam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motioncam.ui.theme.AccentTeal
import com.example.motioncam.ui.theme.BackgroundDark
import com.example.motioncam.ui.theme.MotionCamTheme
import com.example.motioncam.ui.theme.Primary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        BackgroundEffects()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 80.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            HeaderSection()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            LoginFormSection(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                onForgotPasswordClick = onForgotPasswordClick,
                onLoginClick = onLoginSuccess,
                onBiometricClick = { }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            FooterSection(
                onSignUpClick = onSignUpClick
            )
        }
        
        DecorativeBottomBar()
    }
}

@Composable
fun BackgroundEffects() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            x = 0.5f,
                            y = 1.2f
                        )
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = (-80).dp)
                .size(256.dp)
                .background(Primary, CircleShape)
                .blur(100.dp)
                .align(Alignment.TopEnd)
        )
        
        Box(
            modifier = Modifier
                .offset(x = (-128).dp, y = 0.dp)
                .size(320.dp)
                .background(AccentTeal.copy(alpha = 0.2f), CircleShape)
                .blur(120.dp)
                .align(Alignment.CenterStart)
        )
    }
}

@Composable
fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "MotionCam Logo",
                tint = Primary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "MotionCam",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SECURE AUTOMOTIVE LOGGING",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Primary.copy(alpha = 0.6f),
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun LoginFormSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        FloatingLabelTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "EMAIL ADDRESS",
            placeholder = "driver@motioncam.app",
            keyboardType = KeyboardType.Email
        )
        
        FloatingLabelPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "PASSWORD",
            placeholder = "••••••••••••",
            passwordVisible = passwordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onForgotPasswordClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "FORGOT PASSWORD?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary
            ),
            shape = CircleShape,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 2.dp
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Text(
                    text = "LOGIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(weight = 1f),
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            Text(
                text = "OR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8E8E93)
            )
            HorizontalDivider(
                modifier = Modifier.weight(weight = 1f),
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )
        }
        
        OutlinedButton(
            onClick = onBiometricClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = AccentTeal
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = AccentTeal.copy(alpha = 0.3f)
            ),
            shape = CircleShape
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = AccentTeal
                )
                Text(
                    text = "BIOMETRIC LOGIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentTeal
                )
            }
        }
    }
}

@Composable
fun FloatingLabelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedLabelColor = Primary,
                unfocusedLabelColor = Color(0xFF8E8E93),
                cursorColor = Primary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            label = {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.2f)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun FloatingLabelPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    passwordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedLabelColor = Primary,
                unfocusedLabelColor = Color(0xFF8E8E93),
                cursorColor = Primary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            label = {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.2f)
                )
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = Color(0xFF8E8E93)
                    )
                }
            }
        )
    }
}

@Composable
fun FooterSection(
    onSignUpClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93)
            )
            TextButton(
                onClick = onSignUpClick,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(
                    text = "Sign up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "SECURE BY ROOM DB",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 0.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(4.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = CircleShape
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.33f)
                        .fillMaxHeight()
                        .background(Primary, CircleShape)
                )
            }
        }
    }
}

@Composable
fun BoxScope.DecorativeBottomBar() {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
            .width(128.dp)
            .height(4.dp)
            .background(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    MotionCamTheme {
        LoginScreen()
    }
}
