package net.gask13.oghmai.ui

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.gask13.oghmai.R
import net.gask13.oghmai.model.DescriptionRequest
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.model.WordStatus
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar
import net.gask13.oghmai.ui.components.WordResultCard
import net.gask13.oghmai.util.SnackbarManager

/**
 * A unified screen that handles both word discovery and description.
 * 
 * @param navController The navigation controller
 * @param textToSpeech The text-to-speech wrapper
 * @param initialWord Optional initial word to discover. If provided, the screen will behave like
 *                   DiscoverWordScreen. If null, it will behave like DescribeWordScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDiscoveryScreen(
    navController: NavController,
    textToSpeech: TextToSpeechWrapper,
    initialWord: String? = null
) {
    // State variables
    var inputText by rememberSaveable { mutableStateOf(initialWord ?: "") }
    var results by rememberSaveable { mutableStateOf<List<WordResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(initialWord != null) }
    var isGuessing by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var previousStatusMap by remember { mutableStateOf<Map<String, WordStatus>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Determine if we're in "discover" mode (specific word) or "describe" mode (general input)
    val isDiscoverMode = initialWord != null

    // Speech recognition for voice input (only in describe mode)
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

    // Fetch word details for discovery mode
    LaunchedEffect(initialWord) {
        if (initialWord != null) {
            try {
                val result = RetrofitInstance.apiService.describeWord(DescriptionRequest(initialWord))
                if (result.isSuccessful && result.body() != null) {
                    results = listOf(result.body()!!)
                } else {
                    SnackbarManager.showInfo(
                        snackbarHostState = snackbarHostState,
                        message = "Word not found"
                    )
                }
                isLoading = false
            } catch (e: Exception) {
                SnackbarManager.showOperationError(
                    snackbarHostState = snackbarHostState,
                    operation = "load",
                    exception = e
                )
                isLoading = false
            }
        }
    }

    ScaffoldWithTopBar(
        title = if (isDiscoverMode) "Discover Word" else "Describe Word",
        isMainScreen = false,
        onBackClick = { navController.navigateUp() },
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Input field (only shown in describe mode)
                if (!isDiscoverMode) {
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

                    // Action buttons (only in describe mode)
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
                                                SnackbarManager.showInfo(
                                                    snackbarHostState = snackbarHostState,
                                                    message = "No more matches!"
                                                )
                                            }
                                        } else {
                                            SnackbarManager.showInfo(
                                                snackbarHostState = snackbarHostState,
                                                message = "No matching words found"
                                            )
                                        }
                                        focusRequester.freeFocus()
                                    } catch (e: Exception) {
                                        SnackbarManager.showOperationError(
                                            snackbarHostState = snackbarHostState,
                                            operation = "search",
                                            exception = e
                                        )
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
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            enabled = !isGuessing && !isSaving && results.isNotEmpty()
                        ) {
                            Text("Reset")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Word results display
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (results.isEmpty() && !isDiscoverMode) {
                    // Empty state for describe mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Enter a word or description to get started")
                    }
                } else if (results.isEmpty()) {
                    // Empty state for discovery mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Word not found")
                    }
                } else {
                    // Display word results
                    val state = rememberLazyListState()
                    var currentPage by remember { mutableStateOf(0) }

                    // Dots indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        results.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == currentPage) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                    .clickable {
                                        currentPage = index
                                        coroutineScope.launch {
                                            state.animateScrollToItem(index)
                                        }
                                    }
                            )
                        }
                    }

                    // Update current page when scrolling
                    LaunchedEffect(state) {
                        snapshotFlow { state.firstVisibleItemIndex }
                            .distinctUntilChanged()
                            .collect { index ->
                                currentPage = index
                            }
                    }

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
                            WordResultCard(
                                wordResult = result,
                                textToSpeech = textToSpeech,
                                modifier = Modifier.fillParentMaxWidth().fillMaxHeight(),
                                isSaving = isSaving,
                                previousStatus = previousStatusMap[result.word],
                                onSave = {
                                    isSaving = true
                                    coroutineScope.launch {
                                        try {
                                            // Store the old status before saving
                                            previousStatusMap = previousStatusMap + (result.word to result.status)

                                            RetrofitInstance.apiService.saveWord(result)
                                            result.status = WordStatus.NEW
                                        } catch (e: Exception) {
                                            SnackbarManager.showOperationError(
                                                snackbarHostState = snackbarHostState,
                                                operation = "save",
                                                exception = e
                                            )
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    LaunchedEffect(results) {
                        if (results.isNotEmpty()) {
                            state.scrollToItem(0)
                        }
                    }
                }
            }
        }
    }
}
