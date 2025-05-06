package net.gask13.oghmai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.ChallengeScreen
import net.gask13.oghmai.ui.WordDetailScreen
import net.gask13.oghmai.ui.WordDiscoveryScreen
import net.gask13.oghmai.ui.WordListingScreen
import net.gask13.oghmai.ui.components.MenuButton
import net.gask13.oghmai.ui.components.OptionMenuItem
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar

class MainActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeechWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeechWrapper()
        textToSpeech.initializeTextToSpeech(this)

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
    ScaffoldWithTopBar(
        title = "OghmAI",
        isMainScreen = true,
        showOptionsMenu = true,
        optionsMenuItems = listOf(
            OptionMenuItem(
                text = "Settings",
                icon = Icons.Default.Settings,
                onClick = { onNavigate("settings") }
            )
        )
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            MenuButton(
                icon = { Icon(Icons.Default.Search, contentDescription = "") },
                name = "Describe Word",
                onClick = { onNavigate("describeWord") }
            )
            MenuButton(
                icon = { Icon(Icons.Default.Menu, contentDescription = "") },
                name = "List Words",
                onClick = { onNavigate("listWords") }
            )
            MenuButton(
                icon = { Icon(Icons.Default.Edit, contentDescription = "") },
                name = "Test Knowledge",
                onClick = { onNavigate("testWords") }
            )
        }
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
fun OghmAINavHost(navController: NavHostController, textToSpeech: TextToSpeechWrapper) {
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
        composable("describeWord") { WordDiscoveryScreen(navController, textToSpeech) }
        composable("listWords") { WordListingScreen(navController) }
        composable("wordDetail/{word}") { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            WordDetailScreen(word, navController,  textToSpeech)
        }
        composable("settings") {
            SettingsScreen()
        }
        composable("testWords") {
            ChallengeScreen(navController, textToSpeech)
        }
        composable("discoverWord/{word}") { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            WordDiscoveryScreen(navController, textToSpeech, word)
        }
    }
}
