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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.gask13.oghmai.auth.AuthManager
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.*
import net.gask13.oghmai.ui.components.MenuButton
import net.gask13.oghmai.ui.components.OptionMenuItem
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar

class MainActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeechWrapper
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeechWrapper()
        textToSpeech.initializeTextToSpeech(this)

        setContent {
            var isInitialized by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                // Initialize AWS Mobile Client and RetrofitInstance
                coroutineScope.launch {
                    try {
                        AuthManager.initialize(context = this@MainActivity)
                        Log.d("MainActivity", "AWS Mobile Client initialized")
                        isInitialized = true
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error initializing AWS Mobile Client", e)
                    }
                }
            }

            if (isInitialized) {
                val navController = rememberNavController()
                OghmAINavHost(navController, textToSpeech)
            } else {
                LoadingScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Circular progress indicator around the icon
            CircularProgressIndicator(
                modifier = Modifier.size(160.dp), // Adjust size to fit around the icon
                strokeWidth = 8.dp
            )
            // App logo icon
            Icon(
                painter = painterResource(id = R.drawable.ic_oghmai_round), // Use your app logo resource
                contentDescription = "App Logo",
                modifier = Modifier.size(128.dp), // Adjust the size of the icon
                tint = Color.Unspecified // Use the default tint color
            )
        }
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

// SettingsScreen moved to a separate file in ui package

@Composable
fun OghmAINavHost(navController: NavHostController, textToSpeech: TextToSpeechWrapper) {
    // Check if a user is authenticated
    var isAuthenticated by remember { mutableStateOf(AuthManager.isSignedIn()) }

    // Determine the start destination based on authentication status
    val startDestination = if (isAuthenticated) { "main" } else { "login" }

    NavHost(navController, startDestination = startDestination,
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
        composable("login") {
            LoginScreen(navController)
        }
        composable("main") {
            // Check authentication status when navigating to the main screen
            LaunchedEffect(Unit) {
                if (!AuthManager.validateSession()) {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }

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
            SettingsScreen(navController)
        }
        composable("testWords") {
            ChallengeScreen(navController, textToSpeech)
        }
        composable("discoverWord/{word}") { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            WordDiscoveryScreen(navController, textToSpeech, word)
        }
        composable("explainTenses/{word}") { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            ExplanationScreen(
                word = word,
                navController = navController,
                explanationType = "Tenses",
                getExplanation = { RetrofitInstance.apiService.getWordTenses(it) }
            )
        }
    }
}
