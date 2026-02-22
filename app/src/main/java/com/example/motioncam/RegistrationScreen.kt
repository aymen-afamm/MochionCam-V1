package com.example.motioncam

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motioncam.auth.AuthUiResult
import com.example.motioncam.auth.FirebaseAuthManager
import com.example.motioncam.ui.theme.AccentTeal
import com.example.motioncam.ui.theme.BackgroundDark
import com.example.motioncam.ui.theme.MotionCamTheme
import com.example.motioncam.ui.theme.Primary
import kotlinx.coroutines.launch

@Composable
fun RegistrationScreen(
    onBackClick: () -> Unit = {},
    onRegistrationSuccess: () -> Unit = {},
    onGoogleSignUpClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authManager = remember { FirebaseAuthManager.getInstance() }
    val scope = rememberCoroutineScope()

    // Form state
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Google Sign-In launcher (legacy fallback)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                isLoading = true
                val authResult = authManager.handleGoogleSignInResult(result.data)
                isLoading = false

                when (authResult) {
                    is AuthUiResult.Success -> {
                        showSuccessDialog = true
                    }
                    is AuthUiResult.Error -> {
                        errorMessage = authResult.message
                    }
                    else -> {}
                }
            }
        }
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success!") },
            text = { Text("Account created successfully! Please check your email to verify your account.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onRegistrationSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Continue to Login", color = Color.White)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter),
            snackbar = { data ->
                Surface(
                    color = Color(0xFFB00020),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = data.visuals.message,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                }
            }
        )

        // Background with subtle gradient
        BackgroundGradient()

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Sticky Header
            RegistrationHeader(
                currentStep = 1,
                totalSteps = 3,
                onBackClick = onBackClick
            )

            // Main content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Hero Header
                HeroSection()

                Spacer(modifier = Modifier.height(24.dp))

                // Registration Form
                RegistrationForm(
                    fullName = fullName,
                    onFullNameChange = { fullName = it },
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    isPasswordVisible = isPasswordVisible,
                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                    isConfirmPasswordVisible = isConfirmPasswordVisible,
                    onToggleConfirmPasswordVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    onCreateAccountClick = {
                        // Validate inputs
                        when {
                            fullName.isBlank() -> errorMessage = "Please enter your full name"
                            email.isBlank() -> errorMessage = "Please enter your email"
                            password.isBlank() -> errorMessage = "Please enter a password"
                            password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                            password != confirmPassword -> errorMessage = "Passwords do not match"
                            else -> {
                                scope.launch {
                                    isLoading = true
                                    val result = authManager.registerWithEmail(email, password, fullName)
                                    isLoading = false

                                    when (result) {
                                        is AuthUiResult.Success -> {
                                            showSuccessDialog = true
                                        }
                                        is AuthUiResult.Error -> {
                                            errorMessage = result.message
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    },
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Social Login Section
                SocialLoginSection(
                    onGoogleSignUpClick = {
                        // Try new Credential Manager first
                        scope.launch {
                            isLoading = true
                            val result = authManager.signInWithGoogle(context)
                            isLoading = false

                            when (result) {
                                is AuthUiResult.Success -> {
                                    showSuccessDialog = true
                                }
                                is AuthUiResult.Error -> {
                                    // Fallback to legacy Google Sign-In
                                    val signInIntent = authManager.getGoogleSignInClient(context).signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                }
                                else -> {}
                            }
                        }
                    },
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Security Badge
                SecurityBadge()

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Link
                FooterSection(
                    onLoginClick = onLoginClick,
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Safe Area Indicator
                BottomIndicator()

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun BackgroundGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDark,
                        BackgroundDark,
                        BackgroundDark.copy(alpha = 0.95f)
                    )
                )
            )
    )
}

@Composable
private fun RegistrationHeader(
    currentStep: Int,
    totalSteps: Int,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = BackgroundDark.copy(alpha = 0.8f)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Top row with back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Step indicator and title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Step ${String.format("%02d", currentStep)} of ${String.format("%02d", totalSteps)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Registration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Spacer for balance (same size as back button)
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            val progress = currentStep.toFloat() / totalSteps.toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(Primary)
                    .drawWithContent {
                        drawContent()
                        // Add glow effect
                        drawRect(
                            color = Primary.copy(alpha = 0.3f),
                            topLeft = Offset(0f, -2f),
                            size = androidx.compose.ui.geometry.Size(width = size.width, height = size.height + 4f)
                        )
                    }
            )
        }
    }
}

@Composable
private fun HeroSection() {
    Column {
        Text(
            text = "Join MotionCam",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set up your automotive-inspired dashboard to start recording your road trips with precision.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.5f),
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun RegistrationForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isConfirmPasswordVisible: Boolean,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onCreateAccountClick: () -> Unit,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Full Name Field
        LabeledTextField(
            label = "FULL NAME",
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = "John Doe",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            },
            keyboardType = KeyboardType.Text,
            enabled = !isLoading
        )

        // Email Field
        LabeledTextField(
            label = "EMAIL ADDRESS",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "john@motioncam.app",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            },
            keyboardType = KeyboardType.Email,
            enabled = !isLoading
        )

        // Password Field with Strength Meter
        PasswordFieldWithStrength(
            label = "PASSWORD",
            value = password,
            onValueChange = onPasswordChange,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            passwordStrength = calculatePasswordStrength(password),
            enabled = !isLoading
        )

        // Confirm Password Field
        LabeledTextField(
            label = "CONFIRM PASSWORD",
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            },
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isPasswordVisible = isConfirmPasswordVisible,
            onTogglePasswordVisibility = onToggleConfirmPasswordVisibility,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Create Account Button
        Button(
            onClick = onCreateAccountClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "CREATE ACCOUNT",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = 16.sp
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
                {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !isPasswordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Primary.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                cursorColor = Primary,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun PasswordFieldWithStrength(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    passwordStrength: PasswordStrength,
    enabled: Boolean = true
) {
    Column {
        // Password field
        LabeledTextField(
            label = label,
            value = value,
            onValueChange = onValueChange,
            placeholder = "",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            },
            keyboardType = KeyboardType.Password,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            enabled = enabled
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password Strength Meter
        PasswordStrengthMeter(strength = passwordStrength)
    }
}

@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        // Label and percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STRENGTH: ${strength.label.uppercase()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = strength.color,
                letterSpacing = 1.sp
            )
            Text(
                text = "${strength.percentage}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                val isFilled = index < strength.filledSegments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) strength.color else Color.White.copy(alpha = 0.1f)
                        )
                )
            }
        }
    }
}

