import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.DescriptionRequest
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay

@Composable
fun DescribeWordScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<WordResult?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isGuessing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var lastWord by remember { mutableStateOf("") }

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

        Button(
            onClick = {
                isGuessing = true
                coroutineScope.launch {
                    try {
                        val wordResult = RetrofitInstance.apiService.describeWord(DescriptionRequest(inputText))
                        result = wordResult
                        lastWord = wordResult.word
                        focusRequester.freeFocus() // Unfocus the text field
                    } catch (e: Exception) {
                        showResult(navController.context, "Error: ${e.message}")
                    } finally {
                        isGuessing = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = inputText.isNotEmpty() && !isGuessing && !isSaving
        ) {
            if (isGuessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Guess")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isGuessing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        result?.let {
            Text("Word: ${it.word}", style = MaterialTheme.typography.bodyLarge)
            Text("Translation: ${it.translation}", style = MaterialTheme.typography.bodyLarge)
            Text("Definition: ${it.definition}", style = MaterialTheme.typography.bodyLarge)
            Text("Examples:", style = MaterialTheme.typography.bodyLarge)
            it.examples.forEach { ex ->
                Text("- $ex", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        try {
                            RetrofitInstance.apiService.saveWord(it)
                            inputText = ""
                            result = null
                            showSnackbar = true // Show success message
                            Log.d("Snackbar", "Snackbar shown")
                        } catch (e: Exception) {
                            showResult(navController.context, "Error: ${e.message}")
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isGuessing && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Word")
                }
            }
        }

        if (showSnackbar) {
            Snackbar(
                modifier = Modifier.padding(8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text("Word \"$lastWord\" saved.", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            // Trigger the effect when `showSnackbar` changes
            LaunchedEffect(showSnackbar) {
                delay(2000) // Show the snackbar for 2 seconds
                showSnackbar = false
                Log.d("Snackbar", "Snackbar dismissed")
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
