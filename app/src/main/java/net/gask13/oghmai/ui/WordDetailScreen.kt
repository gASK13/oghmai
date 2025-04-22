package net.gask13.oghmai.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.ui.components.WordResultCard

@Composable
fun WordDetailScreen(
    word: String,
    navController: NavHostController?,
    textToSpeech: TextToSpeech
) {
    var wordResult by remember { mutableStateOf<WordResult?>(null) }
    var speakingWord by remember { mutableStateOf<String?>(null) }
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
            WordResultCard(
                wordResult = it,
                speakingWord = speakingWord,
                textToSpeech = textToSpeech,
                modifier = Modifier
                    .fillMaxHeight(0.5f)
            )
        }
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...", style = MaterialTheme.typography.bodyLarge)
    }
}
