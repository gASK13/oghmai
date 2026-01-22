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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
import net.gask13.oghmai.notifications.NotificationHelper
import net.gask13.oghmai.notifications.NotificationScheduler
import net.gask13.oghmai.preferences.PreferencesManager
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.*
import net.gask13.oghmai.ui.components.MenuButton
import net.gask13.oghmai.ui.components.OptionMenuItem
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar
import net.gask13.oghmai.util.SoundEffectsManager

class MainActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeechWrapper
    private lateinit var soundEffectsManager: SoundEffectsManager
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeechWrapper()
        textToSpeech.initializeTextToSpeech(this)

        soundEffectsManager = SoundEffectsManager(this)
        soundEffectsManager.initialize()

        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule notifications if enabled
        val preferencesManager = PreferencesManager(this)
        if (preferencesManager.isNotificationsEnabled()) {
            NotificationScheduler.scheduleDailyNotification(
                this,
                preferencesManager.getNotificationHour(),
                preferencesManager.getNotificationMinute()
            )
        }

        // Extract shared text from intent if present
        val sharedText = extractSharedText(intent)

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
                OghmAINavHost(navController, textToSpeech, soundEffectsManager, sharedText)
            } else {
                LoadingScreen()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle new shared text when app is already running
        val sharedText = extractSharedText(intent)
        if (sharedText != null) {
            // We need to restart the activity to pass the new intent
            // This is a simple approach - a more sophisticated one would use a shared state
            finish()
            startActivity(intent)
        }
    }

    private fun extractSharedText(intent: android.content.Intent?): String? {
        if (intent?.action == android.content.Intent.ACTION_SEND &&
            intent.type == "text/plain") {
            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)

            if (text.isNullOrBlank()) {
                Log.w("MainActivity", "Received empty shared text")
                return null
            }

            // Extract the first word from the shared text
            // Handle cases where multiple words or sentences are shared
            val firstWord = text.trim()
                .split(Regex("\\s+")) // Split by whitespace
                .firstOrNull()
                ?.replace(Regex("[^a-zA-ZÀ-ÿ'-]"), "") // Remove non-letter characters except apostrophes and hyphens
                ?.take(100) // Limit to 100 characters

            if (firstWord.isNullOrBlank()) {
                Log.w("MainActivity", "No valid word found in shared text: $text")
                return null
            }

            Log.d("MainActivity", "Extracted shared word: $firstWord from text: $text")
            return firstWord
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
        soundEffectsManager.release()
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
            ),
            OptionMenuItem(
                text = "About",
                icon = Icons.Default.Info,
                onClick = { onNavigate("about") }
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
            MenuButton(
                icon = { Icon(Icons.Default.Notifications, contentDescription = "") },
                name = "Match Challenge",
                onClick = { onNavigate("matchChallenge") }
            )
        }
    }
}

// SettingsScreen moved to a separate file in ui package

@Composable
fun OghmAINavHost(
    navController: NavHostController,
    textToSpeech: TextToSpeechWrapper,
    soundEffectsManager: SoundEffectsManager,
    sharedText: String? = null
) {
    // Check if a user is authenticated
    var isAuthenticated by remember { mutableStateOf(AuthManager.isSignedIn()) }
    var pendingSharedText by remember { mutableStateOf(sharedText) }

    // Determine the start destination based on authentication status and shared text
    val startDestination = when {
        !isAuthenticated -> "login"
        sharedText != null -> "discoverWord/$sharedText"
        else -> "main"
    }

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
            LoginScreen(navController, onLoginSuccess = {
                if (pendingSharedText != null) {
                    navController.navigate("discoverWord/$pendingSharedText") {
                        popUpTo("login") { inclusive = true }
                    }
                    pendingSharedText = null
                } else {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            })
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
        composable("about") {
            AboutScreen(navController)
        }
        composable("testWords") {
            ChallengeScreen(navController, textToSpeech, soundEffectsManager)
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
        composable("matchChallenge") {
            MatchChallengeScreen(navController, soundEffectsManager)
        }
    }
}