@Composable
private fun SocialLoginSection(
    onGoogleSignUpClick: () -> Unit,
    isLoading: Boolean
) {
    Column {
        // Divider with text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
            Text(
                text = "or continue with",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sign Up Button
        OutlinedButton(
            onClick = onGoogleSignUpClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Logo (simplified as "G")
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                    }
                    Text(
                        text = "Sign up with Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityBadge() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Secure AES-256 Encryption",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FooterSection(
    onLoginClick: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        TextButton(
            onClick = onLoginClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            enabled = !isLoading
        ) {
            Text(
                text = "Log In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

@Composable
private fun BottomIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(128.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
        )
    }
}

// Password strength data class
data class PasswordStrength(
    val label: String,
    val color: Color,
    val percentage: Int,
    val filledSegments: Int
)

// Calculate password strength
private fun calculatePasswordStrength(password: String): PasswordStrength {
    return when {
        password.isEmpty() -> PasswordStrength(
            label = "Empty",
            color = Color.Gray,
            percentage = 0,
            filledSegments = 0
        )
        password.length < 6 -> PasswordStrength(
            label = "Weak",
            color = Color(0xFFFF6B6B),
            percentage = 25,
            filledSegments = 1
        )
        password.length < 10 -> PasswordStrength(
            label = "Fair",
            color = Color(0xFFFFA500),
            percentage = 50,
            filledSegments = 2
        )
        password.length < 12 || !password.any { it.isUpperCase() } || !password.any { it.isDigit() } -> PasswordStrength(
            label = "Good",
            color = Color(0xFFFFD700),
            percentage = 75,
            filledSegments = 3
        )
        else -> PasswordStrength(
            label = "Secure",
            color = AccentTeal,
            percentage = 100,
            filledSegments = 4
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistrationScreenPreview() {
    MotionCamTheme(darkTheme = true) {
        RegistrationScreen()
    }
}
