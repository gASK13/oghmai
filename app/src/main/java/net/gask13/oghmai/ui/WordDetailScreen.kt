package net.gask13.oghmai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.WordActionEnum
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.components.ConfirmDialogHandler
import net.gask13.oghmai.ui.components.ConfirmDialogRequest
import net.gask13.oghmai.ui.components.OptionMenuItem
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar
import net.gask13.oghmai.ui.components.WordResultCard
import net.gask13.oghmai.ui.components.WordStatusBadge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    word: String,
    navController: NavHostController?,
    textToSpeech: TextToSpeechWrapper
) {
    var wordResult by remember { mutableStateOf<WordResult?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Single state variable for confirmation action
    var dialogRequest by remember { mutableStateOf<ConfirmDialogRequest?>(null) }

    LaunchedEffect(word) {
        coroutineScope.launch {
            val response = RetrofitInstance.apiService.getWord(word)
            wordResult = response
        }
    }

    ConfirmDialogHandler(
        dialogRequest = dialogRequest,
        onDismiss = { dialogRequest = null }
    )

    ScaffoldWithTopBar(
        title = word,
        isMainScreen = false,
        onBackClick = { navController?.navigateUp() },
        showOptionsMenu = true,
        optionsMenuItems = listOf(
            OptionMenuItem(
                text = "Delete",
                icon = Icons.Default.Delete,
                onClick = {
                    dialogRequest = ConfirmDialogRequest.Generic(
                        action = "Delete",
                        item = "word '$word'",
                        onConfirmAction = {
                            coroutineScope.launch {
                                RetrofitInstance.apiService.deleteWord(word)
                                wordResult = null
                                snackbarHostState.showSnackbar(
                                    "Word deleted successfully",
                                    duration = SnackbarDuration.Short, withDismissAction = true
                                )
                                navController?.navigateUp()
                            }
                        }
                    )
                }
            ),
            OptionMenuItem(
                text = "Reset status",
                icon = Icons.Default.Refresh,
                onClick = {
                    dialogRequest = ConfirmDialogRequest.Generic(
                        action = "Reset",
                        item = "word '$word' to NEW status",
                        onConfirmAction = {
                            coroutineScope.launch {
                                RetrofitInstance.apiService.patchWord(word, WordActionEnum.RESET)
                                wordResult = RetrofitInstance.apiService.getWord(word)
                                snackbarHostState.showSnackbar(
                                    "Word status reset successfully",
                                    duration = SnackbarDuration.Short, withDismissAction = true
                                )
                            }
                        }
                    )
                }
            ),
            OptionMenuItem(
                text = "Explain...",
                icon = Icons.Default.Info,
                onClick = {
                    // TODO: Implement explain functionality
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Explain functionality not implemented yet")
                    }
                }
            )
        ),
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            wordResult?.let {
                val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    WordResultCard(
                        wordResult = it,
                        textToSpeech = textToSpeech,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Display creation date
                            Text(
                                text = "Created At: ${it.createdAt?.let { date -> dateFormatter.format(date) } ?: "N/A"}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Display last test date and test results
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "Last Test: ${it.lastTest?.let { date -> dateFormatter.format(date) } ?: "N/A"}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row {
                                    it.testResults.forEach { result ->
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(end = 4.dp)
                                                .background(
                                                    color = if (result) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                                    shape = MaterialTheme.shapes.small
                                                )
                                        )
                                    }
                                }
                            }

                            // Display status as a badge
                            WordStatusBadge(
                                wordStatus = it.status,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
