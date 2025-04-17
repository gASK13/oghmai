package net.gask13.oghmai

import DescribeWordScreen
import android.util.Log
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.gask13.oghmai.ui.WordListingScreen
import net.gask13.oghmai.ui.WordDetailActivity
import net.gask13.oghmai.ui.WordDetailScreen
import android.speech.tts.TextToSpeech
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ITALIAN
            }
        }

        setContent {
            val navController = rememberNavController()

            OghmAINavHost(navController, textToSpeech)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OghmAI") },
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { onNavigate("describeWord") }) {
                Text("Describe Word")
            }

            DisabledButton("Add Word")
            Button(onClick = { onNavigate("listWords") }) {
                Text("List Words")
            }
            DisabledButton("Test")
        }
    }
}

@Composable
fun DisabledButton(text: String) {
    Button(onClick = {}, enabled = false) {
        Text("$text (Not Implemented)")
    }
}

@Composable
fun SettingsScreen(
    version: String = "v0.1-dev",
    user: String = "test",
    language: String = "Italian"
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Version: $version")
        Text("Username: $user")
        Text("Language: $language")
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun OghmAINavHost(navController: NavHostController, textToSpeech: TextToSpeech) {
    NavHost(navController, startDestination = "main",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }, // Starts off-screen to the right
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth }, // Moves off-screen to the left
                animationSpec = tween(durationMillis = 500)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }, // Reverse direction for back navigation
                animationSpec = tween(durationMillis = 500)
            ) + fadeIn(animationSpec = tween(durationMillis = 300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth }, // Reverse exit direction
                animationSpec = tween(durationMillis = 500)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        }) {
        composable("main") {
            MainMenuScreen { destination ->
                navController.navigate(destination) {
                    // Clear the back stack when navigating to the main menu
                    Log.d("Navigation", "Navigating to $destination")
                    if (destination == "main") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
        }
        composable("describeWord") { DescribeWordScreen(navController) }
        composable("listWords") { WordListingScreen(navController) }
        composable("wordDetail/{word}") { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            WordDetailScreen(word, navController, textToSpeech)
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}

private fun showDeleteResult(context: Context, result: String) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("DELETE")
        .setMessage(result)
        .setCancelable(true) // Disables touch outside dialog to dismiss it
        // "No" button, dismiss dialog
        .setPositiveButton("Ok") { dialog, id ->
            dialog.dismiss()
        }

    // Create and show the dialog
    val dialog = builder.create()
    dialog.show()
}

