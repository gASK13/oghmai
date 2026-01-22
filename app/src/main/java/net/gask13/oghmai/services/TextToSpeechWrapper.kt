package net.gask13.oghmai.services

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.gask13.oghmai.preferences.PreferencesManager

class TextToSpeechWrapper {
    private lateinit var textToSpeech: TextToSpeech
    private var preferencesManager: PreferencesManager? = null
    // Observable state for Compose
    var storedUtteranceId by mutableStateOf<String?>(null)
        private set

    private var onInitCallback: (() -> Unit)? = null

    fun stop() {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
        }
    }

    fun speak(text: String, utteranceId: String) {
        if (this::textToSpeech.isInitialized) {
            applyVoiceSettings()
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun applyVoiceSettings() {
        preferencesManager?.let { prefs ->
            val speechRate = prefs.getTtsSpeechRate()
            val pitch = prefs.getTtsPitch()
            val voiceName = prefs.getTtsVoice()

            textToSpeech.setSpeechRate(speechRate)
            textToSpeech.setPitch(pitch)

            // Apply voice if set
            if (voiceName.isNotEmpty()) {
                setVoice(voiceName)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.setSpeechRate(rate)
        }
    }

    fun setPitch(pitch: Float) {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.setPitch(pitch)
        }
    }

    fun setVoice(voiceName: String) {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.voices?.find { it.name == voiceName }?.let { voice ->
                textToSpeech.voice = voice
                Log.d("TextToSpeechWrapper", "Voice set to: ${voice.name}")
            } ?: Log.w("TextToSpeechWrapper", "Voice not found: $voiceName")
        }
    }

    fun getAvailableVoices(): List<Pair<String, String>> {
        if (!this::textToSpeech.isInitialized) {
            return emptyList()
        }

        val italianVoices = textToSpeech.voices?.filter { voice ->
            !voice.isNetworkConnectionRequired && (voice.locale.language == "it" || voice.locale.language == "ita")
        }?.map { voice ->
            val displayName = buildVoiceDisplayName(voice)
            Pair(voice.name, displayName)
        }?.sortedBy { it.second } ?: emptyList()

        Log.d("TextToSpeechWrapper", "Found ${italianVoices.size} Italian voices")
        return italianVoices
    }

    private fun buildVoiceDisplayName(voice: Voice): String {
        return voice.name
    }

    fun shutdown() {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun initializeTextToSpeech(ctx : Context, onInitialized: (() -> Unit)? = null) {
        onInitCallback = onInitialized
        preferencesManager = PreferencesManager(ctx)
        textToSpeech = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ITALIAN
                applyVoiceSettings()
                onInitCallback?.invoke()
            } else {
                Log.e("TextToSpeechWrapper", "TTS initialization failed with status: $status")
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