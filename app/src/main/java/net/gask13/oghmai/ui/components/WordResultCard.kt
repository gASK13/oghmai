package net.gask13.oghmai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.gask13.oghmai.R
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.model.WordStatus
import net.gask13.oghmai.services.TextToSpeechWrapper

@Composable
fun WordResultCard(
    wordResult: WordResult,
    textToSpeech: TextToSpeechWrapper,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    onSave: (() -> Unit)? = null, // Optional Save action
    previousStatus: WordStatus? = null // For animation when status changes
) {
    Card(
        modifier = modifier
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        text = wordResult.word,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        painter = painterResource(
                            id = if (textToSpeech.storedUtteranceId == wordResult.word) R.drawable.ic_pause else R.drawable.ic_speaker
                        ),
                        contentDescription = if (textToSpeech.storedUtteranceId == wordResult.word) "Pause Speech" else "Speak Word",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                if (textToSpeech.storedUtteranceId == wordResult.word) {
                                    textToSpeech.stop()
                                } else {
                                    textToSpeech.speak(wordResult.word, wordResult.word)
                                }
                            }
                    )
                }
            }

            // Dots indicator for meanings
            if (wordResult.meanings.isNotEmpty()) {
                val state = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                var currentPage by remember { mutableStateOf(0) }

                // Manual page change handler
                val onPageChange = { page: Int ->
                    currentPage = page
                    coroutineScope.launch {
                        state.animateScrollToItem(page)
                    }
                }

                // Dots indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    wordResult.meanings.forEachIndexed { index, _ ->
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
                                    onPageChange(index)
                                }
                        )
                    }
                }

                // Update current page when scrolling
                LaunchedEffect(state) {
                    // Use snapshotFlow to observe changes to the first visible item index
                    snapshotFlow { state.firstVisibleItemIndex }
                        .distinctUntilChanged()
                        .collect { index ->
                            currentPage = index
                        }
                }

                // Horizontal pager for meanings
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    state = state,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
                ) {
                    items(wordResult.meanings.size) { index ->
                        val meaning = wordResult.meanings[index]

                        // Meaning card
                        Card(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            // Scrollable content for each meaning
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    // Translation
                                    Text(
                                        text = "Translation: ${meaning.translation}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                item {
                                    // Definition
                                    Text(
                                        text = meaning.definition,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                items(meaning.examples) { ex ->
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
                                                    id = if (textToSpeech.storedUtteranceId == ex) R.drawable.ic_pause else R.drawable.ic_speaker
                                                ),
                                                contentDescription = if (textToSpeech.storedUtteranceId == ex) "Pause Speech" else "Speak Example",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clickable {
                                                        if (textToSpeech.storedUtteranceId == ex) {
                                                            textToSpeech.stop()
                                                        } else {
                                                            textToSpeech.speak(ex, ex)
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Display a message if there are no meanings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No meanings available for this word")
                }
            }

            // Save Button or Status Badge
            onSave?.let {
                if (wordResult.status == WordStatus.UNSAVED) {
                    Button(
                        modifier = Modifier.align(Alignment.End),
                        onClick = it,
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                } else {
                    AnimatedWordStatusBadge(
                        wordStatus = wordResult.status,
                        previousStatus = previousStatus,
                        modifier = Modifier.align(Alignment.End),
                        showCelebration = true
                    )
                }
            }
        }
    }
}
