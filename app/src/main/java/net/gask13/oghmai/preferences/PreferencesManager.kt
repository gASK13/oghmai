package net.gask13.oghmai.preferences

import android.content.Context
import android.content.SharedPreferences
import net.gask13.oghmai.model.AppLanguage

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "oghmai_preferences"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_HOUR = "notification_hour"
        private const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TTS_SPEECH_RATE = "tts_speech_rate"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_VOICE = "tts_voice"

        // Default values
        private const val DEFAULT_HOUR = 9
        private const val DEFAULT_MINUTE = 0
        private const val DEFAULT_TTS_SPEECH_RATE = 1.0f
        private const val DEFAULT_TTS_PITCH = 1.0f
        private const val DEFAULT_TTS_VOICE = ""

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }

        fun getCurrentLanguageCode(): String {
            return instance?.getAppLanguage()?.code ?: AppLanguage.DEFAULT.code
        }
    }

    init {
        instance = this
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getNotificationHour(): Int {
        return sharedPreferences.getInt(KEY_NOTIFICATION_HOUR, DEFAULT_HOUR)
    }

    fun getNotificationMinute(): Int {
        return sharedPreferences.getInt(KEY_NOTIFICATION_MINUTE, DEFAULT_MINUTE)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        sharedPreferences.edit().apply {
            putInt(KEY_NOTIFICATION_HOUR, hour)
            putInt(KEY_NOTIFICATION_MINUTE, minute)
            apply()
        }
    }

    fun getAppLanguage(): AppLanguage {
        val raw = sharedPreferences.getString(KEY_LANGUAGE, null)
        return AppLanguage.fromCode(raw)
    }

    fun setAppLanguage(language: AppLanguage) {
        sharedPreferences.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun getLanguage(): String {
        return getAppLanguage().displayName
    }

    fun getLanguageCode(): String {
        return getAppLanguage().code
    }

    fun setLanguage(language: String) {
        val appLanguage = AppLanguage.fromCode(language)
        setAppLanguage(appLanguage)
    }

    fun getTtsSpeechRate(): Float {
        return sharedPreferences.getFloat(KEY_TTS_SPEECH_RATE, DEFAULT_TTS_SPEECH_RATE)
    }

    fun setTtsSpeechRate(rate: Float) {
        sharedPreferences.edit().putFloat(KEY_TTS_SPEECH_RATE, rate).apply()
    }

    fun getTtsPitch(): Float {
        return sharedPreferences.getFloat(KEY_TTS_PITCH, DEFAULT_TTS_PITCH)
    }

    fun setTtsPitch(pitch: Float) {
        sharedPreferences.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
    }

    fun getTtsVoice(languageCode: String = getAppLanguage().code): String {
        val langSpecificKey = "${KEY_TTS_VOICE}_${languageCode.lowercase()}"
        val langVoice = sharedPreferences.getString(langSpecificKey, null)
        if (!langVoice.isNullOrEmpty()) {
            return langVoice
        }
        if (languageCode.equals("IT", ignoreCase = true)) {
            return sharedPreferences.getString(KEY_TTS_VOICE, DEFAULT_TTS_VOICE) ?: DEFAULT_TTS_VOICE
        }
        return DEFAULT_TTS_VOICE
    }

    fun setTtsVoice(voiceName: String, languageCode: String = getAppLanguage().code) {
        val langSpecificKey = "${KEY_TTS_VOICE}_${languageCode.lowercase()}"
        sharedPreferences.edit().apply {
            putString(langSpecificKey, voiceName)
            if (languageCode.equals("IT", ignoreCase = true)) {
                putString(KEY_TTS_VOICE, voiceName)
            }
            apply()
        }
    }
}
