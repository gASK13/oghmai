package net.gask13.oghmai.services

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TextToSpeechWrapper {
    private lateinit var textToSpeech: TextToSpeech
    // Observable state for Compose
    var storedUtteranceId by mutableStateOf<String?>(null)
        private set

    fun stop() {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }
    }

    fun speak(text: String, utteranceId: String) {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun shutdown() {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun initializeTextToSpeech(ctx : Context) {
        textToSpeech = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ITALIAN
            }
        }

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                storedUtteranceId = null
            }
            override fun onError(utteranceId: String?) {
                storedUtteranceId = null
            }
            override fun onStart(utteranceId: String?) {
                storedUtteranceId = utteranceId
            }
        })
    }
}