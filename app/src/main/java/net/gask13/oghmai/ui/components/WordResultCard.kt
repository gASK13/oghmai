package net.gask13.oghmai.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.gask13.oghmai.R
import net.gask13.oghmai.model.WordResult

@Composable
fun WordResultCard(
    wordResult: WordResult,
    speakingWord: String?,
    textToSpeech: TextToSpeech,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    onSave: (() -> Unit)? = null // Optional Save action
) {
    var localSpeakingWord by remember { mutableStateOf(speakingWord) }

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
                            id = if (localSpeakingWord == wordResult.word) R.drawable.ic_pause else R.drawable.ic_speaker
                        ),
                        contentDescription = if (localSpeakingWord == wordResult.word) "Pause Speech" else "Speak Word",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                if (localSpeakingWord == wordResult.word) {
                                    textToSpeech.stop()
                                    localSpeakingWord = null
                                } else {
                                    textToSpeech.speak(wordResult.word, TextToSpeech.QUEUE_FLUSH, null, wordResult.word)
                                    localSpeakingWord = wordResult.word
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
                        text = "Translation: ${wordResult.translation}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item {
                    // Definition
                    Text(
                        text = wordResult.definition,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                items(wordResult.examples) { ex ->
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
                                    id = if (localSpeakingWord == ex) R.drawable.ic_pause else R.drawable.ic_speaker
                                ),
                                contentDescription = if (localSpeakingWord == ex) "Pause Speech" else "Speak Example",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        if (localSpeakingWord == ex) {
                                            textToSpeech.stop()
                                            localSpeakingWord = null
                                        } else {
                                            textToSpeech.speak(ex, TextToSpeech.QUEUE_FLUSH, null, ex)
                                            localSpeakingWord = ex
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // Save Button (if provided)
            onSave?.let {
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = it,
                    enabled = !isSaving && !wordResult.saved,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = if (wordResult.saved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (wordResult.saved) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
}
