package com.example.smarty

import android.util.Patterns
import com.example.smarty.ui.components.*
import com.example.smarty.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

@Composable
fun SmartyApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    val context = LocalContext.current
    var user by remember { mutableStateOf(auth.currentUser) }
    var verifiedUid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.uid) {
        verifiedUid = null
        val current = user
        if (current != null && current.isEmailVerified) {
            current.getIdToken(true).addOnSuccessListener {
                if (auth.currentUser?.uid == current.uid) verifiedUid = current.uid
            }
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            user = it.currentUser
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val currentUser = user

        if (currentUser == null) {
            AuthScreen(auth)
        } else if (verifiedUid != currentUser.uid) {
            key(currentUser.uid) {
                VerifyEmailScreen(
                    user = currentUser,
                    onVerified = {
                        if (auth.currentUser?.uid == currentUser.uid) {
                            verifiedUid = currentUser.uid
                        }
                    },
                    onLogout = {
                        auth.signOut()
                        disableExpiryReminders(context)
                    }
                )
            }
        } else {
            key(currentUser.uid) {
                SharedFridgeHome(
                    userId = currentUser.uid,
                    onLogout = {
                        auth.signOut()
                        disableExpiryReminders(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun VerifyEmailScreen(user: FirebaseUser, onVerified: () -> Unit, onLogout: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var cooldown by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("Verify your email before opening your fridges.") }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Verify your email", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(user.email.orEmpty())
        Text(message)
        Spacer(Modifier.height(16.dp))
        Button(enabled = !busy && cooldown == 0, onClick = {
            busy = true
            cooldown = 60
            user.sendEmailVerification().addOnCompleteListener { task ->
                busy = false
                message = if (task.isSuccessful) "Email sent. Check your inbox and spam folder."
                    else "Could not send the email. Please try again later."
            }
        }) { Text(if (cooldown > 0) "Send again in ${cooldown}s" else "Send verification email") }
        Button(enabled = !busy, onClick = {
            busy = true
            user.reload().addOnCompleteListener { reload ->
                if (!reload.isSuccessful || !user.isEmailVerified) {
                    busy = false
                    message = "Verification not confirmed. Open the email link, then try again."
                } else {
                    user.getIdToken(true).addOnCompleteListener { token ->
                        busy = false
                        if (token.isSuccessful) onVerified()
                        else message = "Could not refresh your session. Try again or sign in again."
                    }
                }
            }
        }) { Text("I have verified my email") }
        TextButton(enabled = !busy, onClick = onLogout) { Text("Sign out") }
    }
}

@Composable
private fun AuthScreen(auth: FirebaseAuth) {
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SmartyBrand(large = true)
        Spacer(Modifier.height(8.dp))
        Text("Less waste. More meals.", color = InkMuted, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        Surface(color = Color.White.copy(alpha = 0.55f), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, WarmOutline)) {
            Column(Modifier.padding(20.dp)) {
                Text(if (isRegistering) "Let's get you started" else "Welcome back", style = MaterialTheme.typography.headlineSmall)
                Text(if(isRegistering) "Good food starts with a little teamwork." else "Your groceries have missed you.",
                    style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (isRegistering) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it },
                label = { Text("Confirm password") },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let { message ->
            Spacer(Modifier.height(12.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val cleanEmail = email.trim()

                error = when {
                    !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() ->
                        "Enter a valid email address."

                    password.isBlank() ->
                        "Enter your password."

                    isRegistering && password.length < 6 ->
                        "Use at least 6 characters for your password."

                    isRegistering && password != confirmation ->
                        "Passwords do not match."

                    else -> null
                }

                if (error == null) {
                    isLoading = true

                    val request = if (isRegistering) {
                        auth.createUserWithEmailAndPassword(
                            cleanEmail,
                            password
                        )
                    } else {
                        auth.signInWithEmailAndPassword(
                            cleanEmail,
                            password
                        )
                    }

                    request.addOnCompleteListener { task ->
                        isLoading = false

                        if (task.isSuccessful) {
                            password = ""
                            confirmation = ""
                        } else {
                            error = task.exception?.localizedMessage
                                ?: "Authentication failed. Try again."
                        }
                    }
                }
            }
        ) {
            Text(
                when {
                    isLoading -> "Please wait..."
                    isRegistering -> "Create account"
                    else -> "Log in"
                }
            )
        }

        TextButton(
            enabled = !isLoading,
            onClick = {
                isRegistering = !isRegistering
                error = null
                password = ""
                confirmation = ""
            }
        ) {
            Text(
                if (isRegistering) {
                    "Already have an account? Log in"
                } else {
                    "New here? Create an account"
                }
            )
        }
            }
        }
    }
}
