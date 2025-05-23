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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.WordActionEnum
import net.gask13.oghmai.model.WordItem
import net.gask13.oghmai.model.WordStatus
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar
import net.gask13.oghmai.ui.components.StatusIcon
import net.gask13.oghmai.ui.components.TestResultDots


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListingScreen(
    navController: NavHostController
) {
    var words by rememberSaveable { mutableStateOf<List<WordItem>>(emptyList()) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatuses by rememberSaveable { mutableStateOf<Set<WordStatus>>(emptySet()) }
    var failedLastTest by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var recentlyDeletedWord by rememberSaveable { mutableStateOf<Pair<WordItem, Int>?>(null) }
    var showSnackbar by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }

    fun fetchWords() {
        coroutineScope.launch {
            isLoading = true

            // Prepare status filter
            val statusFilter = if (selectedStatuses.isNotEmpty()) {
                selectedStatuses.joinToString(",") { it.name }
            } else {
                null
            }

            // Prepare failed last test filter
            val failedLastTestFilter = if (failedLastTest) {
                true
            } else {
                null
            }

            // Prepare the search query filter
            val containsFilter = searchQuery.ifEmpty {
                null
            }

            try {
                val response = RetrofitInstance.apiService.getWords(
                    status = statusFilter,
                    failedLastTest = failedLastTestFilter,
                    contains = containsFilter
                )
                words = response.words
                isLoading = false
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Error fetching words! {${e.message}",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (words.isEmpty()) {
            // Only load if no words when returning from word detail
            fetchWords()
        }
    }

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
                    WordStatus.entries.forEach { status ->
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
                                    selectedStatuses = newStatuses.toSet()
                                }
                            )
                            Text(
                                text = status.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Failed last test filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = failedLastTest,
                            onCheckedChange = { failedLastTest = it}
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
                        fetchWords()
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

    ScaffoldWithTopBar(
        title = "Word List",
        isMainScreen = false,
        onBackClick = { navController.navigateUp() },
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar with a filter button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue -> 
                            searchQuery = newValue
                            fetchWords()
                        },
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
                        onClick = { fetchWords() }
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
                                        coroutineScope.launch {
                                            try {
                                                RetrofitInstance.apiService.deleteWord(word.word)
                                                // Update the list locally after successful deletion
                                                words = words.filter { it != word }
                                                showSnackbar = true // Trigger snackbar
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Error deleting word! {${e.message}", withDismissAction = true, duration = SnackbarDuration.Short)
                                            }
                                        }
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
                        coroutineScope.launch {
                            try {
                                RetrofitInstance.apiService.patchWord(
                                    recentlyDeletedWord!!.first.word,
                                    WordActionEnum.UNDELETE
                                )
                                val currentList = words.toMutableList()
                                currentList.add(recentlyDeletedWord!!.second, recentlyDeletedWord!!.first)
                                words = currentList
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error restoring word! {${e.message}", withDismissAction = true, duration = SnackbarDuration.Short)
                            }
                        }
                    }
                    snackbarHostState.currentSnackbarData?.dismiss() // Dismiss the snackbar
                    showSnackbar = false // Reset snackbar trigger
                }
            }
        }
    }
}
