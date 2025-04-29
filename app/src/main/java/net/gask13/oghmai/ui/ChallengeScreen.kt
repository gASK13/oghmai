package net.gask13.oghmai.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.ResultEnum
import net.gask13.oghmai.model.TestChallenge
import net.gask13.oghmai.model.TestResult
import net.gask13.oghmai.model.WordStatus
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.ui.components.WordStatusBadge
import retrofit2.HttpException
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import net.gask13.oghmai.R
import net.gask13.oghmai.services.TextToSpeechWrapper

@Composable
fun ChallengeScreen(navController: NavController, textToSpeech: TextToSpeechWrapper) {
    val coroutineScope = rememberCoroutineScope()
    var challenge by remember { mutableStateOf<TestChallenge?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var userInput by remember { mutableStateOf("") }
    var dialogState by remember { mutableStateOf<DialogState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        fetchNextChallenge(coroutineScope, snackbarHostState) { fetchedChallenge ->
            challenge = fetchedChallenge
            isLoading = false
            if (fetchedChallenge == null) {
                dialogState = DialogState.NoMoreWords
            } else {
                focusRequester.requestFocus()
                keyboardController?.show() // Ensure the keyboard opens
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Description Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
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
                    Text(
                        text = challenge?.description ?: "",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Icon(
                        painter = painterResource(
                            id = if (textToSpeech.storedUtteranceId == challenge?.description) R.drawable.ic_pause else R.drawable.ic_speaker
                        ),
                        contentDescription = if (textToSpeech.storedUtteranceId == challenge?.description) "Pause Speech" else "Speak Example",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp) // Increased size
                            .align(Alignment.BottomEnd) // Positioned in the bottom right corner
                            .clickable {
                                if (textToSpeech.storedUtteranceId == challenge?.description) {
                                    textToSpeech.stop()
                                } else {
                                    textToSpeech.speak(challenge!!.description, challenge!!.description)
                                }
                            }
                            .padding(8.dp) // Padding for better touch target
                    )
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
                            dialogState = DialogState.Result(
                                result = result.result,
                                word = result.word,
                                status = result.newStatus
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Error submitting guess: ${e.message}",
                                duration = SnackbarDuration.Short
                            )
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isSubmitting
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

    if (dialogState == DialogState.NoMoreWords) {
        AlertDialog(
            onDismissRequest = { navController.navigate("main") },
            title = { Text("No more words") },
            text = { Text("There are no more challenges available.") },
            confirmButton = {
                TextButton(onClick = { navController.navigate("main") }) {
                    Text("Back to Menu")
                }
            }
        )
    }

    (dialogState as? DialogState.Result)?.let { result ->
        AlertDialog(
            onDismissRequest = { dialogState = null },
            title = {
                Text(
                    when {
                        result.result == ResultEnum.CORRECT -> "Correct!"
                        result.result == ResultEnum.PARTIAL -> "Close!"
                        else -> "Incorrect"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        when {
                            result.result == ResultEnum.CORRECT -> "You guessed '${result.word}' correctly!"
                            result.result == ResultEnum.PARTIAL -> "This is not the word we're looking for, but you're close!"
                            else -> "Incorrect! The word was '${result.word}'."
                        }
                    )
                    if (result.result != ResultEnum.PARTIAL && result.status != null) {
                        WordStatusBadge(wordStatus = result.status)
                    }
                }
            },
            confirmButton = {
                if (result.result == ResultEnum.PARTIAL) {
                    TextButton(onClick = {
                        dialogState = null
                        userInput = "" // Clear input for the next guess
                    }) {
                        Text("Try Again")
                    }
                } else {
                    TextButton(onClick = {
                        dialogState = null
                        isLoading = true
                        fetchNextChallenge(coroutineScope, snackbarHostState) { fetchedChallenge ->
                            challenge = fetchedChallenge
                            userInput = ""
                            isLoading = false
                            if (fetchedChallenge == null) {
                                dialogState = DialogState.NoMoreWords
                            } else {
                                focusRequester.requestFocus()
                            }
                        }
                    }) {
                        Text("Next Test")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { navController.navigate("main") }) {
                    Text("Back to Menu")
                }
            }
        )
    }

    // SnackbarHost positioned at the bottom of the screen
    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
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
                snackbarHostState.showSnackbar(
                    message = "Error loading next test: ${response.errorBody()?.string()}",
                    duration = SnackbarDuration.Short
                )
            }
        } catch (e: HttpException) {
            snackbarHostState.showSnackbar(
                message = "Error loading next test: ${e.message}",
                duration = SnackbarDuration.Short
            )
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                message = "Unexpected error: ${e.message}",
                duration = SnackbarDuration.Short
            )
        }
    }
}

private suspend fun submitGuess(id: String, guess: String): TestResult {
    return RetrofitInstance.apiService.submitChallengeGuess(id, guess)
}

sealed class DialogState {
    object NoMoreWords : DialogState()
    data class Result(
        val result: ResultEnum,
        val word: String?,
        val status: WordStatus?
    ) : DialogState()
}
