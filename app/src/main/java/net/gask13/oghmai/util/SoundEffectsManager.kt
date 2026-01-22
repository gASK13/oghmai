package net.gask13.oghmai.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manager for playing sound effects throughout the app.
 * Uses ToneGenerator to create simple, non-intrusive sounds for correct and incorrect actions.
 */
class SoundEffectsManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Initialize the ToneGenerator.
     * Should be called when the manager is first created.
     */
    fun initialize() {
        try {
            // Initialize with STREAM_MUSIC and moderate volume
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            // If initialization fails, sound effects will be silently disabled
            toneGenerator = null
        }
    }

    /**
     * Play a sound effect for a correct answer/match.
     * Uses a pleasant ascending tone.
     */
    suspend fun playCorrectSound() {
        withContext(Dispatchers.IO) {
            try {
                if (shouldPlaySound()) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200) // Short pleasant tone
                }
            } catch (e: Exception) {
                // Silently ignore any errors
            }
        }
    }

    /**
     * Play a sound effect for an incorrect answer/match.
     * Uses a brief negative tone.
     */
    suspend fun playIncorrectSound() {
        withContext(Dispatchers.IO) {
            try {
                if (shouldPlaySound()) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200) // Short error tone
                }
            } catch (e: Exception) {
                // Silently ignore any errors
            }
        }
    }

    /**
     * Check if sounds should be played based on system settings.
     * Respects both ringer mode and media volume.
     */
    private fun shouldPlaySound(): Boolean {
        // Don't play if ToneGenerator is not initialized
        if (toneGenerator == null) return false

        // Check ringer mode - don't play in silent mode
        val ringerMode = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            return false
        }

        // Check media volume - don't play if media volume is 0
        val mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (mediaVolume == 0) {
            return false
        }

        return true
    }

    /**
     * Release resources used by the ToneGenerator.
     * Should be called when the manager is no longer needed.
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Silently ignore any errors during cleanup
        }
    }
}
