import android.content.Context
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun DescribeWordScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<WordResult?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val wordResult = RetrofitInstance.apiService.describeWord(DescriptionRequest(inputText))
                        result = wordResult
                    } catch (e: Exception) {
                        showResult(navController.context, "Error: ${e.message}")
                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Guess")
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    coroutineScope.launch {
                        try {
                            RetrofitInstance.apiService.saveWord(it)
                            inputText = ""
                            result = null
                        } catch (e: Exception) {
                            showResult(navController.context, "Error: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Word")
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
