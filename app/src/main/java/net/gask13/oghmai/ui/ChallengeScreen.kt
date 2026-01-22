package net.gask13.oghmai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.gask13.oghmai.R
import net.gask13.oghmai.model.ResultEnum
import net.gask13.oghmai.model.TestChallenge
import net.gask13.oghmai.model.TestResult
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar
import net.gask13.oghmai.ui.components.WordStatusBadge
import net.gask13.oghmai.ui.components.AnimatedWordStatusBadge
import net.gask13.oghmai.util.SnackbarManager
import net.gask13.oghmai.util.SoundEffectsManager
import retrofit2.HttpException

// Custom Saver for TestChallenge
private val testChallengeSaver = Saver<TestChallenge?, List<String?>>(
    save = { challenge ->
        if (challenge == null) {
            listOf(null, null)
        } else {
            listOf(challenge.description, challenge.id)
        }
    },
    restore = { savedList ->
        if (savedList[0] == null || savedList[1] == null) {
            null
        } else {
            TestChallenge(
                description = savedList[0]!!,
                id = savedList[1]!!
            )
        }
    }
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(navController: NavController, textToSpeech: TextToSpeechWrapper, soundEffectsManager: SoundEffectsManager) {
    val coroutineScope = rememberCoroutineScope()
    var challenge by rememberSaveable(stateSaver = testChallengeSaver) { mutableStateOf(null) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var userInput by rememberSaveable { mutableStateOf("") }
    var resultState by remember { mutableStateOf<TestResult?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    // Flag to track if we've already loaded a challenge
    var hasLoadedChallenge by rememberSaveable { mutableStateOf(false) }

    // Only load a challenge if we haven't loaded one yet
    LaunchedEffect(true) {
        if (!hasLoadedChallenge) {
            fetchNextChallenge(coroutineScope, snackbarHostState) { fetchedChallenge ->
                challenge = fetchedChallenge
                isLoading = false
                hasLoadedChallenge = true
                if (fetchedChallenge == null) {
                    resultState = null // No more challenges
                } else {
                    focusRequester.requestFocus()
                    keyboardController?.show() // Ensure the keyboard opens
                }
            }
        }
    }

    ScaffoldWithTopBar(
        title = "Test Knowledge",
        isMainScreen = false,
        onBackClick = { navController.navigateUp() },
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Light blue background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        if (challenge == null) {
                            Text(
                                text = "No more challenges available today!",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else if (resultState != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (resultState?.result) {
                                        ResultEnum.CORRECT -> "Correct! You guessed '${resultState?.word}'!"
                                        ResultEnum.INCORRECT -> "Incorrect! The word was '${resultState?.word}'."
                                        else -> ""
                                    },
                                    fontSize = 18.sp, // Increased font size
                                    fontWeight = FontWeight.Bold, // Made text bold
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (resultState?.result == ResultEnum.CORRECT) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                                resultState?.let { result ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    AnimatedWordStatusBadge(
                                        wordStatus = result.newStatus,
                                        previousStatus = result.oldStatus,
                                        showCelebration = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        isLoading = true
                                        hasLoadedChallenge = false // Reset the flag to allow loading a new challenge
                                        fetchNextChallenge(coroutineScope, snackbarHostState) { fetchedChallenge ->
                                            resultState = null
                                            challenge = fetchedChallenge
                                            isLoading = false
                                            hasLoadedChallenge = true // Set the flag to true after loading
                                            userInput = ""
                                            focusRequester.requestFocus()
                                            keyboardController?.show() // Ensure the keyboard opens
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(8.dp)
                                ) {
                                    Text("Next Challenge")
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                // Confine the text to the upper part of the card
                                Box(
                                    modifier = Modifier
                                        .weight(1f) // Take up available vertical space
                                        .verticalScroll(rememberScrollState()) // Make content scrollable
                                ) {
                                    FlowRow {
                                        challenge!!.description.split(" ").forEach { word ->
                                            Text(
                                                text = "$word ",
                                                fontSize = 18.sp, // Increased font size
                                                fontWeight = FontWeight.Bold, // Made text bold
                                                modifier = Modifier
                                                    .pointerInput(Unit) {
                                                        detectTapGestures(onLongPress = {
                                                            navController.navigate("discoverWord/$word")
                                                        })
                                                    }
                                            )
                                        }
                                    }
                                }
                                // Bottom section with small text and icon
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Hold on a word to explain or save it",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                    Icon(
                                        painter = painterResource(
                                            id = if (textToSpeech.storedUtteranceId == challenge?.description) R.drawable.ic_pause else R.drawable.ic_speaker
                                        ),
                                        contentDescription = if (textToSpeech.storedUtteranceId == challenge?.description) "Pause Speech" else "Speak Example",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(48.dp) // Increased size
                                            .clickable {
                                                if (textToSpeech.storedUtteranceId == challenge?.description) {
                                                    textToSpeech.stop()
                                                } else {
                                                    textToSpeech.speak(challenge!!.description, challenge!!.description)
                                                }
                                            }
                                            .padding(8.dp) // Padding for a better touch target
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input and Submit Section
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Your Answer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                Button(
                    onClick = {
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                val result = submitGuess(challenge!!.id, userInput)
                                if (result.result == ResultEnum.PARTIAL) {
                                    challenge = challenge?.copy(
                                        description = "${challenge!!.description}\n${result.suggestion}"
                                    )
                                    SnackbarManager.showInfo(
                                        snackbarHostState = snackbarHostState,
                                        message = "This is not the word we're looking for, but you're close!"
                                    )
                                    userInput = ""
                                    focusRequester.requestFocus()
                                    keyboardController?.show() // Ensure the keyboard opens
                                } else {
                                    // Play sound based on result
                                    if (result.result == ResultEnum.CORRECT) {
                                        soundEffectsManager.playCorrectSound()
                                    } else if (result.result == ResultEnum.INCORRECT) {
                                        soundEffectsManager.playIncorrectSound()
                                    }
                                    resultState = result
                                    userInput = ""
                                }
                            } catch (e: Exception) {
                                SnackbarManager.showOperationError(
                                    snackbarHostState = snackbarHostState,
                                    operation = "submit",
                                    exception = e
                                )
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !isSubmitting && resultState == null
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

private fun fetchNextChallenge(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onResult: (TestChallenge?) -> Unit
) {
    coroutineScope.launch {
        try {
            val response = RetrofitInstance.apiService.getNextTest()
            if (response.isSuccessful) {
                if (response.body() == null || response.code() == 204) {
                    onResult(null)
                }
                onResult(response.body())
            } else {
                SnackbarManager.showInfo(
                    snackbarHostState = snackbarHostState,
                    message = "No more tests available"
                )
            }
        } catch (e: Exception) {
            SnackbarManager.showOperationError(
                snackbarHostState = snackbarHostState,
                operation = "load",
                exception = e
            )
        }
    }
}

private suspend fun submitGuess(id: String, guess: String): TestResult {
    return RetrofitInstance.apiService.submitChallengeGuess(id, guess)
}
