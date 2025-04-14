package net.gask13.oghmai.ui.net.gask13.oghmai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.WordResult
import net.gask13.oghmai.network.RetrofitInstance

class WordDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val word = intent.getStringExtra("word") ?: return

        setContent {
            var wordResult by remember { mutableStateOf<WordResult?>(null) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(word) {
                coroutineScope.launch {
                    val response = RetrofitInstance.apiService.getWord(word)
                    wordResult = response
                }
            }

            wordResult?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Word: ${it.word}", style = MaterialTheme.typography.headlineMedium)
                    Text("Translation: ${it.translation}", style = MaterialTheme.typography.bodyLarge)
                    Text("Definition: ${it.definition}", style = MaterialTheme.typography.bodyLarge)
                    Text("Examples:", style = MaterialTheme.typography.bodyLarge)
                    it.examples.forEach { ex ->
                        Text("- $ex", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}