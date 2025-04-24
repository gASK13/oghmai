package net.gask13.oghmai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.gask13.oghmai.ui.components.WordStatusBadge
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.model.TestChallenge
import net.gask13.oghmai.model.TestResult
import retrofit2.HttpException

@Composable
fun ChallengeScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var challenge by remember { mutableStateOf<TestChallenge?>(null) }
    var userInput by remember { mutableStateOf("") }
    var dialogState by remember { mutableStateOf<DialogState?>(null) }

    LaunchedEffect(Unit) {
        fetchNextChallenge(coroutineScope, { fetchedChallenge ->
            challenge = fetchedChallenge
            if (fetchedChallenge == null) {
                dialogState = DialogState.NoMoreWords
            }
        })
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

    challenge?.let { currentChallenge ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(currentChallenge.description, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it },
                label = { Text("Your Answer") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = submitGuess(currentChallenge.id, userInput)
                        dialogState = DialogState.Result(
                            correct = result.result == net.gask13.oghmai.model.ResultEnum.CORRECT,
                            word = result.word,
                            status = result.newStatus
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
        }
    }

    (dialogState as? DialogState.Result)?.let { result ->
        AlertDialog(
            onDismissRequest = { dialogState = null },
            title = {
                Text(
                    if (result.correct) "Correct!"
                    else "Incorrect"
                )
            },
            text = {
                Column {
                    Text(
                        if (result.correct)
                            "You guessed '${result.word}' correctly!"
                        else
                            "Incorrect! The word was '${result.word}'."
                    )
                    WordStatusBadge(wordStatus = result.status)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogState = null
                    fetchNextChallenge(coroutineScope, { fetchedChallenge ->
                        challenge = fetchedChallenge
                        userInput = ""
                        if (fetchedChallenge == null) {
                            dialogState = DialogState.NoMoreWords
                        }
                    })
                }) {
                    Text("Next Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { navController.navigate("main") }) {
                    Text("Back to Menu")
                }
            }
        )
    }
}

private fun fetchNextChallenge(coroutineScope: CoroutineScope, onResult: (TestChallenge?) -> Unit) {
    coroutineScope.launch {
        try {
            val response = RetrofitInstance.apiService.getNextTest()
            if (response.isSuccessful) {
                onResult(response.body())
            } else if (response.code() == 204) {
                onResult(null)
            }
        } catch (e: HttpException) {
            onResult(null)
        }
    }
}

private suspend fun submitGuess(id: String, guess: String): TestResult {
    return RetrofitInstance.apiService.submitChallengeGuess(id, guess)
}

sealed class DialogState {
    object NoMoreWords : DialogState()
    data class Result(
        val correct: Boolean,
        val word: String,
        val status: net.gask13.oghmai.model.WordStatus
    ) : DialogState()
}

