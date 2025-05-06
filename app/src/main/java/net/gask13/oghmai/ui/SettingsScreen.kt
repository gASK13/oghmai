package net.gask13.oghmai.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.auth.AuthManager

@Composable
fun SettingsScreen(
    navController: NavController,
    authManager: AuthManager,
    version: String = "v0.1-dev",
    language: String = "Italian"
) {
    val username = authManager.getCurrentUsername() ?: "Not logged in"
    val coroutineScope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Version: $version")
        Text("Username: $username")
        Text("Language: $language")
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoggingOut = true
                    try {
                        authManager.signOut()
                        // Navigate to login screen
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsScreen", "Error signing out", e)
                    } finally {
                        isLoggingOut = false
                    }
                }
            },
            enabled = !isLoggingOut && username != "Not logged in",
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Out")
            }
        }
    }
}