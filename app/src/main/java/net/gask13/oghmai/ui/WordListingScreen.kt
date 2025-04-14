package net.gask13.oghmai.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.gask13.oghmai.ui.net.gask13.oghmai.ui.WordListingViewModel
import kotlin.math.abs

@Composable
fun WordListingScreen(viewModel: WordListingViewModel = viewModel()) {
    val words by viewModel.words.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // Show loading indicator
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Show the list of words
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(words, key = { it }) { word ->
                    var offsetX by remember { mutableStateOf(0f) }
                    val animatableOffsetX = remember { Animatable(0f) }
                    val threshold = 300f // Swipe threshold to trigger delete

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        // Background (Delete action)
                        if (offsetX > threshold / 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }

                        // Foreground (Word item)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0) }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { /* Reset drag state */ },
                                        onDragEnd = {
                                            coroutineScope.launch {
                                                if (abs(offsetX) > threshold) {
                                                    viewModel.deleteWord(word)
                                                }
                                                animatableOffsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = tween(300)
                                                )
                                                offsetX = 0f
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, 1000f)
                                        }
                                    )
                                }
                                .clickable { viewModel.onWordClick(context, word) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = word,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}