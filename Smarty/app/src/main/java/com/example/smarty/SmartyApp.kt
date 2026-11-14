package com.example.smarty

import android.util.Patterns
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

@Composable
fun SmartyApp() {
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

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
        } else {
            key(currentUser.uid) {
                FridgeScreen(
                    userId = currentUser.uid,
                    onLogout = { auth.signOut() }
                )
            }
        }
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
        Text(
            text = "Smarty",
            style = MaterialTheme.typography.headlineLarge
        )

        Text("Less waste. A smarter fridge.")

        Spacer(Modifier.height(32.dp))

        Text(
            text = if (isRegistering) "Create an account" else "Log in",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(16.dp))

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