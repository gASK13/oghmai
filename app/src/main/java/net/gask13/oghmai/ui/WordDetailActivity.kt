package net.gask13.oghmai.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.gask13.oghmai.R
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance
import java.util.*

class WordDetailActivity : ComponentActivity() {
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ITALIAN
            }
        }

        val word = intent.getStringExtra("word") ?: return

        setContent {
            WordDetailScreen(word = word, navController = null, textToSpeech = textToSpeech)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }
}

@Composable
fun WordDetailScreen(word: String, navController: NavHostController?, textToSpeech: TextToSpeech) {
    var wordResult by remember { mutableStateOf<WordResult?>(null) }
    var speakingWord by remember { mutableStateOf<String?>(null) } // Track which word/example is speaking
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(word) {
        coroutineScope.launch {
            val response = RetrofitInstance.apiService.getWord(word)
            wordResult = response
        }
    }

    wordResult?.let {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                text = it.word,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                painter = painterResource(
                                    id = if (speakingWord == it.word) R.drawable.ic_pause else R.drawable.ic_speaker
                                ),
                                contentDescription = if (speakingWord == it.word) "Pause Speech" else "Speak Word",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        if (speakingWord == it.word) {
                                            textToSpeech.stop()
                                            speakingWord = null
                                        } else {
                                            textToSpeech.speak(it.word, TextToSpeech.QUEUE_FLUSH, null, it.word)
                                            speakingWord = it.word
                                        }
                                    }
                            )
                        }
                    }

                    // Translation
                    Text(
                        text = "Translation: ${it.translation}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Definition
                    Text(
                        text = it.definition,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    // Examples
                    it.examples.forEach { ex ->
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
            }
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...", style = MaterialTheme.typography.bodyLarge)
    }
}

