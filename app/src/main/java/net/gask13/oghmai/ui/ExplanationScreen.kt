package net.gask13.oghmai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.ExplanationResponse
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplanationScreen(
    word: String,
    navController: NavHostController?,
    explanationType: String,
    getExplanation: suspend (String) -> ExplanationResponse
) {
    var explanationResponse by remember { mutableStateOf<ExplanationResponse?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(word) {
        coroutineScope.launch {
            try {
                val response = getExplanation(word)
                explanationResponse = response
                isLoading = false
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    "Error loading $explanationType: ${e.message}",
                    duration = SnackbarDuration.Long
                )
                isLoading = false
            }
        }
    }

    ScaffoldWithTopBar(
        title = "$explanationType for '$word'",
        isMainScreen = false,
        onBackClick = { navController?.navigateUp() },
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                explanationResponse?.let { response ->
                    if (response.explanations.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No $explanationType information available for this word.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(response.explanations.entries.toList()) { (category, explanations) ->
                                ExplanationCard(category, explanations)
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Failed to load $explanationType information.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ExplanationCard(category: String, explanations: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            explanations.forEach { explanation ->
                Text(
                    text = "• $explanation",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

