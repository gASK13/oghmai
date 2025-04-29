package net.gask13.oghmai.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import net.gask13.oghmai.model.WordItem
import net.gask13.oghmai.model.WordStatus
import net.gask13.oghmai.ui.components.StatusIcon
import net.gask13.oghmai.ui.components.TestResultDots


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListingScreen(navController: NavHostController, viewModel: WordListingViewModel = viewModel()) {
    val words by viewModel.words.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatuses by viewModel.selectedStatuses.collectAsState()
    val failedLastTest by viewModel.failedLastTest.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyDeletedWord by remember { mutableStateOf<Pair<WordItem, Int>?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Options") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)

                    // Status filter checkboxes
                    WordStatus.values().forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedStatuses.contains(status),
                                onCheckedChange = { checked ->
                                    val newStatuses = selectedStatuses.toMutableSet()
                                    if (checked) {
                                        newStatuses.add(status)
                                    } else {
                                        newStatuses.remove(status)
                                    }
                                    viewModel.updateSelectedStatuses(newStatuses)
                                }
                            )
                            Text(
                                text = status.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Failed last test filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = failedLastTest,
                            onCheckedChange = { viewModel.updateFailedLastTest(it) }
                        )
                        Text(
                            text = "Failed Last Test",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyFilters()
                        showFilterDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFilterDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar with filter button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface),
                    placeholder = { Text("Search words...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true
                )

                IconButton(
                    onClick = { showFilterDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Filter"
                    )
                }

                IconButton(
                    onClick = { viewModel.applyFilters() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Apply Filters"
                    )
                }
            }

            // Word list
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(words, key = { _, word -> word.word }) { index, word -> // Use word.word as the key
                        val isActionTriggered = remember { mutableStateOf(false) }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.StartToEnd && !isActionTriggered.value) {
                                    isActionTriggered.value = true
                                    recentlyDeletedWord = Pair(word, index)
                                    Log.d("SwipeToDismiss", "Word $word dismissed")
                                    viewModel.deleteWord(word)
                                    showSnackbar = true // Trigger snackbar
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(Color.Red),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            },
                            content = {
                                val isDismissed = dismissState.currentValue != SwipeToDismissBoxValue.Settled
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isDismissed) {
                                            Log.d("Navigation", "Navigating to word detail for $word")
                                            navController.navigate("wordDetail/${word.word}")
                                        },
                                    shape = RoundedCornerShape(32.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = word.word,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }

                                        // Status icon and test result dots
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Test result dots
                                            TestResultDots(
                                                testResults = word.testResults,
                                                dotSize = 8,
                                                dotSpacing = 2
                                            )

                                            // Status icon
                                            StatusIcon(
                                                wordStatus = word.status,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Trigger snackbar in a proper @Composable scope
        if (showSnackbar && recentlyDeletedWord != null) {
            LaunchedEffect(snackbarHostState, recentlyDeletedWord) {
                val result = snackbarHostState.showSnackbar(
                    message = "Deleted ${recentlyDeletedWord!!.first.word}",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                    withDismissAction = true,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undoDeleteWord(recentlyDeletedWord!!.first, recentlyDeletedWord!!.second)
                }
                snackbarHostState.currentSnackbarData?.dismiss() // Dismiss the snackbar
                showSnackbar = false // Reset snackbar trigger
            }
        }

        // SnackbarHost to display the snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
