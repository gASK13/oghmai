import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.R
import net.gask13.oghmai.model.DescriptionRequest
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance

@Composable
fun DescribeWordScreen(navController: NavController, textToSpeech: TextToSpeech) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var results by rememberSaveable { mutableStateOf<List<WordResult>>(emptyList()) }
    var isGuessing by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var speakingWord by remember { mutableStateOf<String?>(null) } // Track which word/example is speaking
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                inputText = spokenText
            } else {
                Toast.makeText(context, "No speech detected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // No action needed on start
            }

            override fun onDone(utteranceId: String?) {
                speakingWord = null // Reset speakingWord when speech finishes
            }

            override fun onError(utteranceId: String?) {
                // Handle errors if needed
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                enabled = !isGuessing && !isSaving,
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT") // Set to Italian
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
                        }
                        speechRecognizerLauncher.launch(intent)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_microphone),
                            contentDescription = "Voice Input"
                        )
                    }
                }
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
                                if (wordResult.isSuccessful) {
                                    if (wordResult.body() != null) {
                                        results = listOf(wordResult.body()!!) + results
                                    } else {
                                        snackbarHostState.showSnackbar("No more matches!", duration = SnackbarDuration.Short, withDismissAction = true)
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Error: ${wordResult.errorBody()?.string()}", duration = SnackbarDuration.Short, withDismissAction = true)
                                }
                                focusRequester.freeFocus()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}", duration = SnackbarDuration.Short, withDismissAction = true)
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
                    .weight(1f),
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
                            .fillMaxHeight()
                            .padding(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Word Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.word,
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                                        textAlign = TextAlign.Center
                                    )
                                    Icon(
                                        painter = painterResource(
                                            id = if (speakingWord == result.word) R.drawable.ic_pause else R.drawable.ic_speaker
                                        ),
                                        contentDescription = if (speakingWord == result.word) "Pause Speech" else "Speak Word",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable {
                                                if (speakingWord == result.word) {
                                                    textToSpeech.stop()
                                                    speakingWord = null
                                                } else {
                                                    textToSpeech.speak(result.word, TextToSpeech.QUEUE_FLUSH, null, result.word)
                                                    speakingWord = result.word
                                                }
                                            }
                                    )
                                }
                            }

                            // Scrollable content
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    // Translation
                                    Text(
                                        text = "Translation: ${result.translation}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                item {
                                    // Definition
                                    Text(
                                        text = result.definition,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                items(result.examples) { ex ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = ex,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                painter = painterResource(
                                                    id = if (speakingWord == ex) R.drawable.ic_pause else R.drawable.ic_speaker
                                                ),
                                                contentDescription = if (speakingWord == ex) "Pause Speech" else "Speak Example",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable {
                                                        if (speakingWord == ex) {
                                                            textToSpeech.stop()
                                                            speakingWord = null
                                                        } else {
                                                            textToSpeech.speak(ex, TextToSpeech.QUEUE_FLUSH, null, ex)
                                                            speakingWord = ex
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            // Save Button
                            Button(
                                modifier = Modifier.align(Alignment.End),
                                onClick = {
                                    isSaving = true
                                    coroutineScope.launch {
                                        try {
                                            RetrofitInstance.apiService.saveWord(result)
                                            result.saved = true
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error: ${e.message}", duration = SnackbarDuration.Short, withDismissAction = true)
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

            LaunchedEffect(results) {
                if (results.isNotEmpty()) {
                    state.scrollToItem(0)
                }
            }
        }

        // SnackbarHost positioned over the content
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp) // Optional padding
        )
    }
}
