package net.gask13.oghmai.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.gask13.oghmai.model.AppLanguage
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

    fun setLanguage(appLanguage: AppLanguage) {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.language = appLanguage.locale
            applyVoiceSettings(appLanguage)
        }
    }

    fun applyVoiceSettings(appLanguage: AppLanguage? = null) {
        if (!this::textToSpeech.isInitialized) return

        preferencesManager?.let { prefs ->
            val lang = appLanguage ?: prefs.getAppLanguage()
            val speechRate = prefs.getTtsSpeechRate(lang.code)
            val pitch = prefs.getTtsPitch(lang.code)
            val voiceName = prefs.getTtsVoice(lang.code)

            textToSpeech.setSpeechRate(speechRate)
            textToSpeech.setPitch(pitch)

            // Apply selected voice if set
            var voiceApplied = false
            if (voiceName.isNotEmpty()) {
                textToSpeech.voices?.find { it.name == voiceName }?.let { voice ->
                    textToSpeech.voice = voice
                    voiceApplied = true
                    Log.d("TextToSpeechWrapper", "Voice set to: ${voice.name}")
                }
            }

            // Fallback to default voice matching current language if none set or not found
            if (!voiceApplied) {
                val matchingVoice = textToSpeech.voices?.firstOrNull { voice ->
                    lang.isoLanguages.contains(voice.locale.language.lowercase()) && !voice.isNetworkConnectionRequired
                } ?: textToSpeech.voices?.firstOrNull { voice ->
                    lang.isoLanguages.contains(voice.locale.language.lowercase())
                }

                if (matchingVoice != null) {
                    textToSpeech.voice = matchingVoice
                    Log.d("TextToSpeechWrapper", "Default voice applied for ${lang.displayName}: ${matchingVoice.name}")
                } else {
                    textToSpeech.language = lang.locale
                }
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

    fun getAvailableVoices(appLanguage: AppLanguage? = null): List<Pair<String, String>> {
        if (!this::textToSpeech.isInitialized) {
            return emptyList()
        }

        val lang = appLanguage ?: preferencesManager?.getAppLanguage() ?: AppLanguage.DEFAULT

        val allVoicesForLang = textToSpeech.voices?.filter { voice ->
            lang.isoLanguages.contains(voice.locale.language.lowercase())
        } ?: emptyList()

        val offlineVoices = allVoicesForLang.filter { !it.isNetworkConnectionRequired }
        val candidateVoices = if (offlineVoices.isNotEmpty()) offlineVoices else allVoicesForLang

        val result = candidateVoices.map { voice ->
            val displayName = buildVoiceDisplayName(voice)
            Pair(voice.name, displayName)
        }.sortedBy { it.second }

        Log.d("TextToSpeechWrapper", "Found ${result.size} ${lang.displayName} voices")
        return result
    }

    private fun buildVoiceDisplayName(voice: Voice): String {
        val networkSuffix = if (voice.isNetworkConnectionRequired) " (Network)" else ""
        return "${voice.name}$networkSuffix"
    }

    fun shutdown() {
        if (this::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun initializeTextToSpeech(ctx : Context, onInitialized: (() -> Unit)? = null) {
        onInitCallback = onInitialized
        preferencesManager = PreferencesManager.getInstance(ctx)
        textToSpeech = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val currentLang = preferencesManager?.getAppLanguage() ?: AppLanguage.DEFAULT
                textToSpeech.language = currentLang.locale
                applyVoiceSettings(currentLang)
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