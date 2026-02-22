package com.example.motioncam.auth

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.motioncam.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Sealed class representing authentication results
 */
sealed class AuthUiResult<out T> {
    data class Success<T>(val data: T) : AuthUiResult<T>()
    data class Error(val exception: Throwable, val message: String) : AuthUiResult<Nothing>()
    object Loading : AuthUiResult<Nothing>()
}

/**
 * Firebase Authentication Manager
 * Handles all authentication operations: Email/Password, Google Sign-In
 */
class FirebaseAuthManager private constructor() {

    companion object {
        @Volatile
        private var instance: FirebaseAuthManager? = null

        fun getInstance(): FirebaseAuthManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthManager().also { instance = it }
            }
        }
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // StateFlow to observe authentication state
    private val _authState = MutableStateFlow<AuthUiResult<FirebaseUser?>>(AuthUiResult.Success(auth.currentUser))
    val authState: StateFlow<AuthUiResult<FirebaseUser?>> = _authState

    // StateFlow for loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Current user
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): AuthUiResult<FirebaseUser> {
        return try {
            _isLoading.value = true
            _authState.value = AuthUiResult.Loading

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                _authState.value = AuthUiResult.Success(user)
                AuthUiResult.Success(user)
            } else {
                val error = AuthUiResult.Error(
                    Exception("Authentication failed"),
                    "Unable to sign in. Please check your credentials."
                )
                _authState.value = error
                error
            }
        } catch (e: Exception) {
            val error = AuthUiResult.Error(e, getErrorMessage(e))
            _authState.value = error
            error
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Register with email and password
     */
    suspend fun registerWithEmail(email: String, password: String, displayName: String? = null): AuthUiResult<FirebaseUser> {
        return try {
            _isLoading.value = true
            _authState.value = AuthUiResult.Loading

            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Update display name if provided
                displayName?.let {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(it)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }

                // Send email verification
                user.sendEmailVerification().await()

                _authState.value = AuthUiResult.Success(user)
                AuthUiResult.Success(user)
            } else {
                val error = AuthUiResult.Error(
                    Exception("Registration failed"),
                    "Unable to create account. Please try again."
                )
                _authState.value = error
                error
            }
        } catch (e: Exception) {
            val error = AuthUiResult.Error(e, getErrorMessage(e))
            _authState.value = error
            error
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Sign in with Google using new Credential Manager API (recommended for Android 14+)
     */
    suspend fun signInWithGoogle(context: Context): AuthUiResult<FirebaseUser> {
        return try {
            _isLoading.value = true
            _authState.value = AuthUiResult.Loading

            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            handleGoogleSignInResult(result)
        } catch (e: Exception) {
            val error = AuthUiResult.Error(e, getErrorMessage(e))
            _authState.value = error
            error
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Handle Google Sign-In result from Credential Manager
     */
    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse): AuthUiResult<FirebaseUser> {
        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        firebaseAuthWithGoogle(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        AuthUiResult.Error(e, "Failed to parse Google ID token")
                    }
                } else {
                    AuthUiResult.Error(Exception("Invalid credential type"), "Invalid credential type")
                }
            }
            else -> {
                AuthUiResult.Error(Exception("Invalid credential"), "Invalid credential type")
            }
        }
    }

    /**
     * Authenticate with Firebase using Google ID token
     */
    private suspend fun firebaseAuthWithGoogle(idToken: String): AuthUiResult<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                _authState.value = AuthUiResult.Success(user)
                AuthUiResult.Success(user)
            } else {
                val error = AuthUiResult.Error(
                    Exception("Google sign-in failed"),
                    "Unable to sign in with Google. Please try again."
                )
                _authState.value = error
                error
            }
        } catch (e: Exception) {
            val error = AuthUiResult.Error(e, getErrorMessage(e))
            _authState.value = error
            error
        }
    }

    /**
     * Legacy Google Sign-In using GoogleSignInClient (fallback for older devices)
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Handle Google Sign-In result from legacy API
     */
    suspend fun handleGoogleSignInResult(data: Intent?): AuthUiResult<FirebaseUser> {
        return try {
            _isLoading.value = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                AuthUiResult.Error(Exception("No ID token"), "Google sign-in failed: No ID token")
            }
        } catch (e: ApiException) {
            AuthUiResult.Error(e, "Google sign-in failed: ${e.statusCode}")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthUiResult<Unit> {
        return try {
            _isLoading.value = true
            auth.sendPasswordResetEmail(email).await()
            AuthUiResult.Success(Unit)
        } catch (e: Exception) {
            AuthUiResult.Error(e, getErrorMessage(e))
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(context: Context): AuthUiResult<Unit> {
        return try {
            _isLoading.value = true

            // Sign out from Firebase
            auth.signOut()

            // Clear credentials from CredentialManager
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // CredentialManager might not be available on all devices
            }

            // Sign out from Google Sign-In (legacy)
            getGoogleSignInClient(context).signOut().await()

            _authState.value = AuthUiResult.Success(null)
            AuthUiResult.Success(Unit)
        } catch (e: Exception) {
            AuthUiResult.Error(e, getErrorMessage(e))
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(displayName: String?, photoUrl: String?): AuthUiResult<FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")

            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()

            user.updateProfile(profileUpdates).await()
            AuthUiResult.Success(user)
        } catch (e: Exception) {
            AuthUiResult.Error(e, getErrorMessage(e))
        }
    }

    /**
     * Delete user account
     */
    suspend fun deleteAccount(): AuthUiResult<Unit> {
        return try {
            _isLoading.value = true
            val user = auth.currentUser ?: throw Exception("No user logged in")
            user.delete().await()
            _authState.value = AuthUiResult.Success(null)
            AuthUiResult.Success(Unit)
        } catch (e: Exception) {
            AuthUiResult.Error(e, getErrorMessage(e))
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Get user-friendly error message
     */
    private fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                "Password is too weak. Use at least 6 characters."
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password. Please check your credentials."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "An account already exists with this email."
            is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                "No account found with this email. Please register first."
            is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException ->
                "Please sign in again to complete this action."
            is com.google.android.gms.common.api.ApiException ->
                "Google sign-in failed. Please try again."
            else -> exception.message ?: "An unexpected error occurred. Please try again."
        }
    }
}

/**
 * Extension function to convert AuthUiResult to user-friendly message
 */
fun AuthUiResult.Error.toUserMessage(): String = this.message
