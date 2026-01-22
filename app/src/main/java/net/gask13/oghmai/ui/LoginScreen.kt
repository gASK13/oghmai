package net.gask13.oghmai.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.R
import net.gask13.oghmai.auth.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (() -> Unit)? = null
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    // Load saved credentials when the screen is first displayed
    suspend fun getStoredCredentials(): Credential? {
        try {
            val getPasswordOption = GetPasswordOption()
            val request = GetCredentialRequest(listOf(getPasswordOption))

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            return credential
        } catch (e: GetCredentialException) {
            Log.e("LoginScreen", "Error getting credentials", e)
            return null
        } catch (e: Exception) {
            Log.e("LoginScreen", "Unexpected error getting credentials", e)
            return null
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // First check if we already have a valid session (should not happen due to NAV, but doesn't hurt to check)
                if (AuthManager.validateSession()) {
                    Log.d("LoginScreen", "Valid session found, navigating to main screen")
                    if (onLoginSuccess != null) {
                        onLoginSuccess()
                    } else {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                    return@launch
                }

                // Try to get credentials from Credentials API
                val credential = getStoredCredentials()

                if (credential is PasswordCredential) {
                    username = credential.id
                    password = credential.password
                    isLoading = true
                    // Login and navigate on
                    AuthManager.signIn(username, password) // Always save credentials from the Credentials API
                    if (onLoginSuccess != null) {
                        onLoginSuccess()
                    } else {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Login failed"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App logo icon
        Icon(
            painter = painterResource(id = R.drawable.ic_oghmai_round), // Use your app logo resource
            contentDescription = "App Logo",
            modifier = Modifier.size(196.dp), // Adjust the size of the icon
            tint = Color.Unspecified // Use the default tint color
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to OghmAI",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            isError = errorMessage != null
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = errorMessage != null
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter both username and password"
                    return@Button
                }

                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        AuthManager.signIn(username, password)
                        try {
                            val request = CreatePasswordRequest(
                                id = username,
                                password = password
                            )
                            credentialManager.createCredential(context, request)
                        } catch (e: CreateCredentialException) {
                            Log.e("LoginScreen", "Error saving credentials", e)
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "Unexpected error saving credentials", e)
                        }

                        // Navigate to the main screen on successful login
                        if (onLoginSuccess != null) {
                            onLoginSuccess()
                        } else {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.message ?: "Login failed"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In")
            }
        }
    }
}