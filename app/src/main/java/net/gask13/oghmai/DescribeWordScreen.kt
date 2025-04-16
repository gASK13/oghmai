import android.content.Context
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.DescriptionRequest
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance

@Composable
fun DescribeWordScreen(navController: NavController) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var results by rememberSaveable { mutableStateOf<List<WordResult>>(emptyList()) }
    var isGuessing by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Describe Word", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter a word or description") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusRequester.freeFocus() }),
            enabled = !isGuessing && !isSaving
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    isGuessing = true
                    coroutineScope.launch {
                        try {
                            val wordResult = RetrofitInstance.apiService.describeWord(
                                DescriptionRequest(inputText, results.map { it.word })
                            )
                            results = results + wordResult
                            focusRequester.freeFocus()
                        } catch (e: Exception) {
                            showResult(navController.context, "Error: ${e.message}")
                        } finally {
                            isGuessing = false
                        }
                    }
                },
                enabled = inputText.isNotEmpty() && !isGuessing && !isSaving
            ) {
                if (isGuessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (results.isEmpty()) "Guess" else "Next Guess")
                }
            }

            Button(
                onClick = {
                    inputText = ""
                    results = emptyList()
                },
                enabled = !isGuessing && !isSaving && results.isNotEmpty()
            ) {
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val state = rememberLazyListState()

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Ensures LazyRow occupies the remaining vertical space
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
        ) {
            items(results.size) { index ->
                val result = results[index]
                Card(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .fillMaxHeight() // Ensures all cards have the same height as LazyRow
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Ensures content fills the card
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()) // Enables scrolling within the card
                        ) {
                            Text("Word: ${result.word}", style = MaterialTheme.typography.bodyLarge)
                            Text("Translation: ${result.translation}", style = MaterialTheme.typography.bodyLarge)
                            Text("Definition: ${result.definition}", style = MaterialTheme.typography.bodyLarge)
                            Text("Examples:", style = MaterialTheme.typography.bodyLarge)
                            result.examples.forEach { ex ->
                                Text("- $ex", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Button(
                            modifier = Modifier
                                .align(Alignment.BottomEnd) // Positions the button at the bottom-right corner
                                .padding(8.dp),
                            onClick = {
                                isSaving = true
                                coroutineScope.launch {
                                    try {
                                        RetrofitInstance.apiService.saveWord(result)
                                        result.saved = true
                                    } catch (e: Exception) {
                                        showResult(navController.context, "Error: ${e.message}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving && !result.saved,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = if (result.saved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (result.saved) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Saved",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun showResult(context: Context, result: String) {
    val builder = android.app.AlertDialog.Builder(context)
    builder.setTitle("ERROR")
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
