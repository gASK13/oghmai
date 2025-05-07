package net.gask13.oghmai.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.auth.AuthManager
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar

@Composable
fun SettingsScreen(
    navController: NavController,
    version: String = "v0.1-dev",
    language: String = "Italian"
) {
    val username = AuthManager.getCurrentUsername() ?: "Not logged in"
    val coroutineScope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }

    ScaffoldWithTopBar(
        title = "Settings",
        isMainScreen = false,
        showOptionsMenu = false
    ) {
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
                            AuthManager.signOut()
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
}